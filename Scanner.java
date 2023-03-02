package artmann.microjava;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

public final class Scanner {

    private static final char EOF = (char) -1;
    private static final char LF = '\n';
    private Reader in;
    private char ch;
    private int line;
    private int col;

    public final Errors errors;

    private final HashMap<String, Token.TokenType> keywordMap = new HashMap<>();

    public Scanner(Reader r) {
        in = r;
        errors = new Errors();
        keywordMap.put("break", Token.TokenType.break_);
        keywordMap.put("class", Token.TokenType.class_);
        keywordMap.put("else", Token.TokenType.else_);
        keywordMap.put("final", Token.TokenType.final_);
        keywordMap.put("if", Token.TokenType.if_);
        keywordMap.put("new", Token.TokenType.new_);
        keywordMap.put("print", Token.TokenType.print);
        keywordMap.put("program", Token.TokenType.program);
        keywordMap.put("read", Token.TokenType.read);
        keywordMap.put("return", Token.TokenType.return_);
        keywordMap.put("void", Token.TokenType.void_);
        keywordMap.put("while", Token.TokenType.while_);
        line = 1; col = 0;
        nextCh();
    }

    public Token next() {

        while (Character.isWhitespace(ch)) {
            nextCh();
        }

        Token t = new Token(Token.TokenType.none, line, col);

        switch (ch) {
            //ident or keyword
            case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n': case 'o':  case 'p': case 'q': case 'r': case 's': case 't': case 'u': case 'v': case 'w': case 'x': case 'y': case 'z':
            case 'A': case 'B': case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I': case 'J': case 'K': case 'L': case 'M': case 'N': case 'O':  case 'P': case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W': case 'X': case 'Y': case 'Z':
                readName(t);
                break;
            //number
            case '0': case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9':
                readNumber(t);
                break;
            //charConst
            case '\'':
                readCharConst(t);
                break;
            //simple tokens
            case ';':
                t.tokenType = Token.TokenType.semicolon; nextCh(); break;
            case ',':
                t.tokenType = Token.TokenType.comma; nextCh(); break;
            case '(':
                t.tokenType = Token.TokenType.lpar; nextCh(); break;
            case ')':
                t.tokenType = Token.TokenType.rpar; nextCh(); break;
            case '[':
                t.tokenType = Token.TokenType.lbrack; nextCh(); break;
            case ']':
                t.tokenType = Token.TokenType.rbrack; nextCh(); break;
            case '{':
                t.tokenType = Token.TokenType.lbrace; nextCh(); break;
            case '}':
                t.tokenType = Token.TokenType.rbrace; nextCh(); break;
            case '#':
                t.tokenType = Token.TokenType.hash; nextCh(); break;
            case EOF:
                t.tokenType = Token.TokenType.eof; break;
            //compound tokens
            case '=':
                nextCh();
                if (ch == '=') { t.tokenType = Token.TokenType.eql; nextCh(); }
                else { t.tokenType = Token.TokenType.assign; }
                break;
            case '/':
                nextCh();
                if (ch == '*') { skipComment(t); t = next(); }
                else if (ch == '=') { t.tokenType = Token.TokenType.slashas; nextCh(); }
                else { t.tokenType = Token.TokenType.slash; }
                break;
            case '+':
                nextCh();
                if (ch == '+') { t.tokenType = Token.TokenType.pplus; nextCh(); }
                else if (ch == '=') { t.tokenType = Token.TokenType.plusas; nextCh(); }
                else { t.tokenType = Token.TokenType.plus; }
                break;
            case '-':
                nextCh();
                if (ch == '-') { t.tokenType = Token.TokenType.mminus; nextCh(); }
                else if (ch == '=') { t.tokenType = Token.TokenType.minusas; nextCh(); }
                else { t.tokenType = Token.TokenType.minus; }
                break;
            case '*':
                nextCh();
                if (ch == '=') { t.tokenType = Token.TokenType.timesas; nextCh(); }
                else { t.tokenType = Token.TokenType.times; }
                break;
            case '%':
                nextCh();
                if (ch == '=') { t.tokenType = Token.TokenType.remas; nextCh(); }
                else { t.tokenType = Token.TokenType.rem; }
                break;
            case '!':
                nextCh();
                if (ch == '=') { t.tokenType = Token.TokenType.neq; nextCh(); }
                else { error(t, Errors.Message.INVALID_CHAR, '!'); }
                break;
            case '<':
                nextCh();
                if (ch == '=') { t.tokenType = Token.TokenType.leq; nextCh(); }
                else { t.tokenType = Token.TokenType.lss; }
                break;
            case '>':
                nextCh();
                if (ch == '=') { t.tokenType = Token.TokenType.geq; nextCh(); }
                else { t.tokenType = Token.TokenType.gtr; }
                break;
            case '&':
                nextCh();
                if (ch == '&') { t.tokenType = Token.TokenType.and; nextCh(); }
                else { error(t, Errors.Message.INVALID_CHAR, '&'); }
                break;
            case '|':
                nextCh();
                if (ch == '|') { t.tokenType = Token.TokenType.or; nextCh(); }
                else { error(t, Errors.Message.INVALID_CHAR, '|'); }
                break;
            case '.':
                nextCh();
                if (ch == '.') {
                    nextCh();
                    if (ch == '.') { t.tokenType = Token.TokenType.ppperiod; nextCh(); } else { t.tokenType = Token.TokenType.pperiod; }
                }
                else { t.tokenType = Token.TokenType.period; }
                break;
            default:
                error(t, Errors.Message.INVALID_CHAR, ch); nextCh();
                break;
        }

    	return t;

    }

