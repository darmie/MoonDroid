package lpegj;

public interface RuntimeMatcher {
    public MatcherResult match(char[] subject, int pos, Object[] values);
}
