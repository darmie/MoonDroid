package org.moon.mooonlanding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lpegj.CaptureFunction;
import lpegj.FoldFunction;
import lpegj.MatcherResult;
import lpegj.Pattern;
import lpegj.RuntimeMatcher;
import lpegj.Table;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

class LuaMap implements Map {
    LuaTable tab;

    LuaMap(LuaTable tab) {
        this.tab = tab;
    }

    public int size() {
        return tab.keyCount();
    }

    public boolean isEmpty() {
        return tab.keyCount() == 0;
    }

    public boolean containsKey(Object o) {
        return tab.get(lpeg.toluaval(o)) != LuaValue.NIL;
    }

    public boolean containsValue(Object o) {
        return tab.get(lpeg.toluaval(o)) != LuaValue.NIL;
    }

    public Object get(Object o) {
        Object res = tab.get(lpeg.toluaval(o));
        return res == LuaValue.NIL ? null : res;
    }

    public Object put(Object o, Object o1) {
        tab.set(lpeg.toluaval(o), lpeg.toluaval(o1));
        return o1;
    }

    public Object remove(Object o) {
        tab.set(lpeg.toluaval(o), LuaValue.NIL);
        return o;
    }

    public void putAll(Map map) {
        for (Object key : map.keySet())
            this.put(key, map.get(key));
    }

    public void clear() {
        tab.presize(0, 0);
    }

    public Set keySet() {
        return new HashSet(Arrays.asList(tab.keys()));
    }

    public Collection values() {
        ArrayList list = new ArrayList(tab.length());
        for (Object key : tab.keys())
            list.add(this.get(key));
        return list;
    }

    public Set entrySet() {
        HashMap hash = new HashMap(tab.length());
        for (Object key : tab.keys())
            hash.put(key, this.get(key));
        return hash.entrySet();
    }
}

class LpegMatcher implements RuntimeMatcher {
    LuaValue func;

    LpegMatcher(LuaValue func) {
        this.func = func;
    }

    public MatcherResult match(char[] subject, int pos, Object[] values) {
        LuaValue[] args = new LuaValue[(values != null ? values.length : 0) + 2];
        args[0] = LuaValue.valueOf(String.copyValueOf(subject));
        args[1] = LuaValue.valueOf(pos + 1);
        if (values != null)
            for (int i = 0; i < values.length; i++)
                args[i + 2] = lpeg.toluaval(values[i]);
        Varargs res = func.invoke(args);
        int nargs = res.narg();
        if (nargs == 0 || res.isnil(1) || res.arg1() == LuaValue.FALSE) {
            return null;
        } else {
            MatcherResult mres = new MatcherResult();
            if (res.arg1() == LuaValue.TRUE) {
                mres.pos = pos;
            } else
                mres.pos = res.toint(1) - 1;
            mres.values = new Object[nargs - 1];
            for (int i = 2; i <= nargs; i++)
                mres.values[i - 2] = res.arg(i);
            return mres;
        }
    }
}

class LpegFolder implements FoldFunction {
    LuaValue func;

    LpegFolder(LuaValue func) {
        this.func = func;
    }

    public Object fold(Object acc, Object[] values) {
        LuaValue[] args = new LuaValue[values.length + 1];
        args[0] = lpeg.toluaval(acc);
        for (int i = 1; i < args.length; i++) {
            args[i] = lpeg.toluaval(values[i - 1]);
        }
        Varargs res = this.func.invoke(args);
        return res.arg1();
    }
}

class LpegCaptureFunc implements CaptureFunction {
    LuaValue func;

    LpegCaptureFunc(LuaValue func) {
        this.func = func;
    }

    public Object[] invoke(Object[] values) {
        LuaValue[] args = new LuaValue[values.length];
        for (int i = 0; i < args.length; i++)
            args[i] = lpeg.toluaval(values[i]);
        Varargs res = func.invoke(args);
        LuaValue[] results = new LuaValue[res.narg()];
        for (int i = 0; i < results.length; i++)
            results[i] = res.arg(i + 1);
        return results;
    }
}

public class lpeg extends VarArgFunction {
    static final int MATCH = 1;
    static final int TYPE = 2;
    static final int VERSION = 3;
    static final int SETMAXSTACK = 4;
    static final int P = 5;
    static final int B = 6;
    static final int R = 7;
    static final int S = 8;
    static final int V = 9;
    static final int LOCALE = 10;
    static final int C = 11;
    static final int CARG = 12;
    static final int CB = 13;
    static final int CC = 14;
    static final int CF = 15;
    static final int CG = 16;
    static final int CP = 17;
    static final int CS = 18;
    static final int CT = 19;
    static final int CMT = 20;
    static final int PRINT = 21;
    static final int PLUS = 22;
    static final int CONCAT = 23;
    static final int DIFF = 24;
    static final int RCAP = 25;
    static final int AND = 26;
    static final int NOT = 27;
    static final int EXP = 28;

