package artmann.microjava;

import artmann.microjava.codegen.Code;
import artmann.microjava.codegen.Operand;
import artmann.microjava.codegen.Label;
import artmann.microjava.symtab.SymTab;
import artmann.microjava.symtab.Obj;
import artmann.microjava.symtab.Struct;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Stack;

public final class Parser {

    private static final int MAX_GLOBALS = 32767;
    private static final int MAX_FIELDS = 32767;
    private static final int MAX_LOCALS = 127;

    private Token t;
    private Token la;
    private Token.TokenType sym;
    public final Scanner scanner;
    public final Code code;
    public final SymTab tab;

    private int errDist = 3;

    private static final EnumSet<Token.TokenType> firstFactor = EnumSet.of(Token.TokenType.ident, Token.TokenType.number, Token.TokenType.charConst, Token.TokenType.new_, Token.TokenType.lpar);
    private static final EnumSet<Token.TokenType> assignop = EnumSet.of(Token.TokenType.plusas, Token.TokenType.minusas, Token.TokenType.timesas, Token.TokenType.slashas, Token.TokenType.remas);
    private static final EnumSet<Token.TokenType> syncDecl = EnumSet.of(Token.TokenType.final_, Token.TokenType.class_, Token.TokenType.lbrace, Token.TokenType.eof);
    private static final EnumSet<Token.TokenType> syncMethDecl = EnumSet.of(Token.TokenType.void_, Token.TokenType.eof);
    private static final EnumSet<Token.TokenType> syncStat = EnumSet.of(Token.TokenType.if_, Token.TokenType.while_, Token.TokenType.break_, Token.TokenType.return_, Token.TokenType.read, Token.TokenType.print, Token.TokenType.semicolon, Token.TokenType.else_, Token.TokenType.rbrace, Token.TokenType.eof);
    private Obj curMeth;
    private Label breakLab = null;
    private final Stack<Label> breaks = new Stack<>();


    public Parser(Scanner scanner) {
        this.scanner = scanner;
        tab = new SymTab(this);
        code = new Code(this);
        la = new Token(Token.TokenType.none, 1, 1);
    }

    public void parse() {
        scan();
        Program();
        check(Token.TokenType.eof);
    }

    private void scan() {
        t = la;
        la = scanner.next();
        sym = la.tokenType;

        errDist++;
    }

    private void check(Token.TokenType expected) {
        if (sym == expected) {
            scan();
        } else {
            error(Errors.Message.TOKEN_EXPECTED, expected);
        }
    }

    public void error(Errors.Message msg, Object... msgParams) {
        if(errDist >= 3) {
            scanner.errors.error(la.line, la.col, msg, msgParams);
        }
        errDist = 0;
    }

    private void Program(){
        check(Token.TokenType.program);
        check(Token.TokenType.ident);
        Obj prog = tab.insert(Obj.Kind.Prog, t.str, SymTab.noType);
        tab.openScope();
        for(;;){
            if(sym == Token.TokenType.final_) {
                ConstDecl();
            } else if (sym == Token.TokenType.ident) {
                VarDecl();
            } else if (sym == Token.TokenType.class_) {
                ClassDecl();
            } else if(sym == Token.TokenType.lbrace || sym == Token.TokenType.eof){
                break;
            } else {
                error(Errors.Message.INVALID_DECL);
                recoverDecl();
            }
        }
        if(tab.curScope.nVars() > MAX_GLOBALS) error(Errors.Message.TOO_MANY_GLOBALS);
        code.dataSize = tab.curScope.nVars();
        check(Token.TokenType.lbrace);
        while(sym != Token.TokenType.rbrace && sym != Token.TokenType.eof) {
            MethodDecl();
        }
        check(Token.TokenType.rbrace);
        tab.findMeth("main");
        prog.locals = tab.curScope.locals();
        tab.closeScope();
    }

    private void ConstDecl(){
        check(Token.TokenType.final_);
        Struct type = Type();
        check(Token.TokenType.ident);
        Obj con = tab.insert(Obj.Kind.Con, t.str, type);
        check(Token.TokenType.assign);
        if(sym == Token.TokenType.number) {
            if(type.kind == Struct.Kind.Int) {
                scan();
                con.val = t.val;
            } else error(Errors.Message.CONST_TYPE);
        }
        else if(sym == Token.TokenType.charConst) {
            if(type.kind == Struct.Kind.Char){
                scan();
                con.val = t.val;
            } else error(Errors.Message.CONST_TYPE);
        }
        else error(Errors.Message.CONST_DECL);
        check(Token.TokenType.semicolon);
    }

