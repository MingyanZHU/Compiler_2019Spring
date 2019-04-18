package parser;

import java.io.*;
import java.util.*;

public class Utils {
    public static final String START_STATE = "Start";
    public static final String STACK_BOTTOM_CHARACTER = "#";
    public static final String EMPTY_STRING_CHARACTER = "ε";
//    public static final Item START_ITEM = new Item()

    private Map<String, Set<String>> first = new HashMap<>();
    private final Map<String, Set<String>> follow = new HashMap<>();
    //    private final Map<String, List<String>> production = new HashMap<>();
    private final Set<Production> productions = new HashSet<>();
    private final Set<String> nonTerminators = new HashSet<>();
    private final Set<String> terminators = new HashSet<>();
    private final Set<String> canLeadNullSet = new HashSet<>();
    private String grammarPath;
    private BufferedReader bufferedReader;

    private Map<ItemSet, Integer> itemSets = new HashMap<>();
    private String[][] lrTable;

    public Utils(String grammarPath) {
        this.grammarPath = grammarPath;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(grammarPath)));
            String string = "";
            while ((string = bufferedReader.readLine()) != null) {
                if (string.length() == 0)
                    continue;
                String[] ss = string.split("->");
                String left = ss[0].trim();
                String[] productionList = ss[1].split("\\|");
//                production.put(ss[0].trim(), productionList);
                for (String production : productionList) {
                    production = production.charAt(0) == ' ' ? production.substring(1) : production;
                    productions.add(new Production(left, production.split(" ")));
                }
                nonTerminators.add(left);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Production production : productions) {
            List<String> right = production.getRight();
            for (String s : right) {
                if (!nonTerminators.contains(s) && !s.equals(EMPTY_STRING_CHARACTER)) {
                    terminators.add(s);
                }
            }
        }
        terminators.add(STACK_BOTTOM_CHARACTER);   // 栈底符号
