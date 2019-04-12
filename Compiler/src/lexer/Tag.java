package lexer;

public enum Tag {
    INT("int"), FLOAT("float"), BOOL("bool"), CHAR("char"), STRUCT("struct"), IF("if"),
    ELSE("else"), DO("do"), WHILE("while"), BREAK("break"), CONTINUE("continue"), TRUE("true"), FALSE("false"), // keyword
    ADD("+"), SUB("-"), MUL("*"), DIV("/"), MOD("%"), // arithmetic op
    NE("!="), G(">"), GE(">="), L("<"), LE("<="), EQ("=="), // logical op
    SLP("("), SRP(")"), LP("{"), RP("}"), MLP("["), MRP("]"), ASSIGN("="), SEMICOLON(";"), // delimiters
    REAL("REAL"), // float number
    NUM("NUM"), // integer number
    ID("ID"),  // identifier
    STRING("STRING"),
    NULL("null");

    private String value;

    Tag(String tag) {
        this.value = tag;
    }

    public String getValue() {
        return this.value;
    }

    public static Tag fromString(String tag) {
        for (Tag t : Tag.values()) {
            if (t.value.equals(tag))
                return t;
        }
        throw new IllegalArgumentException("No constant with text " + tag + " found");
    }
}
