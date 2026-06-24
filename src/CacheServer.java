import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import static java.lang.System.out;
public class CacheServer {
    public static Store store;
    static void handleAccept(Selector selector, SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        SelectionKey clientKey=client.register(selector,SelectionKey.OP_READ);
        // This buffer is THIS client's alone and lives for the whole connection.
        clientKey.attach(new Connection(client));
        out.println(client.getRemoteAddress());
    }

static void handleRead(SelectionKey key) throws IOException {
    SocketChannel client = (SocketChannel) key.channel();
    ByteBuffer buf = ByteBuffer.allocate(1024);
    int n = client.read(buf);
    if (n == -1) {
        client.close();
        key.cancel();
        return;
    }
    if (n == 0) return;
    buf.flip();
    String text = StandardCharsets.UTF_8.decode(buf).toString();
    Connection acc = (Connection) key.attachment();
    acc.appendInbound(text);
    while(true){
        int i=acc.stringBuilder.indexOf("\n");
        if(i==-1)break;
        String str=acc.stringBuilder.substring(0,i);
        acc.stringBuilder.delete(0,i+1);
        str=str.trim();
        if(!str.isEmpty()){
            Command cmd = store.parse(str);
            if (cmd == null) continue;
            String reply = store.execute(cmd) + "\r\n";
            acc.queueReply(reply.getBytes(StandardCharsets.UTF_8));
        }
    }
    flush(key);
}

    static void flush(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        Connection conn = (Connection) key.attachment();

        while (conn.hasPendingWrites()) {
            ByteBuffer head = conn.outbox.peek();   // look at front, DON'T remove yet
            client.write(head);                     // writes as much as kernel accepts;
            // advances head.position automatically
            if (head.hasRemaining()) {
                // kernel buffer full — couldn't write it all.
                // leave `head` in the queue (its position is mid-way; next write resumes there).
                // arm OP_WRITE so the loop wakes us when the socket can take more.
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                return;
            }
            conn.outbox.poll();   // fully written → NOW remove it, move to next chunk
        }

        // outbox fully drained → we owe nothing → stop watching for writability
        key.interestOps(SelectionKey.OP_READ);
    }

    public static void main(String[] args) throws IOException {
        int port = 6380;
        Selector selector = Selector.open();
        store=new Store();
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(port));
        server.configureBlocking(false);

        server.register(selector, SelectionKey.OP_ACCEPT);

        out.println("listening on " + port);

        // --- THE EVENT LOOP ---
        while (true) {

             selector.select();

              Set<SelectionKey> keys = selector.selectedKeys();
             Iterator<SelectionKey> it = keys.iterator();
              while (it.hasNext()) {
                  SelectionKey key = it.next();
                   it.remove();
                 try {
                     if (key.isAcceptable()) {
                         handleAccept(selector, key);
                     } else if (key.isReadable()) {
                         handleRead(key);
                     } else if(key.isWritable())
                         flush(key);
                 }catch(IOException e){
                     out.println(e.getMessage());
                     try {
                         key.channel().close();
                     }catch(IOException ignored){}
                 }
            }
        }
    }


}