package lpegj;

import java.util.*;

public class Pattern {
    Instruction[] program;

    public Pattern(char c) {
        this.program = new Instruction[2];
        this.program[0] = new CharInstruction(c);
        this.program[1] = new Instruction(OpCode.IEnd);
    }

    public Pattern(String s) {
        this.program = new Instruction[s.length() + 1];
        char[] arr = s.toCharArray();
        for (int i = 0; i < arr.length; i++)
            this.program[i] = new CharInstruction(arr[i]);
        this.program[arr.length] = new Instruction(OpCode.IEnd);
    }

    public Pattern(short n) {
        if (n == 0) {
            this.program = new Instruction[1];
        } else if (n > 0) {
            this.program = new Instruction[2];
            this.program[0] = new AnyInstruction(n);
        } else {
            this.program = new Instruction[3];
            this.program[0] = new AnyInstruction((short) -n, (short) 2);
            this.program[1] = new Instruction(OpCode.IFail);
        }
        this.program[this.program.length - 1] = new Instruction(OpCode.IEnd);
    }

    public Pattern(boolean b) {
        if (b) {
            this.program = new Instruction[1];
            this.program[0] = new Instruction(OpCode.IEnd);
        } else {
            this.program = new Instruction[2];
            this.program[0] = new Instruction(OpCode.IFail);
            this.program[1] = new Instruction(OpCode.IEnd);
        }
    }

    public Pattern(Map<Object, Pattern> g, Object start) {
        Map<Object, Integer> positions = new HashMap<Object, Integer>(g.size());
        int totalsize = 3;
        for (Pattern p : g.values())
            totalsize += p.length() + 1;
        Instruction[] op = new Instruction[totalsize];
        int pos = 2;
        for (Map.Entry<Object, Pattern> pair : g.entrySet()) {
            Pattern p = pair.getValue();
            positions.put(pair.getKey(), pos);
            copy(p, op, pos);
            pos += p.length();
            op[pos++] = new Instruction(OpCode.IRet);
        }
        op[op.length - 1] = new Instruction(OpCode.IEnd);
        op[0] = new Instruction(OpCode.ICall, positions.get(start).shortValue());
        op[1] = new Instruction(OpCode.IJmp, (short) (op.length - 2));
        for (int i = 0; i < op.length; i++)
            if (op[i].code == OpCode.IOpenCall) {
                OpenCallInstruction oc = (OpenCallInstruction) op[i];
                op[i] = new Instruction(OpCode.ICall, (short) (positions.get(oc.name).shortValue() - i));
            }
        this.program = op;
    }

    public Pattern(RuntimeMatcher func) {
        this.program = new Instruction[3];
        this.program[0] = new CaptureInstruction(OpCode.IOpenCapture, CapKind.Cruntime, func);
        this.program[1] = new CaptureInstruction(OpCode.ICloseRunTime, CapKind.Cclose);
        this.program[2] = new Instruction(OpCode.IEnd);
    }

    public Pattern(Instruction[] program) {
        this.program = program;
    }

    static void copy(Pattern from, Instruction[] to, int start) {
        System.arraycopy(from.program, 0, to, start, from.length());
    }

    static Pattern localeHelper(byte cat) {
        Instruction[] op = new Instruction[2];
        op[0] = new LocaleInstruction(cat);
        op[1] = new Instruction(OpCode.IEnd);
        return new Pattern(op);
    }

    public static Pattern locale(String cat) {
        if (cat.equals("alnum")) return localeHelper(LocaleInstruction.alnum);
        if (cat.equals("alpha")) return localeHelper(LocaleInstruction.alpha);
        if (cat.equals("cntrl")) return localeHelper(LocaleInstruction.cntrl);
        if (cat.equals("digit")) return localeHelper(LocaleInstruction.digit);
        if (cat.equals("graph")) return localeHelper(LocaleInstruction.graph);
        if (cat.equals("lower")) return localeHelper(LocaleInstruction.lower);
        if (cat.equals("print")) return localeHelper(LocaleInstruction.print);
        if (cat.equals("punct")) return localeHelper(LocaleInstruction.punct);
        if (cat.equals("space")) return localeHelper(LocaleInstruction.space);
        if (cat.equals("upper")) return localeHelper(LocaleInstruction.upper);
        if (cat.equals("xdigit")) return localeHelper(LocaleInstruction.xdigit);
        return null;
    }

