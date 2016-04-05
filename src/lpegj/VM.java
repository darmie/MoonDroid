package lpegj;

import java.util.*;

class StackEntry {
    int pos;
    int inst;
    int capLevel;

    StackEntry(int pos, int inst, int capLevel) {
        this.pos = pos;
        this.inst = inst;
        this.capLevel = capLevel;
    }
}

class CapState {
    Stack<Capture> capture;
    int cap;
    int opos;
    char[] subject;
    Capture curr;
    Object[] args;
    HashMap<String, Integer> backs;

    CapState(Stack<Capture> capture, int cap, char[] subject, int opos, Object[] args) {
        this.capture = capture;
        this.cap = cap;
        this.subject = subject;
        this.opos = opos;
        this.curr = capture.elementAt(cap);
        this.args = args;
        this.backs = new HashMap<String, Integer>();
    }

    Capture next() {
        Capture prev = curr;
        cap++;
        curr = cap < capture.size() ? capture.elementAt(cap) : null;
        return prev;
    }
}

public class VM {
    static int findopen(Stack<Capture> capture, int cap) {
        int n = 0;
        for (; ; ) {
            cap--;
            if (capture.elementAt(cap).isclose()) n++;
            else if (n-- == 0) return cap;
        }
    }

    static int nextcap(Stack<Capture> capture, int cap) {
        int n = 0;
        for (; ; ) {
            cap++;
            if (capture.elementAt(cap).isclose()) {
                if (n-- == 0) return cap + 1;
            } else n++;
        }
    }

