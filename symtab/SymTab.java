package artmann.microjava.symtab;

import artmann.microjava.Errors;
import artmann.microjava.Parser;

public final class SymTab {

    public static final Struct noType = new Struct(Struct.Kind.None);
    public static final Struct intType = new Struct(Struct.Kind.Int);
    public static final Struct charType = new Struct(Struct.Kind.Char);
    public static final Struct nullType = new Struct(Struct.Kind.Class);

    public Obj noObj, chrObj, ordObj, lenObj;

    private final Parser parser;

    public Scope curScope = null;

    private int curLevel = -1;

    public SymTab(Parser p) {
        parser = p;
        init();
    }

    private void init(){

        openScope();
        insert(Obj.Kind.Type, "int", intType);
        insert(Obj.Kind.Type, "char", charType);
        insert(Obj.Kind.Con, "null", nullType);

        noObj = new Obj(Obj.Kind.Var, "$none", intType);

        chrObj = insert(Obj.Kind.Meth, "chr", charType);
        openScope();
        insert(Obj.Kind.Var, "i", intType);
        chrObj.locals = curScope.locals();
        closeScope();
        chrObj.nPars++;

        ordObj = insert(Obj.Kind.Meth, "ord", intType);
        openScope();
        insert(Obj.Kind.Var, "ch", charType);
        ordObj.locals = curScope.locals();
        closeScope();
        ordObj.nPars++;

        lenObj = insert(Obj.Kind.Meth, "len", intType);
        openScope();
        insert(Obj.Kind.Var, "arr", new Struct(noType));
        lenObj.locals = curScope.locals();
        closeScope();
        lenObj.nPars++;

    }

    public void openScope(){
        curScope = new Scope(curScope);
        curLevel++;
    }

    public void closeScope(){
        curScope = curScope.outer();
        curLevel--;
    }

    public Obj insert(Obj.Kind kind, String name, Struct type){

        if(name == null || name.equals("")) return noObj;

        Obj obj = new Obj(kind, name, type);
        if (kind == Obj.Kind.Var) {
            obj.adr = curScope.nVars();
            obj.level = curLevel;
        }
        if(curScope.findLocal(name) != null){
            parser.error(Errors.Message.DECL_NAME, name);
        } else {
            curScope.insert(obj);
        }
        return obj;

    }

    public Obj find(String name){
        Obj obj = curScope.findGlobal(name);
        if(obj != null) return obj;
        else {
            parser.error(Errors.Message.NOT_FOUND, name);
            return noObj;
        }
    }

    public Obj findMeth(String name){
        Obj obj = curScope.findGlobal(name);
        if(obj != null && obj.kind == Obj.Kind.Meth) return obj;
        else {
            parser.error(Errors.Message.METH_NOT_FOUND, name);
            return noObj;
        }
    }

    public Obj findField(String name, Struct type){
        Obj obj = type.findField(name);
        if(obj != null) return obj;
        else {
            parser.error(Errors.Message.NO_FIELD, name);
            return noObj;
        }
    }
}