    public static Pattern C(Pattern p1) {
        return Pattern.caphelper(p1, CapKind.Csimple, null);
    }

    public static Pattern Carg(int n) {
        return Pattern.caphelper(new Pattern(""), CapKind.Carg, n);
    }

    public static Pattern Cp() {
        return Pattern.caphelper(new Pattern(""), CapKind.Cposition, null);
    }

    public static Pattern Cb(String name) {
        return Pattern.caphelper(new Pattern(""), CapKind.Cbackref, name);
    }

    public static Pattern Cc(Object[] values) {
        /*Instruction[] op = new Instruction[values.length * 2 + 1];
        int pos = 0;
        for(int i = 0; i < values.length; i++) {
            op[pos++] = new CaptureInstruction(OpCode.IOpenCapture, CapKind.Cconst, values[i]);
            op[pos++] = new CaptureInstruction(OpCode.ICloseCapture, CapKind.Cclose, null);
        }*/
        Instruction[] op = new Instruction[3];
        op[0] = new CaptureInstruction(OpCode.IOpenCapture, CapKind.Cconst, values);
        op[1] = new CaptureInstruction(OpCode.ICloseCapture, CapKind.Cclose, null);
        op[2] = new Instruction(OpCode.IEnd);
        return new Pattern(op);
    }

    public static Pattern Cf(Pattern p1, FoldFunction func) {
        return Pattern.caphelper(p1, CapKind.Cfold, func);
    }

    public static Pattern Cg(Pattern p1, String name) {
        return Pattern.caphelper(p1, CapKind.Cgroup, name);
    }

    public static Pattern Cs(Pattern p1) {
        return Pattern.caphelper(p1, CapKind.Csubst, null);
    }

    public static Pattern Ct(Pattern p1) {
        return Pattern.caphelper(p1, CapKind.Ctable, null);
    }

    public static Pattern Cstr(Pattern p1, String str) {
        return Pattern.caphelper(p1, CapKind.Cstring, str);
    }

    public static Pattern Cquery(Pattern p1, Map tab) {
        return Pattern.caphelper(p1, CapKind.Cquery, tab);
    }

    public static Pattern Cfunc(Pattern p1, CaptureFunction func) {
        return Pattern.caphelper(p1, CapKind.Cfunction, func);
    }

    static Pattern caphelper(Pattern p1, CapKind kind, Object data) {
        Instruction[] op = new Instruction[3 + p1.length()];
        op[0] = new CaptureInstruction(OpCode.IOpenCapture, kind, data);
        copy(p1, op, 1);
        op[op.length - 2] = new CaptureInstruction(OpCode.ICloseCapture, CapKind.Cclose);
        op[op.length - 1] = new Instruction(OpCode.IEnd);
        return new Pattern(op);
    }

    public static Pattern Cmt(Pattern p1, RuntimeMatcher func) {
        Instruction[] op = new Instruction[3 + p1.length()];
        op[0] = new CaptureInstruction(OpCode.IOpenCapture, CapKind.Cruntime, func);
        copy(p1, op, 1);
        op[op.length - 2] = new CaptureInstruction(OpCode.ICloseRunTime, CapKind.Cclose);
        op[op.length - 1] = new Instruction(OpCode.IEnd);
        return new Pattern(op);
    }