    static Object[] getcapture(CapState cs) {
        switch (cs.curr.kind) {
            case Cposition: {
                Object[] res = new Object[]{cs.next().pos + 1};
                cs.next();
                return res;
            }
            case Cconst: {
                Object[] res = (Object[]) cs.next().data;
                cs.next();
                return res;
            }
            case Carg: {
                Object[] res = new Object[]{cs.args[(Integer) cs.next().data]};
                cs.next();
                return res;
            }
            case Csimple: {
                Object[] values = getcaptures(cs, true);
                if (values.length > 1) {
                    Object last = values[values.length - 1];
                    System.arraycopy(values, 0, values, 1, values.length - 1);
                    values[0] = last;
                }
                return values;
            }
            case Cruntime: {
                ArrayList<Object> values = new ArrayList<Object>();
                Capture cap;
                cs.next();
                while (!(cap = cs.next()).isclose()) {
                    values.add(cap.data);
                    cs.next();
                }
                return values.toArray();
            }
            case Cstring: {
                StringBuilder buf = new StringBuilder();
                String scap = (String) cs.curr.data;
                int smatch = cs.curr.pos;
                Object[] values = getcaptures(cs);
                int ematch = cs.capture.elementAt(cs.cap - 1).pos;
                int start = 0;
                int end;
                while ((end = scap.indexOf('%', start)) != -1) {
                    buf.append(scap, start, end);
                    if (scap.charAt(end + 1) == '%') {
                        buf.append('%');
                        end++;
                    } else {
                        end++;
                        int idx = Integer.parseInt(scap.substring(end, end + 1));
                        if (idx == 0) {
                            buf.append(cs.subject, smatch, ematch - smatch);
                        } else {
                            buf.append(values[idx - 1]);
                        }
                    }
                    start = ++end;
                }
                buf.append(scap, start, scap.length());
                return new Object[]{buf.toString()};
            }
            case Csubst: {
                StringBuilder buf = new StringBuilder();
                int curr = cs.next().pos;
                while (!cs.curr.isclose()) {
                    int next = cs.curr.pos;
                    buf.append(cs.subject, curr, next - curr);
                    Object[] values = getcapture(cs);
                    if (values != null && values.length > 0) {
                        buf.append(values[0]);
                        curr = cs.capture.elementAt(cs.cap - 1).pos;
                    } else {
                        curr = next;
                    }
                }
                buf.append(cs.subject, curr, cs.curr.pos - curr);
                cs.next();
                return new Object[]{buf.toString()};
            }
            case Cgroup: {
                if (cs.curr.data == null) {
                    return getcaptures(cs, false);
                } else {
                    //cs.backs.put((String) cs.curr.data, cs.cap);
                    cs.cap = nextcap(cs.capture, cs.cap);
                    cs.curr = cs.capture.elementAt(cs.cap);
                    return null;
                }
            }
            case Cbackref: {
                int curr = cs.cap;
                String name = (String) cs.curr.data;
                Capture c = null;
                for (;;) {
                    if (--cs.cap < 0) break;
                    c = cs.capture.elementAt(cs.cap);
                    if (c.isclose()) {
                        cs.cap = findopen(cs.capture, cs.cap);
                        c = cs.capture.elementAt(cs.cap);
                    } else continue;
                    if (c.kind == CapKind.Cgroup &&
                            c.data.equals(name))
                        break;
                }
                Object[] values;
                if (cs.cap >= 0) {
                    cs.curr = c;
                    values = getcaptures(cs, false);
                } else throw new RuntimeException("backref not found: " + name);
                cs.cap = curr + 2;
                cs.curr = cs.capture.elementAt(cs.cap);
                return values;
            }
            case Ctable: {
                HashMap<String, Object> map = new HashMap<String, Object>();
                ArrayList<Object> list = new ArrayList<Object>();
                cs.next();
                while (!cs.curr.isclose()) {
                    if (cs.curr.kind == CapKind.Cgroup && cs.curr.data != null) {
                        String name = (String) cs.curr.data;
                        Object[] values = getcaptures(cs, false);
                        if (values.length == 0) continue;
                        map.put(name, values[0]);
                    } else {
                        Object[] values = getcapture(cs);
                        if (values.length == 0) continue;
                        Collections.addAll(list, values);
                    }
                }
                cs.next();
                return new Object[]{new Table(map, list.toArray())};
            }
            case Cquery: {
                Map map = (Map) cs.curr.data;
                Object[] values = getcaptures(cs, false);
                Object val = map.get(values[0]);
                return val == null ? (new Object[]{}) : (new Object[]{val});
            }
            case Cfold: {
                FoldFunction fold = (FoldFunction) cs.curr.data;
                cs.next();
                Object acc;
                Object[] values;
                if (cs.curr.isclose() || (values = getcapture(cs)) == null || values.length == 0)
                    throw new RuntimeException("no initial value for fold capture");
                acc = values[0];
                while (!cs.curr.isclose()) {
                    values = getcapture(cs);
                    acc = fold.fold(acc, values);
                }
                cs.next();
                return new Object[]{acc};
            }
            case Cfunction: {
                CaptureFunction func = (CaptureFunction) cs.curr.data;
                Object[] args = getcaptures(cs, false);
                return func.invoke(args);
            }
            case Cclose:
                throw new RuntimeException("BUG: reached a close capture");
        }
        return null;
    }

    static Object[] getcaptures(CapState cs) {
        Capture ocap = cs.next();
        ArrayList<Object> values = new ArrayList<Object>();
        while (!cs.curr.isclose()) {
            Object[] caps = getcapture(cs);
            if (caps != null && caps.length > 0)
                Collections.addAll(values, caps);
            else values.add(null);
        }
        cs.next();
        return values.toArray();
    }

    static Object[] getcaptures(CapState cs, boolean addextra) {
        Capture ocap = cs.next();
        ArrayList<Object> values = new ArrayList<Object>();
        while (!cs.curr.isclose()) {
            Object[] caps = getcapture(cs);
            if (caps != null)
                Collections.addAll(values, caps);
        }
        if (addextra || values.size() == 0)
            values.add(String.copyValueOf(cs.subject, ocap.pos, cs.curr.pos - ocap.pos));
        cs.next();
        return values.toArray();
    }