    private void VarDecl(){
        Struct type = Type();
        for(;;){
            if(sym == Token.TokenType.ident) {
                scan();
                tab.insert(Obj.Kind.Var, t.str, type);
            } else error(Errors.Message.TOKEN_EXPECTED, Token.TokenType.ident);
            if(sym == Token.TokenType.comma) scan();
            else break;
        }
        check(Token.TokenType.semicolon);
    }

    private void ClassDecl(){
        check(Token.TokenType.class_);
        check(Token.TokenType.ident);
        Obj clazz = tab.insert(Obj.Kind.Type, t.str, new Struct(Struct.Kind.Class));
        check(Token.TokenType.lbrace);
        tab.openScope();
        while(sym == Token.TokenType.ident){
            VarDecl();
        }
        if(tab.curScope.nVars() > MAX_FIELDS)
            error(Errors.Message.TOO_MANY_FIELDS);
        clazz.type.fields = tab.curScope.locals();
        tab.closeScope();
        check(Token.TokenType.rbrace);
    }

    private void MethodDecl(){

        if(sym != Token.TokenType.ident && sym != Token.TokenType.void_){
            error(Errors.Message.METH_DECL);
            recoverMethodDecl();
        }
        if(sym == Token.TokenType.eof) return;

        Struct type = SymTab.noType;
        if(sym == Token.TokenType.ident){
           type = Type();
        } else if(sym == Token.TokenType.void_){
            scan();
        }

        check(Token.TokenType.ident);
        curMeth = tab.insert(Obj.Kind.Meth, t.str, type);
        curMeth.adr = code.pc;
        check(Token.TokenType.lpar);
        tab.openScope();
        if(sym == Token.TokenType.ident){
            curMeth.nPars = FormPars();
        }
        check(Token.TokenType.rpar);
        while (sym == Token.TokenType.ident) {
            VarDecl();
        }

        if(tab.curScope.nVars() > MAX_LOCALS) error(Errors.Message.TOO_MANY_LOCALS);
        if(curMeth.name.equals("main")){
            if(curMeth.type != SymTab.noType) error(Errors.Message.MAIN_NOT_VOID);
            if(curMeth.nPars != 0) error(Errors.Message.MAIN_WITH_PARAMS);
            code.mainpc = curMeth.adr;
        }

        code.put(artmann.microjava.codegen.Code.OpCode.enter);
        code.put(curMeth.nPars);
        code.put(tab.curScope.nVars());

        Block();

        code.return_(curMeth);

        curMeth.locals = tab.curScope.locals();
        tab.closeScope();

    }

    private int FormPars(){
        int nPars = 0;
        Obj last;
        for(;;){
            Struct type = Type();
            check(Token.TokenType.ident);
            last = tab.insert(Obj.Kind.Var, t.str, type);
            nPars++;
            if(sym == Token.TokenType.comma) scan();
            else break;
        }
        if(sym == Token.TokenType.ppperiod) {
            scan();
            last.type = new Struct(last.type);
            curMeth.hasVarArg = true;
        }
        return nPars;
    }

    private Struct Type(){
        check(Token.TokenType.ident);
        Obj o = tab.find(t.str);
        Struct type;
        if(o == null || o.kind != Obj.Kind.Type){
            error(Errors.Message.NO_TYPE);
            type = SymTab.noType;
        } else {
            type = o.type;
        }
        if(sym == Token.TokenType.lbrack) {
            scan();
            check(Token.TokenType.rbrack);
            type = new Struct(type);
        }
        return type;
    }

    private void Block(){
        check(Token.TokenType.lbrace);
        while(sym != Token.TokenType.rbrace && sym != Token.TokenType.eof){
            Statement();
        }
        check(Token.TokenType.rbrace);
    }

