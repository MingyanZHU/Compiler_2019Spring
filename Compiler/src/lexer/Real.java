package lexer;

public class Real extends Token {
    // 用于处理浮点数
    private final double value;

    public Real(Tag tag, double value, int line) {
        super(tag, line);
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "<" + this.getTag() + ", " + this.value + ">";
    }
}
