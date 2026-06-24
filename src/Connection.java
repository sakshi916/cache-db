import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

public class Connection {
    SocketChannel client;
    StringBuilder stringBuilder;
    Queue<ByteBuffer> outbox = new ArrayDeque<>();

    public Connection(SocketChannel ch){
        client=ch;
        stringBuilder=new StringBuilder();
    }
    void appendInbound(String text) { stringBuilder.append(text); }
    void queueReply(byte[] bytes) {
        outbox.add(ByteBuffer.wrap(bytes));
    }
    boolean hasPendingWrites() { return !outbox.isEmpty(); }
}