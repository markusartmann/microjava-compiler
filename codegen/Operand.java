package artmann.microjava.codegen;

import artmann.microjava.Errors;
import artmann.microjava.Parser;
import artmann.microjava.symtab.Obj;
import artmann.microjava.symtab.Struct;
import artmann.microjava.symtab.SymTab;


public class Operand {

    public enum Kind {
        Con, Local, Static, Stack, Fld, Elem, Meth, Cond, None
    }

    public Kind kind;
    public Struct type;
    public int val;
    public int adr;
    public Code.CompOp op;
    public Obj obj;
    public Label tLabel;
    public Label fLabel;

    public Operand(Obj o, Parser parser) {
        type = o.type;
        val = o.val;
        adr = o.adr;
        switch (o.kind) {
            case Con:
                kind = Kind.Con;
                break;
            case Var:
                if (o.level == 0) {
                    kind = Kind.Static;
                } else {
                    kind = Kind.Local;
                }
                break;
            case Meth:
                kind = Kind.Meth;
                obj = o;
                break;
            default:
                kind = Kind.None;
                parser.error(Errors.Message.NO_OPERAND);
        }
    }

    public Operand(Code.CompOp op, Code code) {
        this(code);
        this.kind = Kind.Cond;
        this.op = op;
    }

    public Operand(Code code) {
        tLabel = new Label(code);
        fLabel = new Label(code);
    }

    public Operand(Struct type) {
        this.kind = Kind.Stack;
        this.type = type;
    }

    public Operand(int x) {
        kind = Kind.Con;
        type = SymTab.intType;
        val = x;
    }

}
