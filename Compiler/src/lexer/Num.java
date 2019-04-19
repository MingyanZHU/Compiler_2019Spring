package lexer;

public class Num extends Token {
    // 用于处理整数
    private final int value;

    public Num(Tag tag, int value, int line) {
        super(tag, line);
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "<" + this.getTag() + ", " + this.value + ">";
    }
}
