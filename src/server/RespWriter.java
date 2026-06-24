package server;

import java.io.ByteArrayOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RespWriter {
    private final ByteArrayOutputStream buf;
    public RespWriter(ByteArrayOutputStream buf) { this.buf = buf; }

    private void raw(String s) {
        byte[] b = s.getBytes(UTF_8);
        buf.write(b, 0, b.length);
    }

    public void writeSimple(String s)  { raw("+" + s + "\r\n"); }
    public void writeError(String msg) { raw("-" + msg + "\r\n"); }
    public void writeInteger(long n)   { raw(":" + n + "\r\n"); }
    public void writeArrayLen(int n)   { raw("*" + n + "\r\n"); }

    public void writeBulk(String s) {

           if (s == null ) raw("$-1\r\n");
          else {
               byte[] body = s.getBytes(UTF_8);
               raw("$" + body.length + "\r\n");
               buf.write(body, 0, body.length);
               raw("\r\n");
           }
    }

}