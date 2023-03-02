package artmann.microjava.symtab;

import java.util.LinkedList;

public final class Scope {

    private Scope outer;
    private LinkedList<Obj> locals = new LinkedList<>();
    private int nVars;
    public Scope(Scope outer) {
        this.outer = outer;
    }
    public int nVars() {
        return nVars;
    }

    public Obj findGlobal(String name) {
        Obj res = findLocal(name);
        if (res == null && outer != null) {
            res = outer.findGlobal(name);
        }
        return res;
    }

    public Obj findLocal(String name) {
        for(Obj o : locals) {
            if(o.name.equals(name)) {
                return o;
            }
        }
        return null;
    }

    public void insert(Obj o) {
        locals.add(o);
        if (o.kind == Obj.Kind.Var) {
            nVars++;
        }
    }

    public Scope outer() {
        return outer;
    }

    public LinkedList<Obj> locals() {
        return locals;
    }
}