    private void Statement(){
        Operand cond;
        switch (sym) {
            case ident:
                Operand x, y;
                x = Designator();

                if(sym == Token.TokenType.assign){
                    Assignop();
                    y = Expr();
                    if(x.kind != Operand.Kind.Local && x.kind != Operand.Kind.Static
                            && x.kind != Operand.Kind.Elem && x.kind != Operand.Kind.Fld) error(Errors.Message.NO_VAR);
                    if(y.kind == Operand.Kind.Cond || y.kind == Operand.Kind.None) error(Errors.Message.NO_VAL);
                    if(!y.type.assignableTo(x.type)) error(Errors.Message.INCOMP_TYPES);
                    code.assign(x, y);
                } else if (assignop.contains(sym)) {
                    Token.TokenType op = Assignop();
                    if(x.kind == Operand.Kind.Fld) code.put(artmann.microjava.codegen.Code.OpCode.dup);
                    else if(x.kind == Operand.Kind.Elem) code.put(artmann.microjava.codegen.Code.OpCode.dup2);
                    else if(x.kind != Operand.Kind.Local && x.kind != Operand.Kind.Static) error(Errors.Message.NO_VAR);
                    code.loadAndKeep(x);
                    y = Expr();
                    if(y.kind == Operand.Kind.Cond || y.kind == Operand.Kind.None) error(Errors.Message.NO_VAL);
                    assign(x, y, op);
                } else if (sym == Token.TokenType.lpar) {
                    ActPars(x);
                    code.call(x);
                } else if (sym == Token.TokenType.pplus) {
                    code.incDec(x, true);
                    scan();
                } else if (sym == Token.TokenType.mminus) {
                    code.incDec(x, false);
                    scan();
                } else error(Errors.Message.DESIGN_FOLLOW);
                check(Token.TokenType.semicolon);
                break;
            case if_:
                scan();
                check(Token.TokenType.lpar);
                cond = Condition();
                code.fJump(cond);
                cond.tLabel.here();
                check(Token.TokenType.rpar);
                Statement();
                if (sym == Token.TokenType.else_) {
                    Label end = new Label(code);
                    code.jump(end);
                    cond.fLabel.here();
                    scan();
                    Statement();
                    end.here();
                } else {
                    cond.fLabel.here();
                }
                break;
            case while_:
                scan();
                breaks.push(breakLab);
                breakLab = new Label(code);
                check(Token.TokenType.lpar);
                Label top = new Label(code);
                top.here();
                cond = Condition();
                code.fJump(cond);
                cond.tLabel.here();
                check(Token.TokenType.rpar);
                Statement();
                code.jump(top);
                breakLab.here();
                breakLab = breaks.pop();
                cond.fLabel.here();
                break;
            case break_:
                scan();
                if(breakLab == null) error(Errors.Message.NO_LOOP);
                else code.jump(breakLab);
                check(Token.TokenType.semicolon);
                break;
            case return_:
                scan();
                if (sym == Token.TokenType.minus || firstFactor.contains(sym)) {
                    if(curMeth.type == SymTab.noType) error(Errors.Message.RETURN_VOID);
                    x = Expr();
                    if(x.kind == Operand.Kind.Meth) code.call(x);
                    else code.load(x);
                    if(!x.type.assignableTo(curMeth.type)) error(Errors.Message.RETURN_TYPE);
                } else {
                    if(curMeth.type != SymTab.noType) error(Errors.Message.RETURN_NO_VAL);
                }
                code.put(artmann.microjava.codegen.Code.OpCode.exit);
                code.put(artmann.microjava.codegen.Code.OpCode.return_);
                check(Token.TokenType.semicolon);
                break;
            case read:
                scan();
                check(Token.TokenType.lpar);
                x = Designator();
                if(x.kind != Operand.Kind.Local && x.kind != Operand.Kind.Static
                        && x.kind != Operand.Kind.Elem && x.kind != Operand.Kind.Fld) error(Errors.Message.NO_VAR);
                if(x.type != SymTab.intType && x.type != SymTab.charType) error(Errors.Message.READ_VALUE);
                code.put(artmann.microjava.codegen.Code.OpCode.read);
                code.assign(x, new Operand(x.type));
                check(Token.TokenType.rpar);
                check(Token.TokenType.semicolon);
                break;
            case print:
                scan();
                check(Token.TokenType.lpar);
                x = Expr();
                int width = -1;
                if (sym == Token.TokenType.comma) {
                    scan();
                    check(Token.TokenType.number);
                    width = t.val;
                }
                if(x.kind == Operand.Kind.Meth) code.call(x);
                else code.load(x);
                if(x.type == SymTab.intType){
                    if(width == -1) width = digitLength(x.val);
                    code.loadConst(width);
                    code.put(artmann.microjava.codegen.Code.OpCode.print);
                } else if(x.type == SymTab.charType) {
                    if(width == -1) width = 1;
                    code.loadConst(width);
                    code.put(artmann.microjava.codegen.Code.OpCode.bprint);
                } else error(Errors.Message.PRINT_VALUE);
                check(Token.TokenType.rpar);
                check(Token.TokenType.semicolon);
                break;
            case lbrace:
                Block();
                break;
            case semicolon:
                scan();
                break;
            default:
                error(Errors.Message.INVALID_STAT);
                recoverStat();
                break;
        }
    }

