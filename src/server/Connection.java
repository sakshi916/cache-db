package server;

import java.io.ByteArrayOutputStream;
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
    // add to server.Connection:
    ByteArrayOutputStream replyBuf = new ByteArrayOutputStream();
    // helper to flush scratch → outbox and reset scratch:
    void flushReply() {
        byte[] bytes = replyBuf.toByteArray();
        outbox.add(ByteBuffer.wrap(bytes));
        replyBuf.reset();   // reuse the same ByteArrayOutputStream next command
    }
    // in Connection:
    private final ByteArrayOutputStream inbound = new ByteArrayOutputStream();

    void appendInbound(byte[] data, int n) { inbound.write(data, 0, n); }
    byte[] inboundBytes() { return inbound.toByteArray(); }   // note: allocates a copy
    int    inboundLen()   { return inbound.size(); }

    // remove the first `consumed` bytes (after a successful parse)
    void consumeInbound(int consumed) {
        byte[] all = inbound.toByteArray();
        inbound.reset();
        inbound.write(all, consumed, all.length - consumed);   // keep the leftover
    }
}