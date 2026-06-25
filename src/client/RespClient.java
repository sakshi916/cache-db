package client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class RespClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 6380);
        OutputStream out = socket.getOutputStream();
        InputStream in = new BufferedInputStream(socket.getInputStream());
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

        String line;
        while ((line = keyboard.readLine()) != null) {
            if (line.equals("quit")) break;
            String[] parts = line.trim().split("\\s+");
            if (parts.length == 0 || parts[0].isEmpty()) continue;

            // --- encode request as RESP array of bulk strings ---
            StringBuilder sb = new StringBuilder();
            sb.append('*').append(parts.length).append("\r\n");
            for (String part : parts) {
                byte[] b = part.getBytes(StandardCharsets.UTF_8);
                sb.append('$').append(b.length).append("\r\n").append(part).append("\r\n");
            }
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
            out.flush();

            // --- read + print one reply ---
            printReply(in, "");
        }
        socket.close();
    }

    static void printReply(InputStream in, String indent) throws IOException {
        int type = in.read();
        if (type == -1) { System.out.println("(connection closed)"); return; }

        switch (type) {
            case '+': // simple string
                System.out.println(indent + readLine(in));
                break;
            case '-': // error
                System.out.println(indent + "(error) " + readLine(in));
                break;
            case ':': // integer
                System.out.println(indent + "(integer) " + readLine(in));
                break;
            case '$': { // bulk string
                int len = Integer.parseInt(readLine(in));
                if (len == -1) { System.out.println(indent + "(nil)"); break; }
                byte[] body = new byte[len];
                int read = 0;
                while (read < len) {
                    int r = in.read(body, read, len - read);
                    if (r == -1) break;
                    read += r;
                }
                in.read(); in.read(); // consume trailing \r\n
                System.out.println(indent + "\"" + new String(body, StandardCharsets.UTF_8) + "\"");
                break;
            }
            case '*': { // array
                int count = Integer.parseInt(readLine(in));
                if (count == -1) { System.out.println(indent + "(nil)"); break; }
                if (count == 0) { System.out.println(indent + "(empty array)"); break; }
                for (int i = 1; i <= count; i++) {
                    System.out.print(indent + i + ") ");
                    printReply(in, indent + "   ");   // recurse; nested arrays indent
                }
                break;
            }
            default:
                System.out.println(indent + "(unknown reply type: " + (char) type + ")");
        }
    }

    // read bytes up to and consuming \r\n, return the text before \r\n
    static String readLine(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') { in.read(); break; }   // consume the \n
            sb.append((char) c);
        }
        return sb.toString();
    }
}