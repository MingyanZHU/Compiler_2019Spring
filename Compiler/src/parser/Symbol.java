package parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Symbol {
    private final String name;
    private final Map<String, String> attribute = new HashMap<>();

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
