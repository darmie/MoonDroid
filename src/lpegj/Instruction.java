package lpegj;

import java.util.Arrays;

class Instruction {
    OpCode code;
    short offset;

    Instruction(OpCode code, short offset) {
        this.code = code;
        this.offset = offset;
    }

    Instruction(OpCode code) {
        this(code, (short) 0);
    }

    public String toString() {
        if (this.offset != 0)
            return this.code.toString() + " -> " + this.offset;
        else
            return this.code.toString();
    }
}

interface TestInstruction {
    boolean test(char c);

    boolean single();
}

class CountInstruction extends Instruction {
    short n;

    CountInstruction(OpCode code, short n) {
        super(code);
        this.n = n;
    }

    CountInstruction(OpCode code, short n, short offset) {
        super(code, offset);
        this.n = n;
    }

    public String toString() {
        if (this.offset != 0)
            return this.code.toString() + " " + this.n + " -> " + this.offset;
        else
            return this.code.toString() + " " + this.n;
    }
}

class AnyInstruction extends CountInstruction implements TestInstruction {
    AnyInstruction(short n) {
        super(OpCode.IAny, n);
    }

    AnyInstruction(short n, short offset) {
        super(OpCode.IAny, n, offset);
    }

    public boolean test(char c) {
        return true;
    }

    public boolean single() {
        return n == 1;
    }
}

class CharInstruction extends Instruction implements TestInstruction {
    char c;

    CharInstruction(OpCode code, char c) {
        super(code);
        this.c = c;
    }

    CharInstruction(char c) {
        this(OpCode.IChar, c);
    }

    public boolean test(char c) {
        return this.c == c;
    }

    public boolean single() {
        return true;
    }

    public String toString() {
        if (this.offset != 0)
            return this.code.toString() + " " + this.c + " -> " + this.offset;
        else
            return this.code.toString() + " " + this.c;
    }
}

class CharsetInstruction extends Instruction implements TestInstruction {
    char[] charset;

    CharsetInstruction(OpCode code, String charset) {
        this(code, charset.toCharArray());
    }

    CharsetInstruction(OpCode code, char[] charset) {
        super(code);
        this.charset = charset;
        Arrays.sort(this.charset);
    }

    public boolean test(char c) {
        return Arrays.binarySearch(charset, c) >= 0;
    }

    public boolean single() {
        return true;
    }

    public String toString() {
        if (this.offset != 0)
            return this.code.toString() + " " + String.copyValueOf(this.charset) + " -> " + this.offset;
        else
            return this.code.toString() + " " + String.copyValueOf(this.charset);
    }
}

class RangeInstruction extends Instruction implements TestInstruction {
    char[][] ranges;

    RangeInstruction(OpCode code, String[] ranges) {
        super(code);
        this.ranges = new char[ranges.length][];
        for (int i = 0; i < ranges.length; i++)
            this.ranges[i] = ranges[i].toCharArray();
    }

    RangeInstruction(OpCode code, char[][] ranges) {
        super(code);
        this.ranges = ranges;
    }

    public boolean test(char c) {
        for (char[] range : ranges) {
            if ((c >= range[0]) && (c <= range[1])) return true;
        }
        return false;
    }

    public boolean single() {
        return true;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("range");
        buf.append(" ");
        for (char[] range : ranges)
            buf.append(String.copyValueOf(range)).append(" ");
        if (this.offset != 0)
            buf.append(" -> ").append(this.offset);
        return buf.toString();
    }
}

class DiffInstruction extends Instruction implements TestInstruction {
    TestInstruction t1, t2;

    DiffInstruction(TestInstruction t1, TestInstruction t2) {
        super(OpCode.ISet);
        this.t1 = t1;
        this.t2 = t2;
    }

    public boolean test(char c) {
        return t1.test(c) && !t2.test(c);
    }

    public boolean single() {
        return t1.single() && t2.single();
    }

    public String toString() {
        return "diff " + t1.toString() + ", " + t2.toString();
    }
}

class CaptureInstruction extends Instruction {
    CapKind kind;
    Object data;

    CaptureInstruction(OpCode code, CapKind kind, Object data) {
        super(code);
        this.kind = kind;
        this.data = data;
    }

    CaptureInstruction(OpCode code, CapKind kind) {
        this(code, kind, null);
    }

    public String toString() {
        if (this.data == null)
            return this.code.toString() + " " + this.kind.toString();
        else
            return this.code.toString() + " " + this.kind.toString() + "[" + this.data + "]";
    }
}

class SpanInstruction extends Instruction implements TestInstruction {
    TestInstruction inst;

    SpanInstruction(TestInstruction inst) {
        super(OpCode.ISpan);
        this.inst = inst;
    }

    public boolean test(char c) {
        return inst.test(c);
    }

    public boolean single() {
        return inst.single();
    }

    public String toString() {
        return "span " + inst.toString();
    }
}

class OpenCallInstruction extends Instruction {
    Object name;

    OpenCallInstruction(Object name) {
        super(OpCode.IOpenCall);
        this.name = name;
    }
}

class LocaleInstruction extends Instruction implements TestInstruction {
    static final byte alnum = 0;
    static final byte alpha = 1;
    static final byte cntrl = 2;
    static final byte digit = 3;
    static final byte graph = 4;
    static final byte lower = 5;
    static final byte print = 6;
    static final byte punct = 7;
    static final byte space = 8;
    static final byte upper = 9;
    static final byte xdigit = 10;

    static String[] lcats = new String[]{
            "alnum", "alpha", "cntrl", "digit", "graph", "lower", "print", "punct", "space", "upper", "xdigit"
    };

    byte cat;

    static boolean isPunct(char c) {
        boolean isPunct = c == '-' || c == '.' || c == ':' || c == '_';
        boolean isUnicodePunct = c == '\u00b7' || c == '\u0387' || c == '\u06dd' || c == '\u06de';
        return isPunct || isUnicodePunct;
    }

    LocaleInstruction(byte cat) {
        super(OpCode.ISet);
        this.cat = cat;
    }

    public boolean test(char c) {
        switch (cat) {
            case alnum:
                return Character.isLetterOrDigit(c);
            case alpha:
                return Character.isLetter(c);
            case cntrl:
                return Character.isISOControl(c);
            case digit:
                return Character.isDigit(c);
            case graph:
                return Character.isDefined(c) && !Character.isWhitespace(c);
            case lower:
                return Character.isLowerCase(c);
            case print:
                return Character.isDefined(c);
            case punct:
                return isPunct(c);
            case space:
                return Character.isWhitespace(c);
            case upper:
                return Character.isUpperCase(c);
            case xdigit:
                return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            default:
                return false;
        }
    }

    public boolean single() {
        return true;
    }

    public String toString() {
        if (this.offset == 0)
            return this.code.toString() + " " + lcats[this.cat];
        else
            return this.code.toString() + " " + lcats[this.cat] + " -> " + this.offset;
    }
}