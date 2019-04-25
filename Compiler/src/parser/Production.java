package parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Production {
    private final String left;
    private final List<String> right;
    private final List<String> syntaxAction = new ArrayList<>();

    public Production(String left, String[] right) {
        this.left = left;
        this.right = new ArrayList<>(Arrays.asList(right));
    }

    public Production(String left, List<String> right) {
        this.left = left;
        this.right = new ArrayList<>(right);
    }

    private String[] toArray() {
        String[] ans = new String[right.size()];
        ans = right.toArray(ans);
        return ans;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Production that = (Production) o;
        return left.equals(that.left) &&
                Arrays.equals(toArray(), that.toArray());
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, Arrays.hashCode(toArray()));
    }

    public String getLeft() {
        return left;
    }

    public List<String> getRight() {
        return right;
    }

    public void addSyntaxAction(String action) {
        this.syntaxAction.add(action);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(left).append(" -> ");
        for (int i = 0; i < right.size(); i++) {
            stringBuilder.append(right.get(i));
            if (i != right.size() - 1)
                stringBuilder.append(" ");
        }
        return stringBuilder.toString();
    }
}
