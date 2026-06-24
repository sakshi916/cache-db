import java.io.*;
import java.net.*;

public class EchoClient {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 6380);

        // reads what the SERVER sends back
        BufferedReader fromServer = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        // sends what YOU type to the server
        PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
        /*
            toServer.println("SET c 22\r\nGET c\r\n");                 // send to server
            System.out.println(fromServer.readLine()); // print server's reply
            System.out.println(fromServer.readLine());*/

        BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in));

        String line;
        while ((line = keyboard.readLine()) != null) {
            if (line.equals("quit")) break;   // clean exit → sends FIN
            toServer.println(line);                      // send what you typed
            System.out.println(fromServer.readLine());   // print server's reply
        }
        socket.close();
    }
}