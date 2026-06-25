package server;


import java.util.*;

public class Store {
    private final Map<String, Object> store = new HashMap<>();
    private final Map<String, Long> expires = new HashMap<>();   // key → epoch-millis deadline

    public long now() { return System.currentTimeMillis(); }


    public Object lookup(String key) {

         Long deadline = expires.get(key);
       if (deadline != null && now() > deadline){   // expired
                store.remove(key);
                expires.remove(key);
                return null;
       }
       return store.get(key);

    }

    public boolean delete(String key) {
        expires.remove(key);
        return store.remove(key) != null;
    }
    public void put(String key, Object value) {
        store.put(key, value);
        expires.remove(key);          // SET clears any old TTL (trap #3 from before)
    }
    // Periodically called from the event loop. Samples keys-with-TTL and evicts
// the dead ones. Adaptive: if a sample is mostly-dead, keep going; if mostly
// alive, stop (the keyspace is probably clean).
    public int activeExpireCycle() {
        final int SAMPLE_SIZE = 20;        // keys to check per round
        final double THRESHOLD = 0.25;     // if >25% of a sample was dead, sweep again
        final int MAX_ROUNDS = 16;         // safety bound so we never loop forever

        int totalReaped = 0;

        for (int round = 0; round < MAX_ROUNDS; round++) {
            if (expires.isEmpty()) break;            // nothing with a TTL → done
            List<String> all = new ArrayList<>(expires.keySet());

            // NOTE: shuffling the full keyset each round is O(n) — fine for a small keyspace.
            // Real Redis samples random keys in O(1) from its own hash table (dictGetRandomKey).
            // A production version would use a hash structure supporting O(1) random sampling.
            Collections.shuffle(all);
            List<String> sample = all.subList(0, Math.min(SAMPLE_SIZE, all.size()));
            // --- check the sample, evict the dead ones ---
            long now = now();
            int deadInSample = 0;
            int checked = 0;
            for(String key:sample) {
                   checked++;
                   Long deadline = expires.get(key);
                  if (deadline != null && now > deadline) {
                      store.remove(key);
                      expires.remove(key);
                      deadInSample++;
                      totalReaped++;
                  }
            }
             if (checked == 0)break;
             if ((double)deadInSample / checked < THRESHOLD)  break;
        }

        return totalReaped;   // handy for logging/testing; caller can ignore
    }
    public void setExpiry(String key, long deadlineMillis) { expires.put(key, deadlineMillis); }

    public Long getExpiry(String key) { return expires.get(key); }

    public boolean persist(String key) { return expires.remove(key) != null; }
}
