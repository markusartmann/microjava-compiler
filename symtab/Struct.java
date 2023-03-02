package artmann.microjava.symtab;

import java.util.Collections;
import java.util.List;

public class Struct {

    public enum Kind {
        None, Int, Char, Arr, Class
    }

    public final Kind kind;

    public final Struct elemType;

    public List<Obj> fields = Collections.emptyList();

    protected Struct(Kind kind, Struct elemType) {
        this.kind = kind;
        this.elemType = elemType;
    }

    public Struct(Kind kind) {
        this(kind, null);
    }

    public Struct(Struct elemType) {
        this(Kind.Arr, elemType);
    }

    public Obj findField(String name) {
        for(Obj field : fields) {
            if(field.name.equals(name)) {
                return field;
            }
        }
        return null;
    }

    public int nrFields() {
        return fields.size();
    }

    public boolean compatibleWith(Struct other) {
        return this.equals(other) ||
                (this == SymTab.nullType && other.isRefType()) ||
                (other == SymTab.nullType && this.isRefType());
    }

    public boolean assignableTo(Struct dest) {
        return this.equals(dest) ||
                (this == SymTab.nullType && dest.isRefType()) ||
                (this.kind == Kind.Arr && dest.kind == Kind.Arr && dest.elemType == SymTab.noType);
    }

    public boolean isRefType() {
        return kind == Kind.Class || kind == Kind.Arr;
    }

    private boolean equals(Struct other) {
        if (kind == Kind.Arr && other != null) {
            return other.kind == Kind.Arr && elemType.equals(other.elemType);
        } else {
            return this == other;
        }
    }
}
