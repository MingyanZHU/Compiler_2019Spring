package parser;

import java.util.*;

public class ItemSet {
    private Set<Item> itemSet;
    private int index;
    private Map<String, Integer> gotoTable = new HashMap<>();

    public ItemSet(Set<Item> items, int index) {
        this.itemSet = items;
        this.index = index;
    }

    public ItemSet(Set<Item> itemSet, int index, Map<String, Integer> gotoTable) {
        this.itemSet = itemSet;
        this.index = index;
        this.gotoTable = gotoTable;
    }

    public void addGOTO(String search, int itemSetIndex) {
        gotoTable.put(search, itemSetIndex);
    }

    public Set<Item> getItemSet() {
        return itemSet;
    }

    public int getIndex() {
        return index;
    }

    public Map<String, Integer> getGotoTable() {
        return gotoTable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemSet itemSet1 = (ItemSet) o;
        if (itemSet1.getItemSet().size() != itemSet.size())
            return false;
        return itemSet.containsAll(itemSet1.itemSet);
    }

    @Override
    public int hashCode() {
        Item[] items = new Item[itemSet.size()];
        new ArrayList<>(itemSet).toArray(items);
        return Arrays.hashCode(items);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("I").append(index).append("\n");
        for (Item item : itemSet) {
            stringBuilder.append(item).append("\n");
        }
        if (gotoTable.size() > 0) {
            stringBuilder.append("GOTO\n");
            for (Map.Entry<String, Integer> integerEntry : gotoTable.entrySet()) {
                stringBuilder.append(integerEntry.getKey()).append("->").append(integerEntry.getValue()).append("\n");
            }
        }
        return stringBuilder.toString();
    }
}