//        nonTerminators.addAll(production.keySet());
        initFirst();
        getFirstNonTerminatorStep(1);
        // TODO 认为输入的文法已经是拓广文法 且Start为文法的开始符号
    }

    private void initFirst() {
        for (String key : nonTerminators) {
            first.put(key, new HashSet<>());
        }
    }

    private void getFirstNonTerminatorStep(int step) {
        while (true) {
            boolean unChanged = true;
            Map<String, Set<String>> tempFirst = deepCopyMap(first);
            for (String key : nonTerminators) {
                for (Production production : productions) {
                    if (production.getLeft().equals(key)) {
                        String rightFirstItem = "";
                        if (step == 1)
                            rightFirstItem = production.getRight().get(0);
                        else {
                            rightFirstItem = "";
                            for (String right : production.getRight()) {
                                if (!canLeadNull(right)) {
                                    rightFirstItem = right;
                                    break;
                                }
                            }
                        }
                        if (nonTerminators.contains(rightFirstItem))
                            tempFirst.get(key).addAll(first.get(rightFirstItem));
                        else
                            tempFirst.get(key).add(rightFirstItem);
                    }
                }
//                for (String right : production.get(key)) {
//                    right = right.length() == 1 ? right : right.charAt(0) == ' ' ? right.substring(1) : right;
//                    String firstItem = right.split(" ")[0];
//                    if (nonTerminators.contains(firstItem))
//                        tempFirst.get(key).addAll(first.get(firstItem));
//                    else
//                        tempFirst.get(key).add(firstItem);
//                }
            }
            for (String key : nonTerminators) {
                boolean t1 = first.get(key).containsAll(tempFirst.get(key));
                boolean t2 = tempFirst.get(key).containsAll(first.get(key));
                if (!t1 || !t2) {
                    unChanged = false;
                    break;
                }
            }
            if (unChanged)
                break;
            else
                first = deepCopyMap(tempFirst);
        }
    }

    public Map<String, Set<String>> getFirst() {
        for (String key : nonTerminators) {
            if (first.get(key).contains(EMPTY_STRING_CHARACTER))
                canLeadNullSet.add(key);
        }
        getFirstNonTerminatorStep(2);
        return deepCopyMap(first);
    }

    private static Map<String, Set<String>> deepCopyMap(Map<String, Set<String>> origin) {
        Map<String, Set<String>> answer = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : origin.entrySet()) {
            answer.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return answer;
    }

    private boolean isTerminator(String test) {
        return !nonTerminators.contains(test);
    }

    private boolean isNonTerminator(String test) {
        return nonTerminators.contains(test);
    }

    public boolean canLeadNull(String rightString) {
        if (!nonTerminators.contains(rightString))
            return false;
        else {
            if (canLeadNullSet.contains(rightString))
                return true;
            for (Production production : productions) {
                if (production.getLeft().equals(rightString)) {
                    if (production.getRight().get(0).equals(EMPTY_STRING_CHARACTER)) {
                        canLeadNullSet.add(rightString);
                        return true;
                    } else {
                        boolean flag = true;
                        List<String> rights = production.getRight();
                        for (String right : rights) {
                            if (right.equals(production.getLeft()))
                                continue;
                            if (!canLeadNull(right)) {
                                flag = false;
                                break;
                                // 当前产生式不能推出空
                            }
                        }
                        if (flag) {
                            canLeadNullSet.add(production.getLeft());
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private Set<String> getFirstFromString(List<String> list) {
        Set<String> ans = new HashSet<>();
//        for(int i = 0;i<list.size();i++){
        String s = list.get(0);
        if (nonTerminators.contains(s) && !canLeadNullSet.contains(s)) {
            ans.addAll(first.get(s));
//                break;
        } else if (nonTerminators.contains(s) && canLeadNullSet.contains(s)) {
            for (String string : first.get(s)) {
                if (!string.equals(EMPTY_STRING_CHARACTER)) {
                    ans.add(string);
                }
            }
            ans.addAll(getFirstFromString(list.subList(1, list.size())));
//                break;
        } else {
            ans.add(s);
//                break;
//        }
//        for (String s : list) {
//            if (nonTerminators.contains(s) && !canLeadNullSet.contains(s)) {
//                ans.addAll(first.get(s));
//                break;
//            } else if (nonTerminators.contains(s) && canLeadNullSet.contains(s)){
//                for(String string:first.get(s)){
//                    if(!string.equals(EMPTY_STRING_CHARACTER)){
//                        ans.add(string);
//                    }
//                }
//                ans.addAll(getFirstFromString(first.))
//                break;
//            }
        }
        return ans;
    }

    public Set<Item> getClosure(Set<Item> startItem) {
        Set<Item> items = new HashSet<>(startItem);
        Set<Item> tempItems = deepCopySetItem(items);
        while (true) {
            for (Item item : items) {
                String[] right = item.getRight();
                int status = item.getStatus();
                String B = status < right.length ? right[status] : "";
                if (nonTerminators.contains(B)) {
                    // beta is not empty string
                    List<String> betaA = new ArrayList<>(Arrays.asList(right).subList(status + 1, right.length));
                    betaA.add(item.getSearch());
                    Set<String> firstBetaA = getFirstFromString(betaA);
                    for (Production production : productions) {
                        if (production.getLeft().equals(B)) {
                            for (String b : firstBetaA) {
                                tempItems.add(new Item(B, production.getRight(), 0, b));
                            }
                        }
                    }
                }
            }
            if (tempItems.size() == items.size())
                break;
            else
                items = deepCopySetItem(tempItems);
        }
        return items;
    }

    private Set<Item> deepCopySetItem(Set<Item> origin) {
        Set<Item> ans = new HashSet<>();
        for (Item item : origin) {
            ans.add(new Item(item.getLeft(), item.getRight(), item.getStatus(), item.getSearch()));
        }
        return ans;
    }

    public Set<Item> goTo(Set<Item> items, String x) {
        Set<Item> ans = new HashSet<>();
        for (Item item : items) {
            if (item.getStatus() == item.getRight().length)
                continue;
            if (item.getRight()[item.getStatus()].equals(x)) {
                ans.add(new Item(item.getLeft(), item.getRight(), item.getStatus() + 1, item.getSearch()));
            }
        }
        return getClosure(ans);
    }

    public void items(Item startItem) {
        Set<Item> startItemSet = getClosure(new HashSet<>(Collections.singletonList(startItem)));
        int index = 0;  // 项目集编号从0开始
        ItemSet startClosure = new ItemSet(startItemSet, index++);
        itemSets.put(startClosure, index);

        Set<ItemSet> tempItemSet = new HashSet<>();
        tempItemSet.add(startClosure);

        Set<String> characters = new HashSet<>(nonTerminators);
        characters.addAll(terminators);

        while (true) {
            for (ItemSet itemSet : itemSets.keySet()) {
                for (String s : characters) {
                    Set<Item> gotoSetItem = goTo(itemSet.getItemSet(), s);
                    if (gotoSetItem.size() > 0) {
                        ItemSet temp = new ItemSet(gotoSetItem, index);
                        if (itemSets.keySet().contains(temp)) {
//                            index--;
                            itemSet.addGOTO(s, itemSets.get(temp));
                        } else {
                            tempItemSet.add(temp);
                            itemSet.addGOTO(s, index);
                            index = index + 1;
                        }
//                        !itemSets.contains(new ItemSet(gotoSetItem, index))
//                        tempItemSet.add(new ItemSet(gotoSetItem, index++));
                    }
                }
            }
            if (tempItemSet.size() == itemSets.size())
                break;
            else {
                itemSets = new HashMap<>();
                for (ItemSet itemSet : tempItemSet) {
                    int i;
                    i = itemSet.getIndex();
                    System.err.println(i);
                    itemSets.put(itemSet, i);
                }
            }
        }
        List<Integer> list = new ArrayList<>(itemSets.values());
//        for(ItemSet itemSet : itemSets.keySet()){
//            list.add(itemSet.getIndex());
//        }
        Collections.sort(list);
        for (int i = 0; i < itemSets.size(); i++) {
            if (i != list.get(i)) {
                System.err.println(i);
                break;
            }
        }

        fillLrTable();
    }

    private void fillLrTable() {
        List<String> characters = new ArrayList<>(terminators);
        characters.addAll(nonTerminators);
        characters.remove(START_STATE);

        // 初始化LR Table
        lrTable = new String[itemSets.size() + 3][characters.size() + 1];
        for (String[] row : lrTable)
            Arrays.fill(row, "");

        for (int i = 0; i < characters.size(); i++)
            lrTable[0][i + 1] = characters.get(i);

        for (int i = 1; i < lrTable.length; i++)
            lrTable[i][0] = "" + (i - 1);
        lrTable[0][0] = " ";

        Item startItem = new Item(START_STATE, new String[]{"P"}, 1, STACK_BOTTOM_CHARACTER);

        for (ItemSet itemSet : itemSets.keySet()) {
            Set<Item> items = itemSet.getItemSet();
            Map<String, Integer> gotoTable = itemSet.getGotoTable();
            int index = itemSet.getIndex() + 1;
            for (Item item : items) {
                if (item.equals(startItem)) {
                    lrTable[index][characters.indexOf(STACK_BOTTOM_CHARACTER) + 1] = "acc";
                    continue;
                }
                // TODO 215 怀疑是文法问题
                if (!item.getLeft().equals(START_STATE) &&
                        (item.getStatus() == item.getRight().length) || (item.getRight().length == 1 && item.getRight()[0].equals(EMPTY_STRING_CHARACTER))) {
                    lrTable[index][characters.indexOf(item.getSearch()) + 1] = "r(" + new Production(item.getLeft(), item.getRight()).toString() + ")";
                    continue;
                }
                for (Map.Entry<String, Integer> entry : gotoTable.entrySet()) {
                    if (terminators.contains(entry.getKey())) {
                        lrTable[index][characters.indexOf(entry.getKey()) + 1] = "s" + entry.getValue();
                    } else {
                        lrTable[index][characters.indexOf(entry.getKey()) + 1] = "" + entry.getValue();
                    }
                }
            }
//            if (itemSet.getItemSet().contains(startItem)) {
//                lrTable[itemSet.getIndex()][characters.indexOf(STACK_BOTTOM_CHARACTER)] = "acc";
//                continue;
//            }
//            for (Map.Entry<String, Integer> entry : itemSet.getGotoTable().entrySet()) {
//                if (terminators.contains(entry.getKey())) {
//                    lrTable[itemSet.getIndex()][characters.indexOf(entry.getKey())] = "S" + entry.getValue();
//                } else {
//                    lrTable[itemSet.getIndex()][characters.indexOf(entry.getKey())] = "" + entry.getValue();
//                }
//            }
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        String path = "src/parser/testParserFirst.txt";
        Utils utils = new Utils(path);
        for (String non : utils.nonTerminators) {
            System.out.println(non);
        }
        System.out.println("///////////////");
        Map<String, Set<String>> ans = utils.getFirst();
        for (String ter : utils.terminators) {
            System.out.println(ter);
        }
        System.out.println("///////////////");
        for (String key : ans.keySet()) {
            System.out.println(key);
            for (String s : ans.get(key)) {
                if (s.length() == 0)
                    continue;
                System.out.print(s + "\t");
            }
            System.out.println();
        }
//        Item startItem = new Item("C", new String[]{"c", "C"}, 1, "c");
//        Item item2 = new Item("C", new String[] {"c", "C"}, 1, "d");
//        Set<Item> items = utils.goTo(utils.getClosure(new HashSet<>(Arrays.asList(startItem, item2))), "C");
//        Set<Item> items = utils.getClosure(new HashSet<>(Collections.singletonList(new Item("Start", new String[]{"S"}, 0, "#"))));
//        for (Item item : items) {
//            System.out.println(item);
//        }

        utils.items(new Item(Utils.START_STATE, new String[]{"P"}, 0, Utils.STACK_BOTTOM_CHARACTER));
        System.out.println(utils.itemSets.size());
        List<Integer> index = new ArrayList<>(utils.itemSets.values());
        Collections.sort(index);
        for(int i = 0;i<utils.itemSets.size();i++){
            if(i != index.get(i)){
                System.err.println(i);
                break;
            }
        }
//        StringBuilder format = new StringBuilder();
//        for(int i = 0;i<utils.lrTable[0].length;i++){
//            format.append()
//        }

//        PrintStream origin = System.out;
//        PrintStream printStream = new PrintStream(new FileOutputStream("src/parser/LRTable.txt"));
//        System.setOut(printStream);
//        for (int i = 0; i < utils.lrTable.length; i++) {
//            String[] row = utils.lrTable[i];
//            for (String s : row) System.out.format("%-25s", s);
//            System.out.println();
//        }
//        System.setOut(origin);
    }
}
