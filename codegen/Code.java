package artmann.microjava.codegen;

import artmann.microjava.Errors;
import artmann.microjava.Parser;
import artmann.microjava.symtab.Obj;
import artmann.microjava.symtab.SymTab;

import java.io.*;
import java.util.Arrays;

public class Code {
    private enum Operands {
        B(1), // byte ( 8 bit signed)
        S(2), // short (16 bit signed)
        W(4); // word (32 bit signed)

        public final int size;

        Operands(int size) {
            this.size = size;
        }
    }

    private static final Operands[] B = new Operands[]{Operands.B};
    private static final Operands[] S = new Operands[]{Operands.S};
    private static final Operands[] W = new Operands[]{Operands.W};
    private static final Operands[] BB = new Operands[]{Operands.B, Operands.B};

    public enum OpCode {
        load(B), //
        load_0, //
        load_1, //
        load_2, //
        load_3, //
        store(B), //
        store_0, //
        store_1, //
        store_2, //
        store_3, //
        getstatic(S), //
        putstatic(S), //
        getfield(S), //
        putfield(S), //
        const_0, //
        const_1, //
        const_2, //
        const_3, //
        const_4, //
        const_5, //
        const_m1, //
        const_(W), //
        add, //
        sub, //
        mul, //
        div, //
        rem, //
        neg, //
        shl, //
        shr, //
        inc(BB), //
        new_(S), //
        newarray(B), //
        aload, //
        astore, //
        baload, //
        bastore, //
        arraylength, //
        pop, //
        dup, //
        dup2, //
        jmp(S), //
        jeq(S), //
        jne(S), //
        jlt(S), //
        jle(S), //
        jgt(S), //
        jge(S), //
        call(S), //
        return_, //
        enter(BB), //
        exit, //
        read, //
        print, //
        bread, //
        bprint, //
        trap(B), //
        nop;

        private final Operands[] ops;

        OpCode(Operands... operands) {
            this.ops = operands;
        }

        public int code() {
            return ordinal() + 1;
        }

        public String cleanName() {
            String name = name();
            if (name.endsWith("_")) {
                name = name.substring(0, name.length() - 1);
            }
            return name;
        }

        public static OpCode get(int code) {
            if (code < 1 || code > values().length) {
                return null;
            }
            return values()[code - 1];
        }
    }

    public enum CompOp {
        eq, ne, lt, le, gt, ge;

        public static CompOp invert(CompOp op) {
            switch (op) {
                case eq:
                    return ne;
                case ne:
                    return eq;
                case lt:
                    return ge;
                case le:
                    return gt;
                case gt:
                    return le;
                case ge:
                    return lt;
            }
            throw new IllegalArgumentException("Unexpected compare operator");
        }
    }

    public String[] buf;

    public int pc;

    public int mainpc;

    public int dataSize;

    protected Parser parser;

    public Code(Parser p) {
        parser = p;
        buf = new String[100];
        pc = 0;
        mainpc = -1;
        dataSize = 0;
    }

    public void put(OpCode code) {
        if (pc == buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[pc++] = "\n" + code.cleanName();
    }

    public void put(int x) {
        if (pc == buf.length) {
            buf = Arrays.copyOf(buf, buf.length * 2);
        }
        buf[pc++] = " " + x;
    }

    public void put2(int x) {
        put(x >> 8);
        put(x);
    }

    public void put4(int x) {
        put2(x >> 16);
        put2(x);
    }

    public void put2(int pos, int x) {
        int oldpc = pc;
        pc = pos;
        put2(x);
        pc = oldpc;
    }

    public void write(BufferedWriter os) throws IOException {
        int codeSize = pc;

        os.write("MJ\n");
        os.write(codeSize + "\n");
        os.write(dataSize + "\n");
        os.write(mainpc + "\n");

        for(String s : buf){
            if (s != null) os.write(s);
        }
        os.flush();
        os.close();
    }

    public void load(Operand x) {
        loadAndKeep(x);
        x.kind = Operand.Kind.Stack;
    }

    public void loadAndKeep(Operand x) {
        switch (x.kind){
            case Con:
                if(0 <= x.val && x.val <= 5) put(OpCode.get(OpCode.const_0.code() + x.val));
                else if (x.val == -1) put(OpCode.const_m1);
                else { put(OpCode.const_); put4(x.val); }
                break;
            case Static:
                put(OpCode.getstatic); put2(x.adr); break;
            case Local:
                if (0 <= x.adr && x.adr <= 3) put(OpCode.get(OpCode.load_0.code() + x.adr));
                else { put(OpCode.load); put(x.adr); }
                break;
            case Fld:
                put(OpCode.getfield); put2(x.adr); break;
            case Elem:
                if (x.type == SymTab.charType) put(OpCode.baload); else put(OpCode.aload);
                break;
            case Stack: break;
            default: parser.error(Errors.Message.NO_VAL);
        }
    }

    public void assign(Operand x, Operand y){
        if(y.kind == Operand.Kind.Meth) call(y);
        else load(y);
        switch (x.kind) {
            case Local:
                if (0 <= x.adr && x.adr <= 3) put(OpCode.get(OpCode.store_0.code() + x.adr));
                else { put(OpCode.store); put(x.adr); }
                break;
            case Static: put(OpCode.putstatic); put2(x.adr); break;
            case Fld: put(OpCode.putfield); put2(x.adr); break;
            case Elem:
                if (x.type == SymTab.charType) { put(OpCode.bastore); }
                else { put(OpCode.astore); }
                break;
            default: parser.error(Errors.Message.NO_VAR);
        }
    }

    public void incDec(Operand x, boolean inc){
        if(x.type != SymTab.intType) parser.error(Errors.Message.NO_INT);
        if(x.kind == Operand.Kind.Local) {
            put(Code.OpCode.inc);
            put(x.adr);
            if (inc) put(1);
            else put(255);
        } else if (x.kind == Operand.Kind.Static || x.kind == Operand.Kind.Fld || x.kind == Operand.Kind.Elem){
            dup(x);
            loadAndKeep(x);
            if (inc) put(OpCode.const_1);
            else put(OpCode.const_m1);
            put(Code.OpCode.add);
            assign(x, new Operand(SymTab.intType));
        } else parser.error(Errors.Message.NO_VAR);
    }

    public void call(Operand meth){
        if (meth.kind != Operand.Kind.Meth) parser.error(Errors.Message.NO_METH);
        put(Code.OpCode.call);
        put2(meth.adr - (pc - 1));
    }

    public void return_(Obj meth){
        if(meth.type == SymTab.noType){
            put(Code.OpCode.exit);
            put(Code.OpCode.return_);
        } else {
            put(Code.OpCode.trap); put(1);
        }
    }

    public void loadConst(int val){
        load(new Operand(val));
    }

    public void jump(Label lab){
        put(OpCode.jmp);
        lab.put();
    }

    public void tJump(Operand x){
        put(OpCode.get(OpCode.jeq.code() + (x.op.ordinal())));
        x.tLabel.put();
    }

    public void fJump(Operand x){
        if(x.op == null) return;
        put(OpCode.get(OpCode.jeq.code() + (CompOp.invert(x.op).ordinal())));
        x.fLabel.put();
    }

    private void dup(Operand x){
        if (x.kind == Operand.Kind.Fld){
            put(Code.OpCode.dup);
        } else if (x.kind == Operand.Kind.Elem){
            put(Code.OpCode.dup2);
        } else if (x.kind != Operand.Kind.Static){
            parser.error(Errors.Message.NO_VAR);
        }
    }

}
