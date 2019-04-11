package lexer;

public enum Tag {
    INT("int"), FLOAT("float"), BOOL("bool"), STRUCT("struct"), IF("if"),
    ELSE("else"), DO("do"), WHILE("while"), // keyword
    PLUSOP("+"), MINUSOP("-"), MULOP("*"), DEVOP("/"), MODOP("%"), // arithmetic op
    NE("!="), G(">"), GE(">="), L("<"), LE("<="), // logical op
    SLP("("), SRP(")"), LP("{"), RP("}"), MLP("["), MRP("]"), EQUAL("="),// delimiters
    REAL("REAL"), // number
    ID("ID"),  // identifier
    NULL("null");

    private String value;
    Tag(String tag){
        this.value = tag;
    }

    public String getValue(){
        return this.value;
    }

    public static Tag fromString(String tag){
        for(Tag t : Tag.values()){
            if (t.value.equals(tag))
                return t;
        }
        throw new IllegalArgumentException("No constant with text " + tag + " found");
    }
}
