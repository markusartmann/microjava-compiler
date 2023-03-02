package artmann.microjava;

public class Token {
    public enum TokenType {

        and("&&"),
        assign("="),
        break_("break"),
        charConst("character constant"),
        class_("class"),
        comma(","),
        else_("else"),
        eof("end of file"),
        eql("=="),
        final_("final"),
        geq(">="),
        gtr(">"),
        hash("hash"),
        ident("identifier"),
        if_("if"),
        lbrace("{"),
        lbrack("["),
        leq("<="),
        lpar("("),
        lss("<"),
        minus("-"),
        minusas("-="),
        mminus("--"),
        neq("!="),
        new_("new"),
        none("none"),
        number("number"),
        or("||"),
        period("."),
        plus("+"),
        plusas("+="),
        pperiod(".."),
        pplus("++"),
        ppperiod("..."),
        print("print"),
        program("program"),
        rbrace("}"),
        rbrack("]"),
        read("read"),
        rem("%"),
        remas("%="),
        return_("return"),
        rpar(")"),
        semicolon(";"),
        slash("/"),
        slashas("/="),
        times("*"),
        timesas("*="),
        void_("void"),
        while_("while");

        private String label;

        TokenType(String label) {
            this.label = label;
        }
    }

    public TokenType tokenType;

    public final int line;

    public final int col;

    public int val;

    public String str;

    public Token(TokenType tokenType, int line, int col) {
        this.tokenType = tokenType;
        this.line = line;
        this.col = col;
    }

}
