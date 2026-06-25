package protocol;   // adjust to your package

import server.Command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RespParser {

    public record Parsed(Command command, int consumed) {}

    public static class ProtocolException extends RuntimeException {
        public ProtocolException(String m) { super(m); }
    }

    // Returns a parsed command + bytes consumed, or null if the buffer
    // doesn't yet contain a complete command (caller waits for more bytes).
    public static Parsed tryParse(byte[] buf, int len) {
        if (len == 0) return null;

        int pos = 0;
        if (buf[pos] != '*') {
            throw new ProtocolException("expected '*' (array), got byte " + buf[pos]);
        }

        // --- read array count: *<N>\r\n ---
        int crlf = findCrlf(buf, pos + 1, len);
        if (crlf == -1) return null;                 // count line incomplete
        int n = parseInt(buf, pos + 1, crlf);
        pos = crlf + 2;                              // advance past \r\n
        if (n < 0) return null;                      // (treat *-1 as nothing useful)

        // --- read N bulk strings ---
        List<String> parts = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            if (pos >= len) return null;             // no bytes for next bulk header
            if (buf[pos] != '$') {
                throw new ProtocolException("expected '$' (bulk), got byte " + buf[pos]);
            }
            int hdrCrlf = findCrlf(buf, pos + 1, len);
            if (hdrCrlf == -1) return null;          // length line incomplete
            int bulkLen = parseInt(buf, pos + 1, hdrCrlf);
            pos = hdrCrlf + 2;                       // past the $<len>\r\n

            if (bulkLen < 0) {                       // null bulk in a request (rare) → empty arg
                parts.add(null);
                continue;
            }
            // need bulkLen body bytes + trailing \r\n
            if (pos + bulkLen + 2 > len) return null;   // body not fully arrived
            String arg = new String(buf, pos, bulkLen, StandardCharsets.UTF_8);
            parts.add(arg);
            pos = pos + bulkLen + 2;                 // past body + \r\n
        }

        if (parts.isEmpty()) return null;
        String op = parts.get(0).toUpperCase();
        List<String> args = parts.subList(1, parts.size());
        return new Parsed(new Command(op, args), pos);
    }

    // index of \r within a \r\n pair, scanning [from, len); -1 if not present
    private static int findCrlf(byte[] buf, int from, int len) {
        for (int i = from; i + 1 < len; i++) {
            if (buf[i] == '\r' && buf[i + 1] == '\n') return i;
        }
        return -1;
    }

    // parse a base-10 int from bytes [start, end) — handles the digit-vs-ascii correctly
    private static int parseInt(byte[] buf, int start, int end) {
        int result = 0;
        boolean neg = false;
        int i = start;
        if (i < end && buf[i] == '-') { neg = true; i++; }
        for (; i < end; i++) {
            int digit = buf[i] - '0';                // ← byte '5' (53) minus '0' (48) = 5
            if (digit < 0 || digit > 9) {
                throw new ProtocolException("invalid integer in RESP");
            }
            result = result * 10 + digit;
        }
        return neg ? -result : result;
    }
}