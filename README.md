# cache-db

A single-threaded, epoll-based, RESP-speaking key-value store with typed data
structures and Redis-style expiration. Built from raw sockets up in Java, as a
learning project to understand how Redis works internally.

## What it does

- Speaks the real **RESP** wire protocol — compatible with `redis-cli`.
- Serves many concurrent clients on a **single thread** via a non-blocking
  event loop (Java NIO `Selector`, which uses `epoll` on Linux).
- Supports **typed data structures** with Redis-style `WRONGTYPE` safety.
- Implements **TTL / key expiration** using the same lazy + active hybrid
  strategy as Redis.

## Architecture

Three layers, each with a single responsibility:

- **Network layer** (`CacheServer`) — the event loop: accepts connections,
  reads bytes, manages per-connection read framing and write backpressure,
  and runs the periodic expiration sweep. Knows about sockets, not commands.
- **Command layer** (`Executor`, `*Types` classes) — parses RESP requests into
  commands, dispatches each to its handler, and streams RESP replies. Knows
  about commands, not sockets.
- **Storage layer** (`Store`) — the keyspace and expiry tables, plus the
  CRUD and expiration logic. Knows about data, nothing above it.

Request flow: `bytes → RespParser → Command → Executor → Store → RespWriter → bytes`.

### Networking details

- **Connection multiplexing:** one `Selector` watches the listening socket and
  every client socket. The single thread reacts only to sockets that are ready,
  so it never blocks on any one client.
- **Read framing:** TCP has no message boundaries, so each connection
  accumulates incoming bytes and the RESP parser extracts complete commands,
  leaving partial data buffered for the next read.
- **Write backpressure:** replies are queued per connection; if the kernel send
  buffer fills, the unwritten remainder stays queued and the socket registers
  interest in writability, draining as the client reads.

### Expiration

- **Lazy:** every key read goes through a single `lookup` chokepoint that evicts
  the key if its TTL has passed — so an expired key is never served.
- **Active:** the event loop periodically samples keys with TTLs and evicts the
  dead ones, reclaiming memory for keys nobody accesses again. Uses adaptive
  sampling (keep sweeping while a sample is mostly-expired).

## Supported commands

**Strings:** `SET`, `GET`, `DEL`
**Lists:** `LPUSH`, `RPUSH`, `LPOP`, `RPOP`, `LLEN`, `LRANGE`
**Keys / TTL:** `EXPIRE`, `PEXPIRE`, `TTL`, `PTTL`, `PERSIST`
**Connection:** `PING`, `ECHO`

## Running it

Requires Java 17+.

```bash
# from the project root, compile everything
javac -d out $(find src -name "*.java")

# run the server (listens on port 6380)
java -cp out server.CacheServer
```

Then connect with the included Java client:

```bash
java -cp out client.RespClient
```

Example:
```
SET name redis
OK
GET name
"redis"
RPUSH fruits apple banana cherry
(integer) 3
LRANGE fruits 0 -1
"apple"
"banana"
"cherry"
EXPIRE name 60
(integer) 1
TTL name
(integer) 60
```

Because it speaks RESP, the official `redis-cli` also works:
`redis-cli -p 6380 PING`.

## Roadmap

- [ ] Hashes (`HSET`/`HGET`/`HDEL`/`HGETALL`/`HLEN`)
- [ ] Sets (`SADD`/`SREM`/`SMEMBERS`/`SISMEMBER`/`SCARD`)
- [ ] Sorted sets
- [ ] Inline expiry options on `SET` (`EX`/`PX`)
- [ ] Housekeeping commands (`EXISTS`, `TYPE`, `KEYS`, `FLUSHDB`)

## Notes / known simplifications

- Active expiration shuffles the full TTL keyset per round (O(n)); real Redis
  samples random keys in O(1) from its own hash table.
- RESP bulk-string lengths use char length, which equals byte length for ASCII.