    private Token.TokenType Assignop(){

        Token.TokenType op = sym;
        switch (sym) {
            case assign:
            case plusas:
            case minusas:
            case timesas:
            case slashas:
            case remas:
                scan();
                break;
            default:
                error(Errors.Message.ASSIGN_OP);
                break;
        }
        return op;
    }

    private void ActPars(Operand m){
        Operand x;
        check(Token.TokenType.lpar);
        if(m.kind != Operand.Kind.Meth) {
            error(Errors.Message.NO_METH);
            m.obj = tab.noObj;
            return;
        }
        int aPars = 0;
        int fPars = m.obj.nPars;
        if(m.obj.hasVarArg) fPars--;
        Iterator<Obj> locals = m.obj.locals.iterator();
        Obj fp;
        if(sym == Token.TokenType.minus || firstFactor.contains(sym)){
            for(;;){
                x = Expr();
                if(x.kind == Operand.Kind.Meth) code.call(x);
                else code.load(x);
                aPars++;
                if(locals.hasNext() && aPars <= fPars) {
                    fp = locals.next();
                    if(!x.type.assignableTo(fp.type)) error(Errors.Message.PARAM_TYPE);
                }
                if(sym == Token.TokenType.comma) scan();
                else break;
            }
        }
        if(aPars > fPars) error(Errors.Message.MORE_ACTUAL_PARAMS);
        if(aPars < fPars) error(Errors.Message.LESS_ACTUAL_PARAMS);
        if(sym == Token.TokenType.hash){
            if(locals.hasNext() && m.obj.hasVarArg) {
                VarArgs(locals.next().type.elemType);
            } else {
                VarArgs(SymTab.noType);
                error(Errors.Message.INVALID_VARARG_CALL);
            }
        } else if (m.obj.hasVarArg && locals.hasNext()){
            code.loadConst(0);
            code.put(artmann.microjava.codegen.Code.OpCode.newarray);
            if (locals.next().type == SymTab.charType) code.put(0); else code.put(1);
        }
        check(Token.TokenType.rpar);
    }

    private void VarArgs(Struct type){
        Operand x;
        check(Token.TokenType.hash);
        check(Token.TokenType.number);
        int size = t.val;
        code.loadConst(size);
        code.put(artmann.microjava.codegen.Code.OpCode.newarray);
        if (type == SymTab.charType) code.put(0); else code.put(1);
        int parsedVarArgs = 0;
        if(sym == Token.TokenType.minus || firstFactor.contains(sym)){
            for(;;){
                code.put(artmann.microjava.codegen.Code.OpCode.dup);
                code.loadConst(parsedVarArgs);
                x = Expr();
                if(type == SymTab.noType) type = x.type;
                if(!x.type.assignableTo(type)) error(Errors.Message.PARAM_TYPE);
                code.load(x);
                if (type == SymTab.charType) code.put(artmann.microjava.codegen.Code.OpCode.bastore); else code.put(artmann.microjava.codegen.Code.OpCode.astore);
                parsedVarArgs++;
                if(sym == Token.TokenType.comma) scan();
                else break;
            }
        }
        if(parsedVarArgs > size) error(Errors.Message.MORE_ACTUAL_VARARGS);
        if(parsedVarArgs < size) error(Errors.Message.LESS_ACTUAL_VARARGS);
    }

    private Operand Condition(){
        Operand x = new Operand(null, code);
        Operand y;
        for(;;){
            y = CondTerm();
            x.op = y.op;
            x.fLabel = y.fLabel;
            if(sym == Token.TokenType.or) {
                code.tJump(x);
                scan();
                x.fLabel.here();
            }
            else break;
        }
        return x;
    }

    private Operand CondTerm(){
        Operand x = new Operand(null, code);
        Operand y;
        for(;;){
            y = CondFact();
            if(y == null) break;
            x.op = y.op;
            if(sym == Token.TokenType.and) {
                code.fJump(x);
                scan();
            }
            else break;
        }
        return x;
    }

