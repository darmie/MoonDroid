package lpegj;

public class Capture {
    int pos;
    Object data;
    CapKind kind;

    Capture() {
    }

    Capture(int pos, Object data, CapKind kind) {
        this.pos = pos;
        this.data = data;
        this.kind = kind;
    }

    boolean isclose() {
        return kind == CapKind.Cclose;
    }

    public String toString() {
        return kind.toString() + " " + (data == null ? "null" : data.toString());
    }
}
