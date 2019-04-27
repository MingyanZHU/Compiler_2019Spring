package parser;

import java.util.*;

public class Symbol {
    private final String name;
    private final Map<String, String> attribute = new HashMap<>();
    private List<Integer> falseList = new ArrayList<>();
    private List<Integer> trueList = new ArrayList<>();
    private List<Integer> nextList = new ArrayList<>();

    public Symbol(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addAttribute(String key, String value) {
        attribute.put(key, value);
    }

    public String getAttribute(String key) {
        return attribute.get(key);
    }

    public List<Integer> makeList(int nextInstr, int i) {
        if (i == 1)   // true List
        {
            trueList.add(nextInstr);
            return new ArrayList<>(trueList);
        } else if (i == 0) {
            // false list
            falseList.add(nextInstr);
            return new ArrayList<>(falseList);
        } else {
            // nextList
            nextList.add(nextInstr);
            return new ArrayList<>(nextList);
        }
    }

    public List<Integer> getFalseList() {
        return new ArrayList<>(falseList);
    }

    public List<Integer> getTrueList() {
        return new ArrayList<>(trueList);
    }

    public List<Integer> getNextList() {
        return new ArrayList<>(nextList);
    }

    public List<Integer> merge(List<Integer> list1, List<Integer> list2, int i) {
        if (i == 1) {
            // true list
            trueList.addAll(new HashSet<>(list1));
            trueList.addAll(new HashSet<>(list2));
            return new ArrayList<>(trueList);
        } else if (i == 0) {
            falseList.addAll(new HashSet<>(list1));
            falseList.addAll(new HashSet<>(list2));
            return new ArrayList<>(falseList);
        } else {
            nextList.addAll(new HashSet<>(list1));
            nextList.addAll(new HashSet<>(list2));
            return new ArrayList<>(nextList);
        }
    }

    public List<Integer> addList(List<Integer> list, int i) {
        if (i == 1) {
            // true List
            trueList.addAll(new HashSet<>(list));
            return new ArrayList<>(trueList);
        } else if (i == 0) {
            // false list
            falseList.addAll(new HashSet<>(list));
            return new ArrayList<>(falseList);
        } else {
            nextList.addAll(new HashSet<>(list));
            return new ArrayList<>(nextList);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol that = (Symbol) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
