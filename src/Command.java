import java.util.List;

public record Command (String op, List<String> args) {
        // TODO: nothing required here yet. A record gives you op(), args(),
        //       constructor, equals/toString for free.
        //       (Later you might add a helper like arg(int i) — skip for now.)

}
