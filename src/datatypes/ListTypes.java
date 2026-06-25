package datatypes;

import exceptions.TypeException;
import server.RespWriter;
import server.Store;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListTypes {

    private final Store store;
    public ListTypes(Store store) { this.store = store; }
    public static final String WRONGTYPE =
            "WRONGTYPE Operation against a key holding the wrong kind of value";
    public void lpush(List<String> args,RespWriter w) throws TypeException {
        if (args.size() < 2)  w.writeError("ERR wrong number of arguments");
        ArrayDeque<String> arr = asList(args.get(0));
        if (arr == null) { arr = new ArrayDeque<>(); store.put(args.get(0), arr); }
        args.subList(1, args.size()).forEach(arr::addFirst);
        w.writeInteger(arr.size());
    }
    public void range(List<String> args, RespWriter w) throws TypeException {
        if (args.size() != 3) { w.writeError("ERR wrong number of arguments for 'lrange'"); return; }
        ArrayDeque<String> arr = asList(args.get(0));
        if (arr == null || arr.isEmpty()) { w.writeArrayLen(0); return; }
        int start, stop;
        try {
            start = Integer.parseInt(args.get(1));
            stop  = Integer.parseInt(args.get(2));
        } catch (NumberFormatException e) {
            w.writeError("ERR value is not an integer or out of range");
            return;
        }
        int size = arr.size();
        if (start < 0) start += size;
        if (stop  < 0) stop  += size;
        start = Math.max(start, 0);
        stop  = Math.min(stop, size - 1);
        if (start > stop) { w.writeArrayLen(0); return; }
        List<String> sub = new ArrayList<>(arr).subList(start, stop + 1);
        w.writeArrayLen(sub.size());
        sub.forEach(w::writeBulk);
    }
    public void rpush(List<String> args, RespWriter w) throws TypeException {
        if (args.size() < 2) { w.writeError("ERR wrong number of arguments for 'rpush'"); return; }
        ArrayDeque<String> arr = asList(args.get(0));
        if (arr == null) { arr = new ArrayDeque<>(); store.put(args.get(0), arr); }
        arr.addAll(args.subList(1, args.size()));
        w.writeInteger(arr.size());
    }

    public void lpop(List<String> args, RespWriter w) throws TypeException {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'lpop'"); return; }
        ArrayDeque<String> arr = asList(args.get(0));
        if (arr == null || arr.isEmpty()) { w.writeBulk(null); return; }   // nil = $-1
        String v = arr.pollFirst();
        if (arr.isEmpty()) store.delete(args.get(0));
        w.writeBulk(v);
    }

    public void rpop(List<String> args, RespWriter w) throws TypeException {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'rpop'"); return; }
        ArrayDeque<String> arr = asList(args.get(0));
        if (arr == null || arr.isEmpty()) { w.writeBulk(null); return; }
        String v = arr.pollLast();
        if (arr.isEmpty()) store.delete(args.get(0));
        w.writeBulk(v);
    }

    public void llen(List<String> args, RespWriter w) throws TypeException {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'llen'"); return; }
        ArrayDeque<String> arr = asList(args.get(0));
        w.writeInteger(arr == null ? 0 : arr.size());
    }
    @SuppressWarnings("unchecked")
    ArrayDeque<String> asList(String key) throws TypeException {
        Object obj = store.lookup(key);
        if (obj == null) return null;                      // absent ≠ error
        if (!(obj instanceof ArrayDeque)) throw new TypeException(WRONGTYPE);
        return (ArrayDeque<String>) obj;
    }
}
