package lexer;

public class Token {
    // 用于处理关键字和各种运算符
    private final Tag tag;
    private final int line;

    public Token(Tag tag, int line) {
        this.line = line;
        this.tag = tag;
    }


    public Tag getTag() {
        return tag;
    }

    public int getLine() {
        return line;
    }

    @Override
    public String toString() {
        return "<" + tag + ">";
    }
}