    private Operand CondFact(){
        Operand x, y;
        artmann.microjava.codegen.Code.CompOp op;
        x = Expr();
        if(x == null) return null;
        code.load(x);
        op = Relop();
        y = Expr();
        if(y == null) return null;
        code.load(y);
        if(!x.type.compatibleWith(y.type)) error(Errors.Message.INCOMP_TYPES);
        if(x.type.isRefType() && op != artmann.microjava.codegen.Code.CompOp.eq &&  op != artmann.microjava.codegen.Code.CompOp.ne) error(Errors.Message.EQ_CHECK);
        return new Operand(op, code);
    }

    private artmann.microjava.codegen.Code.CompOp Relop(){
        switch (sym) {
            case eql:
                scan();
                return artmann.microjava.codegen.Code.CompOp.eq;
            case neq:
                scan();
                return artmann.microjava.codegen.Code.CompOp.ne;
            case gtr:
                scan();
                return artmann.microjava.codegen.Code.CompOp.gt;
            case geq:
                scan();
                return artmann.microjava.codegen.Code.CompOp.ge;
            case lss:
                scan();
                return artmann.microjava.codegen.Code.CompOp.lt;
            case leq:
                scan();
                return artmann.microjava.codegen.Code.CompOp.le;
            default:
                error(Errors.Message.REL_OP);
                break;
        }
        return null;
    }

    private Operand Expr(){
        artmann.microjava.codegen.Code.OpCode op;
        Operand x, y;
        if(sym == Token.TokenType.minus) {
            scan();
            x = Term();
            if(x.type != SymTab.intType) error(Errors.Message.NO_INT_OP);
            if(x.kind == Operand.Kind.Con) x.val = -x.val;
            else {
                code.load(x); code.put(artmann.microjava.codegen.Code.OpCode.neg);
            }
        } else {
            x = Term();
        }
        while(sym == Token.TokenType.plus || sym == Token.TokenType.minus){
            if(x.kind == Operand.Kind.Meth) code.call(x);
            else code.load(x);
            op = Addop();
            y = Term();
            if(y == null) return x;
            if(x.type != SymTab.intType || y.type != SymTab.intType) error(Errors.Message.NO_INT_OP);
            if(y.kind == Operand.Kind.Meth) code.call(y);
            else code.load(y);
            code.put(op);
        }
        return x;
    }

    private Operand Term(){
        artmann.microjava.codegen.Code.OpCode op;
        Operand x, y;
        x = Factor();
        while(sym == Token.TokenType.times || sym == Token.TokenType.slash || sym == Token.TokenType.rem){
            if(x.kind == Operand.Kind.Meth) code.call(x);
            else code.load(x);
            op = Mulop();
            y = Factor();
            if(x.type != SymTab.intType || y.type != SymTab.intType) error(Errors.Message.NO_INT_OP);
            if(y.kind == Operand.Kind.Meth) code.call(y);
            else code.load(y);
            code.put(op);
        }
        return x;
    }

    private Operand Factor(){

        Operand x;
        switch (sym) {
            case ident:
                x = Designator();
                if (sym == Token.TokenType.lpar) {
                    if(x.kind != Operand.Kind.Meth) error(Errors.Message.NO_METH);
                    if(x.type == SymTab.noType) error(Errors.Message.INVALID_CALL);
                    ActPars(x);
                    if (x.obj == tab.ordObj || x.obj == tab.chrObj) ; //nothing
                    else if (x.obj == tab.lenObj)
                        code.put(artmann.microjava.codegen.Code.OpCode.arraylength);
                    else {
                        code.call(x);
                    }
                    x.kind = Operand.Kind.Stack;
                } else if(x.kind == Operand.Kind.Meth) x.kind = Operand.Kind.None;
                break;
            case number:
                scan();
                x = new Operand(t.val);
                break;
            case charConst:
                scan();
                x = new Operand(t.val);
                x.type = SymTab.charType;
                break;
            case new_:
                scan();
                check(Token.TokenType.ident);
                Obj obj = tab.find(t.str);
                Struct type = obj.type;
                if (sym == Token.TokenType.lbrack) {
                    scan();
                    if(obj.kind != Obj.Kind.Type) error(Errors.Message.NO_TYPE);
                    x = Expr();
                    if(x.type != SymTab.intType) error(Errors.Message.ARRAY_SIZE);
                    if(x.kind == Operand.Kind.Meth) code.call(x);
                    else code.load(x);
                    code.put(artmann.microjava.codegen.Code.OpCode.newarray);
                    if (type == SymTab.charType) code.put(0); else code.put(1);
                    type = new Struct(type);
                    check(Token.TokenType.rbrack);
                } else {
                    if (obj.kind != Obj.Kind.Type) error(Errors.Message.NO_TYPE);
                    if(type.kind != Struct.Kind.Class) error(Errors.Message.NO_CLASS_TYPE);
                    code.put(artmann.microjava.codegen.Code.OpCode.new_); code.put2(type.nrFields());
                }
                x = new Operand(type);
                break;
            case lpar:
                scan();
                x = Expr();
                check(Token.TokenType.rpar);
                break;
            default:
                error(Errors.Message.INVALID_FACT);
                x = null;
                break;
        }
        return x;
    }

