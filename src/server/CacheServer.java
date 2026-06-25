package server;

import exceptions.TypeException;
import protocol.RespParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import static java.lang.System.out;
public class CacheServer {
    public static Executor executor;
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
    byte[] chunk = new byte[n];
    buf.get(chunk);
    Connection conn = (Connection) key.attachment();
    conn.appendInbound(chunk, n);

    while (true) {
        RespParser.Parsed p;
        try {
            p = RespParser.tryParse(conn.inboundBytes(), conn.inboundLen());
        } catch (RespParser.ProtocolException e) {
            // fatal framing error → reply error and close this client
            // (write "-ERR Protocol error: ...\r\n" then close)
            conn.replyBuf.reset();
            new RespWriter(conn.replyBuf).writeError("ERR Protocol error: " + e.getMessage());
            conn.flushReply();
            flush(key);
            client.close(); key.cancel();
            return;
        }
        if (p == null) break;                    // incomplete → wait for more
        conn.consumeInbound(p.consumed());
        Command cmd = p.command();
        RespWriter w = new RespWriter(conn.replyBuf);
        try { executor.execute(cmd, w); }
        catch (TypeException e) { conn.replyBuf.reset(); w.writeError(e.getMessage()); }
        conn.flushReply();
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
         executor=new Executor(store);
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(port));
        server.configureBlocking(false);

        server.register(selector, SelectionKey.OP_ACCEPT);

        out.println("listening on " + port);

        // --- THE EVENT LOOP ---
        while (true) {

            selector.select(100);

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
            int reaped = store.activeExpireCycle();
        }
    }


}