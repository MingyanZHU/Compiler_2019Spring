package parser;

import java.util.*;

public class Item {
    private final String left;
    private final String[] right;
    private final int status;
    private final String search;

    public Item(String left, List<String> right, int status, String search) {
        this.left = left;
        String[] temp = new String[right.size()];
        temp = right.toArray(temp);
        this.right = temp;
        this.status = status;
        this.search = search;
    }

    public Item(String left, String[] right, int status, String search) {
        this.left = left;
        this.right = Arrays.copyOf(right, right.length);
        this.status = status;
        this.search = search;
    }

    public String getLeft() {
        return left;
    }

    public String[] getRight() {
        return Arrays.copyOf(right, right.length);
    }

    public int getStatus() {
        return status;
    }

    public String getSearch() {
        return search;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        if (this.getRight().length == 1 && item.getRight().length == 1 &&
                this.getRight()[0].equals(Parser.EMPTY_STRING_CHARACTER) && item.getRight()[0].equals(Parser.EMPTY_STRING_CHARACTER))
            return Objects.equals(left, item.left) &&
                    Objects.equals(search, item.search);

        return status == item.status &&
                Objects.equals(left, item.left) &&
                Arrays.equals(right, item.right) &&
                Objects.equals(search, item.search);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, Arrays.hashCode(right), status, search);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(left);
        stringBuilder.append(" -> ");
        for (int i = 0; i < right.length; i++) {
            if (i == status)
                stringBuilder.append("· ");
            stringBuilder.append(right[i]);
            if (i != right.length - 1)
                stringBuilder.append(" ");
        }
        if (status == right.length)
            stringBuilder.append(" ·");
        stringBuilder.append(", ").append(search).append("]");
        return stringBuilder.toString();
    }
}