    static String[] functions = new String[]{
            "match", "type", "version", "setmaxstack", "P", "B", "R", "S",
            "V", "locale", "C", "Carg", "Cb", "Cc", "Cf", "Cg", "Cp", "Cs",
            "Ct", "Cmt", "print", "plus", "concat", "diff", "rcap", "and",
            "not", "exp"
    };

    static String[] lcats = new String[]{
            "alnum", "alpha", "cntrl", "digit", "graph", "lower", "print", "punct", "space", "upper", "xdigit"
    };
    
    

    static LuaValue toluaval(Object val) {
        if (val instanceof LuaValue)
            return (LuaValue) val;
        else if (val instanceof String)
            return valueOf((String) val);
        else if (val instanceof Table) {
            Table tab = (Table) val;
            LuaTable t = tableOf(tab.array.length, tab.hash.size());
            for (int i = 0; i < tab.array.length; i++) {
                t.rawset(i + 1, toluaval(tab.array[i]));
            }
            for (Map.Entry<String, Object> entry : tab.hash.entrySet())
                t.rawset(entry.getKey(), toluaval(entry.getValue()));
            return t;
        } else if (val instanceof Integer)
            return valueOf(((Integer) val).intValue());
        else
            return userdataOf(val);
    }

    static LuaValue[] toluaval(Object[] values) {
        LuaValue[] res = new LuaValue[values.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = toluaval(values[i]);
        }
        return res;
    }

    static LuaValue pattmeta;

    public lpeg() {}

    Pattern pattP(LuaValue arg) {
        if (arg.isuserdata(Pattern.class))
            return (Pattern) arg.touserdata(Pattern.class);
        else if (arg.type() == LuaValue.TNUMBER)
            return new Pattern((short) arg.toint());
        else if (arg.type() == LuaValue.TSTRING)
            return new Pattern(arg.toString());
        else if (arg.type() == LuaValue.TBOOLEAN)
            return new Pattern(arg.toboolean());
        else if (arg.type() == LuaValue.TTABLE) {
            Map<Object, Pattern> grammar = new HashMap<Object, Pattern>();
            LuaTable tab = arg.checktable();
            Object start = 1;
            LuaValue[] keys = tab.keys();
            for (LuaValue key : keys) {
                Object nt;
                LuaValue rule = tab.get(key);
                if (key.type() == LuaValue.TNUMBER)
                    nt = key.toint();
                else if (key.type() == LuaValue.TSTRING)
                    nt = key.tojstring();
                else nt = key;
                if (nt.equals(1) &&
                        rule.type() == LuaValue.TSTRING)
                    grammar.put(nt, Pattern.V(rule.tojstring()));
                else
                    grammar.put(nt, pattP(rule));
            }
            return new Pattern(grammar, start);
        } else if (arg.type() == LuaValue.TFUNCTION) {
            RuntimeMatcher func = new LpegMatcher(arg);
            return new Pattern(func);
        } else return null;
    }

    void initmeta(LuaValue env) {
    	
        pattmeta = tableOf();
        pattmeta.set("__index", env.get("lpeg"));
        pattmeta.set("__add", env.get("lpeg").get("plus"));
        pattmeta.set("__sub", env.get("lpeg").get("diff"));
        pattmeta.set("__mul", env.get("lpeg").get("concat"));
        pattmeta.set("__div", env.get("lpeg").get("rcap"));
        pattmeta.set("__len", env.get("lpeg").get("and"));
        pattmeta.set("__unm", env.get("lpeg").get("not"));
        pattmeta.set("__pow", env.get("lpeg").get("exp"));
    }

