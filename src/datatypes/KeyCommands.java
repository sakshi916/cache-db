package datatypes;   // or wherever your command classes live

import java.util.List;
import server.RespWriter;   // adjust imports to your packages
import server.Store;

public class KeyCommands {
    private final Store store;
    public KeyCommands(Store store) { this.store = store; }

    // EXPIRE key seconds  → 1 if TTL set, 0 if key doesn't exist
    public void expire(List<String> args, RespWriter w) {
        if (args.size() != 2) { w.writeError("ERR wrong number of arguments for 'expire'"); return; }
        if (store.lookup(args.get(0)) == null) { w.writeInteger(0); return; }   // no live key
        long seconds;
        try { seconds = Long.parseLong(args.get(1)); }
        catch (NumberFormatException e) { w.writeError("ERR value is not an integer or out of range"); return; }
        store.setExpiry(args.get(0), store.now() + seconds * 1000);
        w.writeInteger(1);
    }

    // PEXPIRE key millis  → same as EXPIRE but milliseconds
    public void pexpire(List<String> args, RespWriter w) {
        if (args.size() != 2) { w.writeError("ERR wrong number of arguments for 'pexpire'"); return; }
        if (store.lookup(args.get(0)) == null) { w.writeInteger(0); return; }
        long millis;
        try { millis = Long.parseLong(args.get(1)); }
        catch (NumberFormatException e) { w.writeError("ERR value is not an integer or out of range"); return; }
        store.setExpiry(args.get(0), store.now() + millis);
        w.writeInteger(1);
    }

    public void ttl(List<String> args, RespWriter w) {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'ttl'"); return; }
        if (store.lookup(args.get(0)) == null) { w.writeInteger(-2); return; }
        Long deadline = store.getExpiry(args.get(0));
        if (deadline == null) { w.writeInteger(-1); return; }
        long msLeft = deadline - store.now();
        w.writeInteger((msLeft + 999) / 1000);   // round up to whole seconds
    }

    // PTTL key  → millis remaining; -2 no key, -1 no TTL
    public void pttl(List<String> args, RespWriter w) {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'pttl'"); return; }
        if (store.lookup(args.get(0)) == null) { w.writeInteger(-2); return; }
        Long deadline = store.getExpiry(args.get(0));
        if (deadline == null) { w.writeInteger(-1); return; }
        w.writeInteger(deadline - store.now());
    }

    // PERSIST key  → 1 if a TTL was removed, 0 otherwise
    public void persist(List<String> args, RespWriter w) {
        if (args.size() != 1) { w.writeError("ERR wrong number of arguments for 'persist'"); return; }
        if (store.lookup(args.get(0)) == null) { w.writeInteger(0); return; }
        w.writeInteger(store.persist(args.get(0)) ? 1 : 0);
    }
}