package artmann.microjava.symtab;

import java.util.*;

public class Obj {

    public enum Kind {
        Con, Var, Type, Meth, Prog
    }

    public final Kind kind;
    public final String name;
    public Struct type;
    public int val;
    public int adr;
    public int level;
    public int nPars;
    public boolean hasVarArg;
    public LinkedList<Obj> locals = new LinkedList<>();

    public Obj(Kind kind, String name, Struct type) {
        this.kind = kind;
        this.name = name;
        this.type = type;
    }
}
