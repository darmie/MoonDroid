package lpegj;

public enum CapKind {
    Cclose, Cposition, Cconst, Cbackref, Carg, Csimple, Ctable, Cfunction,
    Cquery, Cstring, Csubst, Cfold, Cruntime, Cgroup;

    static final String[] modes = new String[]{"close", "position", "constant", "backref",
            "argument", "simple", "table", "function",
            "query", "string", "substitution", "fold",
            "runtime", "group"
    };

    static final CapKind[] kinds = new CapKind[]{
            Cclose, Cposition, Cconst, Cbackref, Carg, Csimple, Ctable, Cfunction,
            Cquery, Cstring, Csubst, Cfold, Cruntime, Cgroup
    };

    public String toString() {
        return modes[this.ordinal()];
    }

    public static CapKind fromOrdinal(int kind) {
        return kinds[kind];
    }
}