    private void nextCh() {
        try {
            ch = (char) in.read(); col++;
            if(ch == LF) { line++; col = 0; }
        } catch (IOException e){
            ch = EOF;
        }
    }

    private void readName(Token t) {
        StringBuilder stringBuilder = new StringBuilder();
        do {
            stringBuilder.append(ch);
            nextCh();
        } while (Character.isAlphabetic(ch) || Character.isDigit(ch) || ch == '_');
        t.str = stringBuilder.toString();
        t.tokenType = keywordMap.getOrDefault(t.str, Token.TokenType.ident);
    }

    private void readCharConst(Token t) {
        char CR = '\r';
        t.tokenType = Token.TokenType.charConst;
        nextCh();
        if (ch == '\\') {
            nextCh();
            if(ch == 'r') {
                t.val = '\r';
            } else if (ch == 'n') {
                t.val = '\n';
            } else if (ch == '\'') {
                t.val = '\'';
            } else if (ch == '\\') {
                t.val = '\\';
            } else {
                error(t, Errors.Message.UNDEFINED_ESCAPE, ch);
            }
        } else if (ch == '\'') {
            error(t, Errors.Message.EMPTY_CHARCONST);
            nextCh();
            return;
        } else if (ch == LF || ch == CR) {
            error(t, Errors.Message.ILLEGAL_LINE_END);
            return;
        } else if (ch == EOF) {
            error(t, Errors.Message.EOF_IN_CHAR);
            return;
        } else {
            t.val = ch;
        }
        nextCh();
        if(ch != '\'') error(t, Errors.Message.MISSING_QUOTE);
        else nextCh();
    }

    private void readNumber(Token t) {
        StringBuilder numberStr = new StringBuilder();
        do {
            numberStr.append(ch);
            nextCh();
        } while (Character.isDigit(ch));
        t.tokenType = Token.TokenType.number;
        try {
            t.val = Integer.parseInt(numberStr.toString());
        } catch (NumberFormatException e){
            error(t, Errors.Message.BIG_NUM, numberStr.toString());
        }
    }

    private void skipComment(Token t) {
        int nestedLevel = 1;
    	nextCh();
    	while (ch != EOF){
            if(ch == '/') {
                nextCh();
                if(ch == '*') { nestedLevel++; nextCh(); }
            } else if(ch == '*') {
                nextCh();
                if(ch == '/') {
                    nestedLevel--; nextCh();
                    if(nestedLevel <= 0) { return; }
                }
            } else nextCh();
        }
        error(t, Errors.Message.EOF_IN_COMMENT);
    }

    private void error(Token t, Errors.Message msg, Object... msgParams) {
        errors.error(t.line, t.col, msg, msgParams);
    }
}