    public Varargs invoke(Varargs args) {
    	
        switch (opcode) {
            case 0: {
                LuaValue t = tableOf();
                this.bind(t, lpeg.class, lpeg.functions, 1);
                t.set("lpeg", t);
                initmeta(t);
                return t;
            }
            case MATCH: {
                Pattern p = pattP(args.arg1());
                String s = args.checkjstring(2);
                int pos = args.optint(3, 1) - 1;
                if (pos < 0) pos = s.length() + pos + 1;
                if (pos < 0) pos = 0;
                int nargs = args.narg();
                Object[] margs;
                if (nargs > 3) {
                    margs = new Object[nargs - 3];
                    for (int i = 0; i < margs.length; i++)
                        margs[i] = args.arg(i + 4);
                } else
                    margs = null;
                try {
                    MatcherResult res = p.match(s, pos, margs);
                    if (res.values != null && res.values.length > 0) {
                        return varargsOf(toluaval(res.values));
                    } else if (res.pos >= 0) {
                        return valueOf(res.pos + 1);
                    } else
                        return NIL;
                } catch (Exception e) {
                    StackTraceElement[] trace = e.getStackTrace();
                    StringBuilder buf = new StringBuilder();
                    buf.append(e.toString()).append("\n    ");
                    for (StackTraceElement frame : trace)
                        buf.append(frame.toString()).append("\n    ");
                    return error(buf.toString());
                }
            }
            case TYPE: {
                if (args.arg(1).isuserdata(Pattern.class))
                    return valueOf("pattern");
                else
                    return NIL;
            }
            case VERSION: {
                return valueOf("0.10.1");
            }
            case SETMAXSTACK: {
                return NIL;
            }
            case LOCALE: {
                LuaValue tab = args.opttable(1, tableOf());
                for (String cat : lcats)
                    tab.set(cat, userdataOf(Pattern.locale(cat), pattmeta));
                return tab;
            }
            case P: {
                return userdataOf(pattP(args.arg1()), pattmeta);
            }
            case S: {
                return userdataOf(Pattern.S(args.checkjstring(1)), pattmeta);
            }
            case V: {
                Object nt;
                LuaValue key = args.arg1();
                if (key.type() == LuaValue.TNUMBER)
                    nt = key.toint();
                else if (key.type() == LuaValue.TSTRING)
                    nt = key.tojstring();
                else nt = key;
                return userdataOf(Pattern.V(nt), pattmeta);
            }
            case CB: {
                return userdataOf(Pattern.Cb(args.checkjstring(1)), pattmeta);
            }
            case CARG: {
                return userdataOf(Pattern.Carg(args.checkint(1) - 1), pattmeta);
            }
            case C: {
                return userdataOf(Pattern.C(pattP(args.arg1())), pattmeta);
            }
            case CC: {
                LuaValue[] vals = new LuaValue[args.narg()];
                for (int i = 0; i < vals.length; i++)
                    vals[i] = args.arg(i + 1);
                return userdataOf(Pattern.Cc(vals), pattmeta);
            }
            case CG: {
                return userdataOf(Pattern.Cg(pattP(args.arg1()), args.optjstring(2, null)), pattmeta);
            }
            case CF: {
                return userdataOf(Pattern.Cf(pattP(args.arg1()), new LpegFolder(args.checkfunction(2))), pattmeta);
            }
            case CMT: {
                return userdataOf(Pattern.Cmt(pattP(args.arg1()), new LpegMatcher(args.checkfunction(2))), pattmeta);
            }
            case CP: {
                return userdataOf(Pattern.Cp(), pattmeta);
            }
            case CS: {
                return userdataOf(Pattern.Cs(pattP(args.arg1())), pattmeta);
            }
            case CT: {
                return userdataOf(Pattern.Ct(pattP(args.arg1())), pattmeta);
            }
            case RCAP: {
                Pattern p = pattP(args.arg1());
                LuaValue arg2 = args.arg(2);
                if (arg2.isstring())
                    return userdataOf(Pattern.Cstr(p, arg2.tojstring()), pattmeta);
                else if (arg2.isfunction())
                    return userdataOf(Pattern.Cfunc(p, new LpegCaptureFunc(arg2)), pattmeta);
                else if (arg2.istable())
                    return userdataOf(Pattern.Cquery(p, new LuaMap(arg2.checktable())), pattmeta);
                else
                    return error("invalid type for capture");
            }
            case PLUS: {
                return userdataOf(Pattern.plus(pattP(args.arg1()),
                        pattP(args.arg(2))), pattmeta);
            }
            case PRINT: {
                return valueOf(pattP(args.arg1()).toString());
            }
            case B: {
                try {
                    return userdataOf(Pattern.B(pattP(args.arg1()), (short) args.optint(2, 1)), pattmeta);
                } catch (Exception e) {
                    return error(e.toString());
                }
            }
            case R: {
                String[] ranges = new String[args.narg()];
                for (int i = 0; i < ranges.length; i++)
                    ranges[i] = args.checkjstring(i + 1);
                return userdataOf(Pattern.R(ranges), pattmeta);
            }
            case EXP: {
                return userdataOf(Pattern.star(pattP(args.arg1()), (short) args.checkint(2)), pattmeta);
            }
            case DIFF: {
                return userdataOf(Pattern.diff(pattP(args.arg1()),
                        pattP(args.arg(2))), pattmeta);
            }
            case CONCAT: {
                return userdataOf(Pattern.concat(pattP(args.arg1()),
                        pattP(args.arg(2))), pattmeta);
            }
            case AND: {
                return userdataOf(Pattern.and(pattP(args.arg1())), pattmeta);
            }
            case NOT: {
                return userdataOf(Pattern.not(pattP(args.arg1())), pattmeta);
            }
            default:
                return error("function not found");
        }
    }
}
