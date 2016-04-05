package lpegj;

interface PropCodes {
    static final int ISJMP = 0x1;
    static final int ISCHECK = 0x2;
    static final int ISFIXCHECK = 0x4;
    static final int ISNOFAIL = 0x8;
    static final int ISCAPTURE = 0x10;
    static final int ISMOVABLE = 0x20;
    static final int ISFENVOFF = 0x40;
}

public enum OpCode implements PropCodes {
    IAny(ISCHECK | ISFIXCHECK | ISJMP),
    IChar(ISCHECK | ISFIXCHECK | ISJMP),
    ISet(ISCHECK | ISFIXCHECK | ISJMP),
    ISpan(ISNOFAIL),
    IBack(0),
    IRet(0),
    IEnd(0),
    IChoice(ISJMP),
    IJmp(ISJMP | ISNOFAIL),
    ICall(ISJMP),
    IOpenCall(ISFENVOFF),
    ICommit(ISJMP),
    IPartialCommit(ISJMP),
    IBackCommit(ISJMP),
    IFailTwice(0),
    IFail(0),
    IGiveup(0),
    IFunc(ISCHECK | ISJMP),
    IFullCapture(ISCAPTURE | ISNOFAIL | ISFENVOFF),
    IEmptyCapture(ISCAPTURE | ISNOFAIL | ISMOVABLE),
    IOpenCapture(ISCAPTURE | ISNOFAIL | ISMOVABLE | ISFENVOFF),
    ICloseCapture(ISCAPTURE | ISNOFAIL | ISMOVABLE | ISFENVOFF),
    ICloseRunTime(ISCAPTURE | ISFENVOFF);

    static final String[] names = new String[]{
            "any", "char", "set", "span", "back",
            "ret", "end",
            "choice", "jmp", "call", "open_call",
            "commit", "partial_commit", "back_commit", "failtwice", "fail", "giveup",
            "func",
            "fullcapture", "emptycapture", "opencapture",
            "closecapture", "closeruntime"
    };

    private int properties;

    OpCode(int properties) {
        this.properties = properties;
    }

    public boolean isprop(Instruction op, int prop) {
        return (op.code.properties & prop) != 0;
    }

    public String toString() {
        return names[this.ordinal()];
    }
}
