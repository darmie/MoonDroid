package lpegj;

public class MatcherResult {
    public int pos;
    public Object[] values;

    public MatcherResult(int pos, Object[] values) {
        this.pos = pos;
        this.values = values;
    }

    public MatcherResult(int pos) {
        this(pos, null);
    }

    public MatcherResult() {
        this(0, null);
    }
}
