package lexer;

public class Token {
    // 用于处理关键字和各种运算符
    private final Tag tag;

    public Token(Tag tag) {
        this.tag = tag;
    }


    public Tag getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "<" + tag + ">";
    }
}