    private Operand Designator(){
        check(Token.TokenType.ident);
        Operand x = new Operand(tab.find(t.str), this);
        for(;;){
            if(sym == Token.TokenType.period){
                if(x.type.kind != Struct.Kind.Class) error(Errors.Message.NO_CLASS);
                scan();
                if(x.kind == Operand.Kind.Meth) code.call(x);
                else code.load(x);
                check(Token.TokenType.ident);
                Obj obj = tab.findField(t.str, x.type);
                x.kind = Operand.Kind.Fld;
                x.type = obj.type;
                x.adr = obj.adr;
            } else if(sym == Token.TokenType.lbrack){
                if(x.kind != Operand.Kind.Local && x.kind != Operand.Kind.Static
                        && x.kind != Operand.Kind.Elem && x.kind != Operand.Kind.Fld) error(Errors.Message.NO_VAL);
                scan();
                if(x.kind == Operand.Kind.Meth) code.call(x);
                else code.load(x);
                Operand y = Expr();
                if(y.type != SymTab.intType) error(Errors.Message.ARRAY_INDEX);
                if(y.kind == Operand.Kind.Meth) code.call(y);
                else code.load(y);
                if(x.type.kind != Struct.Kind.Arr) error(Errors.Message.NO_ARRAY);
                x.kind = Operand.Kind.Elem;
                x.type = x.type.elemType;
                check(Token.TokenType.rbrack);
            } else break;
        }
        return x;
    }

    private artmann.microjava.codegen.Code.OpCode Addop(){
        if(sym == Token.TokenType.plus) { scan(); return artmann.microjava.codegen.Code.OpCode.add; }
        else if(sym == Token.TokenType.minus) { scan(); return artmann.microjava.codegen.Code.OpCode.sub; }
        else error(Errors.Message.ADD_OP);
        return null;
    }

    private artmann.microjava.codegen.Code.OpCode Mulop(){
        if(sym == Token.TokenType.times) { scan(); return artmann.microjava.codegen.Code.OpCode.mul; }
        else if(sym == Token.TokenType.slash) { scan(); return artmann.microjava.codegen.Code.OpCode.div; }
        else if(sym == Token.TokenType.rem) { scan(); return artmann.microjava.codegen.Code.OpCode.rem; }
        else error(Errors.Message.MUL_OP);
        return null;
    }

    private void assign(Operand x, Operand y, Token.TokenType assignOp){
        if(x.type != SymTab.intType || y.type != SymTab.intType) error(Errors.Message.NO_INT_OP);
        if(y.kind == Operand.Kind.Meth) code.call(y);
        else code.load(y);
        switch (assignOp) {
            case plusas:
                code.put(artmann.microjava.codegen.Code.OpCode.add);
                break;
            case minusas:
                code.put(artmann.microjava.codegen.Code.OpCode.sub);
                break;
            case timesas:
                code.put(artmann.microjava.codegen.Code.OpCode.mul);
                break;
            case slashas:
                code.put(artmann.microjava.codegen.Code.OpCode.div);
                break;
            case remas:
                code.put(artmann.microjava.codegen.Code.OpCode.rem);
                break;
        }
        code.assign(x, y);
    }

    //recover functions
    private void recoverDecl(){
        do {
            scan();
        } while (!syncDecl.contains(sym) && symIsNotType());
        errDist = 0;
    }

    private void recoverMethodDecl(){
        do {
            scan();
        } while (!syncMethDecl.contains(sym) && symIsNotType());
        errDist = 0;
    }

    private void recoverStat(){
        while(!syncStat.contains(sym)){
            scan();
        }
        errDist = 0;
    }

    private boolean symIsNotType(){
        if(sym == Token.TokenType.ident) {
            Obj o = tab.find(la.str);
            return o == null || o.kind != Obj.Kind.Type;
        }
        return true;
    }

    private int digitLength(int in){
        int len = 1;
        while (in > 9){
            in /= 10;
            len++;
        }
        return len;
    }

}
