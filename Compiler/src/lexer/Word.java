package lexer;

public class Word extends Token {
    // 用于处理标识符和字符串
    private final String lexme;

    public Word(Tag tag, String string) {
        super(tag);
        this.lexme = string;
    }

    public String getLexme() {
        return lexme;
    }

    @Override
    public String toString() {
        return "<" + this.getTag() + ", " + lexme + ">";
    }
}
