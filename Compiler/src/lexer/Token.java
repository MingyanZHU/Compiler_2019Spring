package lexer;

public class Token {
    private final String origin;
    private final String value;
    private final Tag key;

    public Token(String origin, Tag key, String value) {
        this.origin = origin;
        this.value = value;
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public Tag getKey() {
        return key;
    }

    @Override
    public String toString() {
        return origin + "\t<" + key + ", " + value + ">";
    }
}