    public static Pattern concat(Pattern p1, Pattern p2) {
        if (p1.isfail() || p2.issucc())
            return p1;
        else if (p2.isfail() || p1.issucc())
            return p2;
        else if (p1.isany() && p2.isany()) {
            AnyInstruction a1 = (AnyInstruction) p1.program[0];
            AnyInstruction a2 = (AnyInstruction) p2.program[0];
            return new Pattern((short) (a1.n + a2.n));
        } else {
            Instruction[] op = new Instruction[p1.length() + p2.length() + 1];
            copy(p1, op, 0);
            int j = p1.length();
            copy(p2, op, p1.length());
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        }
    }

    public static Pattern diff(Pattern p1, Pattern p2) {
        if (p1.ischarset() && p2.ischarset()) {
            Instruction[] op = new Instruction[2];
            op[0] = new DiffInstruction((TestInstruction) p1.program[0],
                    (TestInstruction) p2.program[0]);
            op[1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        } else if (p2.isheadfail()) {
            // TODO: headfail opt for -
            return null;
        } else {
            return concat(not(p2), p1);
        }
    }

    public static Pattern not(Pattern p1) {
        if (p1.isfail()) {
            Instruction[] op = new Instruction[1];
            op[0] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        } else if (p1.issucc()) {
            Instruction[] op = new Instruction[2];
            op[0] = new Instruction(OpCode.IFail);
            op[1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        } else {
            Instruction[] op = new Instruction[p1.length() + 3];
            op[0] = new CountInstruction(OpCode.IChoice, (short) 0, (short) (p1.length() + 2));
            copy(p1, op, 1);
            op[op.length - 2] = new Instruction(OpCode.IFailTwice);
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        }
    }

    public static Pattern and(Pattern p1) {
        if (p1.isfail() || p1.issucc()) {
            return p1;
        } else if (p1.ischarset()) {
            Instruction[] op = new Instruction[p1.length() + 2];
            copy(p1, op, 0);
            op[op.length - 2] = new CountInstruction(OpCode.IBack, (short) 1);
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        } else {
            Instruction[] op = new Instruction[p1.length() + 4];
            op[0] = new CountInstruction(OpCode.IChoice, (short) 0, (short) (p1.length() + 2));
            copy(p1, op, 1);
            op[op.length - 3] = new Instruction(OpCode.IBackCommit, (short) 2);
            op[op.length - 2] = new Instruction(OpCode.IFail);
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        }
    }

    public static Pattern B(Pattern p1, short n) {
        if (!p1.nocalls())
            throw new RuntimeException("lookbehind pattern cannot contain non terminals");
        if (p1.isfail() || p1.issucc())
            return p1;
        else if (n == 1 && p1.ischarset()) {
            Instruction[] op = new Instruction[p1.length() + 2];
            op[0] = new CountInstruction(OpCode.IBack, (short) 1);
            copy(p1, op, 1);
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        } else {
            Instruction[] op = new Instruction[p1.length() + 5];
            op[0] = new CountInstruction(OpCode.IChoice, (short) 0, (short) (p1.length() + 3));
            op[1] = new CountInstruction(OpCode.IBack, n);
            copy(p1, op, 2);
            op[op.length - 3] = new Instruction(OpCode.IBackCommit, (short) 2);
            op[op.length - 2] = new Instruction(OpCode.IFail);
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        }
    }

    public static Pattern plus(Pattern p1, Pattern p2) {
        if (p1.isfail())
            return p2;
        else if (p2.isfail() || p1.issucc())
            return p1;
        else {
            Instruction[] op = new Instruction[p1.length() + p2.length() + 3];
            op[0] = new CountInstruction(OpCode.IChoice, (short) 0, (short) (p1.length() + 2));
            copy(p1, op, 1);
            op[p1.length() + 1] = new Instruction(OpCode.ICommit, (short) (p2.length() + 1));
            copy(p2, op, p1.length() + 2);
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        }
    }

    public static Pattern star(Pattern p1, short n) {
        if (n >= 0) {
            if (p1.ischarset()) {
                Instruction[] op = new Instruction[n + 2];
                int pos = 0;
                for (int i = 0; i < n; i++) {
                    copy(p1, op, pos);
                    pos += p1.length();
                }
                op[pos] = new SpanInstruction((TestInstruction) (p1.program[0]));
                op[op.length - 1] = new Instruction(OpCode.IEnd);
                return new Pattern(op);
            } else {
                Instruction[] op = new Instruction[(n + 1) * p1.length() + 3];
                int pos = 0;
                for (int i = 0; i < n; i++) {
                    copy(p1, op, pos);
                    pos += p1.length();
                }
                op[pos] = new CountInstruction(OpCode.IChoice, (short) 0, (short) (p1.length() + 2));
                copy(p1, op, pos + 1);
                op[op.length - 2] = new CountInstruction(OpCode.IPartialCommit, (short) 0, (short) (-p1.length()));
                op[op.length - 1] = new Instruction(OpCode.IEnd);
                return new Pattern(op);
            }
        } else {
            n = (short) -n;
            Instruction[] op = new Instruction[n * (p1.length() + 1) + 2];
            op[0] = new CountInstruction(OpCode.IChoice, (short) 0, (short) (1 + n * (p1.length() + 1)));
            int pos = 1;
            for (int i = 0; i < n; i++) {
                copy(p1, op, pos);
                pos += p1.length();
                op[pos++] = new CountInstruction(OpCode.IPartialCommit, (short) 0, (short) 1);
            }
            op[op.length - 2] = new CountInstruction(OpCode.ICommit, (short) 0, (short) 1);
            op[op.length - 1] = new Instruction(OpCode.IEnd);
            return new Pattern(op);
        }
    }

    public static Pattern V(Object name) {
        Instruction[] op = new Instruction[2];
        op[0] = new OpenCallInstruction(name);
        op[1] = new Instruction(OpCode.IEnd);
        return new Pattern(op);
    }

    public static Pattern R(String[] ranges) {
        Instruction[] op = new Instruction[2];
        op[0] = new RangeInstruction(OpCode.ISet, ranges);
        op[1] = new Instruction(OpCode.IEnd);
        return new Pattern(op);
    }

    public static Pattern S(String set) {
        Instruction[] op = new Instruction[2];
        op[0] = new CharsetInstruction(OpCode.ISet, set);
        op[1] = new Instruction(OpCode.IEnd);
        return new Pattern(op);
    }

    public int length() {
        return this.program.length - 1;
    }

    public boolean nocalls() {
        for (Instruction aProgram : this.program)
            if (aProgram.code == OpCode.IOpenCall) return false;
        return true;
    }

    public boolean isheadfail() {
        return false;
    }

    public boolean isany() {
        return this.program[0].code == OpCode.IAny && this.program[1].code == OpCode.IEnd;
    }

    public boolean isfail() {
        return this.program[0].code == OpCode.IFail;
    }

    public boolean issucc() {
        return this.program[0].code == OpCode.IEnd;
    }

    public boolean ischarset() {
        return this.program[0] instanceof TestInstruction &&
                this.program[1].code == OpCode.IEnd &&
                ((TestInstruction) this.program[0]).single();
    }

    public MatcherResult match(String subject, int pos, Object[] args) {
        Stack<Capture> capture = new Stack<Capture>();
        MatcherResult res = new MatcherResult();
        char[] subj = subject.toCharArray();
        res.pos = VM.match(subj, pos, this.program, capture, args);
        if (!capture.elementAt(0).isclose()) {
            ArrayList values = new ArrayList();
            CapState cs = new CapState(capture, 0, subj, pos, args);
            do {
                Object[] caps = VM.getcapture(cs);
                if (caps != null)
                    Collections.addAll(values, caps);
            } while (!cs.curr.isclose());
            res.values = values.toArray();
        }
        return res;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < this.program.length; i++) {
            buf.append(i);
            buf.append(": ");
            buf.append(this.program[i]);
            buf.append("\n");
        }
        return buf.toString();
    }
}