    public static int match(char[] subject, int s, Instruction[] program, Stack<Capture> capture, Object[] args) {
        Stack<StackEntry> stack = new Stack<StackEntry>();
        int p = 0;
        int e = subject.length;
        int o = s;
        stack.push(new StackEntry(-2, program.length - 1, 0));
        boolean fail = false;
        boolean condfailed = false;
        Instruction inst = null;
//        System.out.println("TRACE");
        for (; ; ) {
//            System.out.println(p + ": " + program[p]);
            if (condfailed) {
                condfailed = false;
                int f = inst.offset;
                if (f != 0) p += f;
                else fail = true;
            }
            if (fail) {
                fail = false;
                StackEntry back;
                do {
                    back = stack.pop();
                    s = back.pos;
                } while (s == -1);
                capture.setSize(back.capLevel);
                p = back.inst;
            }
            switch ((inst = program[p]).code) {
                case IEnd: {
                    capture.push(new Capture(0, null, CapKind.Cclose));
                    return s;
                }
                case IGiveup: {
                    return 0;
                }
                case IRet: {
                    StackEntry last = stack.pop();
                    p = last.inst;
                    continue;
                }
                case IAny: {
                    int n = ((CountInstruction) inst).n;
                    if (n + s <= e) {
                        p++;
                        s += n;
                    } else condfailed = true;
                    continue;
                }
                case IChar:
                case ISet: {
                    if (s < e && ((TestInstruction) inst).test(subject[s])) {
                        p++;
                        s++;
                    } else condfailed = true;
                    continue;
                }
                case IBack: {
                    int n = ((CountInstruction) inst).n;
                    if (n > s - o) {
                        fail = true;
                        continue;
                    }
                    s -= n;
                    p++;
                    continue;
                }
                case ISpan: {
                    TestInstruction ti = (TestInstruction) inst;
                    for (; s < e; s++) {
                        char c = subject[s];
                        if (!ti.test(c)) break;
                    }
                    p++;
                    continue;
                }
                case IFunc: {
                    // TODO: function pattern
                    throw new RuntimeException("not implemented");
                }
                case IJmp: {
                    p += inst.offset;
                    continue;
                }
                case IChoice: {
                    stack.push(new StackEntry(s - ((CountInstruction) inst).n, p + inst.offset, capture.size()));
                    p++;
                    continue;
                }
                case ICall: {
                    stack.push(new StackEntry(-1, p + 1, 0));
                    p += inst.offset;
                    continue;
                }
                case ICommit: {
                    stack.pop();
                    p += inst.offset;
                    continue;
                }
                case IPartialCommit: {
                    StackEntry top = stack.peek();
                    top.pos = s;
                    top.capLevel = capture.size();
                    p += inst.offset;
                    continue;
                }
                case IBackCommit: {
                    StackEntry top = stack.pop();
                    s = top.pos;
                    capture.setSize(top.capLevel);
                    p += inst.offset;
                    continue;
                }
                case IFailTwice: {
                    stack.pop();
                    /* go through */
                }
                case IFail: {
                    fail = true;
                    continue;
                }
                case ICloseRunTime: {
                    capture.push(new Capture(s, null, CapKind.Cclose));
                    int close = capture.size() - 1;
                    int open = findopen(capture, close);
                    Capture capopen = capture.elementAt(open);
                    Capture capclose = capture.elementAt(close);
                    RuntimeMatcher func = (RuntimeMatcher) capopen.data;
                    Object[] values = getcaptures(new CapState(capture, open, subject, o, args), false);
                    MatcherResult res = func.match(subject, s, values);
                    if (res != null) {
                        if (res.pos < s || res.pos > subject.length)
                            throw new RuntimeException("invalid position returned by match-time capture");
                        s = res.pos;
                        capture.setSize(open + 1);
                        if (res.values != null && res.values.length > 0) {
                            for (Object value : res.values) {
                                capture.add(new Capture(capopen.pos, value, CapKind.Cruntime));
                                capture.add(new Capture(s, null, CapKind.Cclose));
                            }
                        }
                        capture.add(new Capture(s, null, CapKind.Cclose));
                        p++;
                        continue;
                    } else {
                        fail = true;
                        continue;
                    }
                }
                case IOpenCapture:
                case ICloseCapture:
                case IEmptyCapture:
                case IFullCapture: {
                    CaptureInstruction ci = (CaptureInstruction) inst;
                    Capture cap = new Capture(s, ci.data, ci.kind);
                    capture.push(cap);
                    p++;
                    continue;
                }
                case IOpenCall: {
                    throw new RuntimeException("reference to rule outside grammar");
                }
            }
        }
    }
}
