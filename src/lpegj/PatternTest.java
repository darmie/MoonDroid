package lpegj;

import java.util.HashMap;
import java.util.Map;

class MatcherFunc implements RuntimeMatcher {

    public MatcherResult match(char[] subject, int pos, Object[] values) {
        MatcherResult res = new MatcherResult(pos);
        System.out.println("Value: " + values[0]);
        res.values = new Object[]{"foo", "bar"};
        return res;
    }
}

public class PatternTest {
    public static void main(String[] args) throws Exception {
        Pattern p1 = Pattern.concat(Pattern.S("bB"), Pattern.R(new String[]{"ad", "AD"}));
        Pattern p2 = new Pattern("foo");
        Pattern p3 = Pattern.Cmt(Pattern.plus(p1, p2), new MatcherFunc());
        MatcherResult res = p3.match("BDbar", 0, null);
        System.out.println(p3);
        System.out.println(res.pos);
        System.out.println(res.values[0] + (String) res.values[1]);
        Pattern S = Pattern.plus(Pattern.plus(Pattern.concat(new Pattern("a"), Pattern.V("B")),
                Pattern.concat(new Pattern("b"), Pattern.V("A"))), new Pattern(""));
        Pattern A = Pattern.plus(Pattern.concat(new Pattern("a"), Pattern.V("S")),
                Pattern.concat(new Pattern("b"), Pattern.concat(Pattern.V("A"), Pattern.V("A"))));
        Pattern B = Pattern.C(Pattern.concat(Pattern.Carg(1), Pattern.plus(Pattern.concat(new Pattern("b"), Pattern.V("S")),
                Pattern.concat(new Pattern("a"), Pattern.concat(Pattern.V("B"), Pattern.V("B"))))));
        Map<Object, Pattern> g = new HashMap<Object, Pattern>();
        g.put("S", S);
        g.put("A", A);
        g.put("B", B);
        Pattern fg = new Pattern(g, "S");
        System.out.println(fg);
        res = fg.match("ababaaabbbba", 0, new Object[]{"foo", "bar"});
        System.out.println(res.pos);
        for (Object val : res.values)
            System.out.println(val);
    }
}
