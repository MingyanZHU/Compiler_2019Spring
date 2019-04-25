package symbols;

public class SymbolItem {
    // 设计成了immutable类型的数据, 后期在语法分析中对于符号表中任意一个条目均需重写, 可能有些繁琐
    // TODO 存储存在冗余 可以进一步改进
    private final String identifier;    // 标识符名字
    private final String type; // 标识符类型
    private final int position; // 标识符在源文件中的位置(语法分析中错误处理使用)
    private final int offset;   // 地址偏移

    public SymbolItem(String identifier, String type, int position, int offset) {
        this.identifier = identifier;
        this.type = type;
        this.position = position;
        this.offset = offset;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getType() {
        return type;
    }

    public int getPosition() {
        return position;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "<" + identifier + ", " + type + ", " + position + ", " + offset + ">";
    }
}
