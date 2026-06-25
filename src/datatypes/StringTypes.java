package datatypes;

import exceptions.TypeException;
import server.RespWriter;
import server.Store;

import java.util.List;
import java.util.Map;

public class StringTypes {
    private final Store store;
    public StringTypes(Store store) { this.store = store; }
    public void set(List<String> args, RespWriter w) {
        if (args.size() != 2) { w.writeError("ERR wrong number of arguments for 'set'"); return; }
        store.put(args.get(0), args.get(1));
        w.writeSimple("OK");
    }

    public void get(List<String> args, RespWriter w) throws TypeException {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'get'"); return; }
        Object obj = store.lookup(args.get(0));
        if (obj == null) { w.writeBulk(null); return; }                 // nil
        if (obj instanceof String) { w.writeBulk((String) obj); return; }
        throw new TypeException("WRONGTYPE Operation against a key holding the wrong kind of value");
    }

    public void del(List<String> args, RespWriter w) {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'del'"); return; }
        w.writeInteger(store.delete(args.get(0)) ? 1 : 0);
    }
}
