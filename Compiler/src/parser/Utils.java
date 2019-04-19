package parser;

import lexer.Lexer;
import lexer.Tag;
import lexer.Token;

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

    private Set<ItemSet> itemSets = new HashSet<>();
    private Map<Integer, Map<String, Integer>> gotoTable = new HashMap<>();
    private String[][] lrTable;
    private List<String> lrTableHead;


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
                // 文法分割符为汉字 丨
                String[] productionList = ss[1].split("丨");
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
        getFirst();
        // TODO 认为输入的文法已经是拓广文法 且Start为文法的开始符号

        lrTableHead = new ArrayList<>(terminators);
        lrTableHead.addAll(nonTerminators);
        lrTableHead.remove(START_STATE);
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

    private void getFirst() {
        initFirst();
        getFirstNonTerminatorStep(1);
        for (String key : nonTerminators) {
            if (first.get(key).contains(EMPTY_STRING_CHARACTER))
                canLeadNullSet.add(key);
        }
        getFirstNonTerminatorStep(2);
    }

    public Map<String, Set<String>> getNonTerminatorsFirst() {
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

    private boolean canLeadNull(String rightString) {
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

    private Set<Item> getClosure(Set<Item> startItem) {
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

    private Set<Item> goTo(Set<Item> items, String x) {
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
        ItemSet startClosure = new ItemSet(startItemSet, index);
        itemSets.add(startClosure);

        Set<ItemSet> tempItemSet = new HashSet<>();
        tempItemSet.add(startClosure);

        index++;

        Set<String> characters = new HashSet<>(nonTerminators);
        characters.addAll(terminators);

        ItemSet debug = null;
        while (true) {
            for (ItemSet itemSet : itemSets) {
                for (String s : characters) {
                    Set<Item> gotoSetItem = goTo(itemSet.getItemSet(), s);
                    if (gotoSetItem.size() > 0) {
                        ItemSet temp = new ItemSet(gotoSetItem, index);
                        boolean found = false;
                        int gotoIndex = -1;
                        for (ItemSet itemSet1 : tempItemSet) {
                            if (itemSet1.equals(temp)) {
                                found = true;
                                gotoIndex = itemSet1.getIndex();
                                break;
                            }
                        }
                        if (found) {
                            itemSet.addGOTO(s, gotoIndex);
                            addGOTOTable(itemSet.getIndex(), s, gotoIndex);
                        } else {
                            tempItemSet.add(temp);
                            itemSet.addGOTO(s, index);
                            addGOTOTable(itemSet.getIndex(), s, index);
                            index = index + 1;
                        }
//                        if (itemSets.keySet().contains(temp)) {
////                            index--;
//                            itemSet.addGOTO(s, itemSets.get(temp));
//                            addGOTOTable(itemSet.getIndex(), s, itemSets.get(temp));
//                        } else {
//                            tempItemSet.put(temp, index);
//                            if(index == 163)
//                                debug = temp;
//                            itemSet.addGOTO(s, index);
//                            addGOTOTable(itemSet.getIndex(), s, index);
//                            index = index + 1;
//                        }
//                        !itemSets.contains(new ItemSet(gotoSetItem, index))
//                        tempItemSet.add(new ItemSet(gotoSetItem, index++));
                    }
                }
            }
            if (tempItemSet.size() == itemSets.size())
                break;
            else {
                itemSets = new HashSet<>();
                for (ItemSet itemSet : tempItemSet) {
                    itemSets.add(new ItemSet(itemSet.getItemSet(), itemSet.getIndex(), itemSet.getGotoTable()));
                }
            }
        }
//        List<Integer> list = new ArrayList<>(itemSets.values());
//        for(ItemSet itemSet : itemSets.keySet()){
//            list.add(itemSet.getIndex());
//        }
//        Collections.sort(list);
//        int j = 0;
//        for (int i = 0; i < itemSets.size(); i++) {
//            if (i != list.get(j)) {
//                System.err.println(i);
//                j--;
//            }
//            j++;
//        }

        fillLrTable();
    }

    private void fillLrTable() {
        List<String> characters = new ArrayList<>(terminators);
        characters.addAll(nonTerminators);
        characters.remove(START_STATE);

        // 初始化LR Table
        lrTable = new String[itemSets.size() + 1][characters.size() + 1];
        for (String[] row : lrTable)
            Arrays.fill(row, "");

        for (int i = 0; i < characters.size(); i++)
            lrTable[0][i + 1] = characters.get(i);

        for (int i = 1; i < lrTable.length; i++)
            lrTable[i][0] = "" + (i - 1);
        lrTable[0][0] = " ";

        Item startItem = new Item(START_STATE, new String[]{"P"}, 1, STACK_BOTTOM_CHARACTER);

        for (ItemSet itemSet : itemSets) {
            Set<Item> items = itemSet.getItemSet();
            Map<String, Integer> gotoTable = itemSet.getGotoTable();
            int index = itemSet.getIndex() + 1;
            for (Item item : items) {
                if (item.equals(startItem)) {
                    int j = characters.indexOf(STACK_BOTTOM_CHARACTER) + 1;
                    if (lrTable[index][j].length() != 0) {
                        System.err.println("" + index + ", " + j + " " + lrTable[index][j]);
                        lrTable[index][j] += "acc";
                    } else {
                        lrTable[index][j] = "acc";
                    }
                    continue;
                }
                if (!item.getLeft().equals(START_STATE) &&
                        (item.getStatus() == item.getRight().length) || (item.getRight().length == 1 && item.getRight()[0].equals(EMPTY_STRING_CHARACTER))) {
                    int j = characters.indexOf(item.getSearch()) + 1;
                    if (lrTable[index][j].length() != 0) {
                        System.err.println("" + index + ", " + j + " " + lrTable[index][j]);
                        lrTable[index][j] += "r(" + new Production(item.getLeft(), item.getRight()).toString() + ")";
                    } else {
                        lrTable[index][j] = "r(" + new Production(item.getLeft(), item.getRight()).toString() + ")";
                    }
                    continue;
                }
                for (Map.Entry<String, Integer> entry : gotoTable.entrySet()) {
                    int j = characters.indexOf(entry.getKey()) + 1;

                    if (terminators.contains(entry.getKey())) {
                        if (!lrTable[index][j].equals("s" + entry.getValue()) && lrTable[index][j].length() != 0) {
                            System.err.println("" + index + ", " + j + " " + lrTable[index][j]);
                        }
                        lrTable[index][characters.indexOf(entry.getKey()) + 1] = "s" + entry.getValue();
                    } else {
                        if (!lrTable[index][j].equals("" + entry.getValue()) && lrTable[index][j].length() != 0) {
                            System.err.println("" + index + ", " + j + " " + lrTable[index][j]);
                        }
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

    private void addGOTOTable(int index, String s, int gotoIndex) {
        if (gotoTable.containsKey(index))
            gotoTable.get(index).put(s, gotoIndex);
        else {
            gotoTable.put(index, new HashMap<>());
            gotoTable.get(index).put(s, gotoIndex);
        }
    }

    public List<Production> reduce(List<Token> tokens) {
        List<Production> ans = new ArrayList<>();
//        Stack<Integer> state = new Stack<>();
//        state.push(0);
//        Queue<String> input = new LinkedList<>();
//        for (String token : tokens) {
//            input.add(token);
//        }
//        String a = input.poll();
//        int s;
//        while (true) {
//            s = state.peek() + 1;
//            int aIndex = lrTableHead.indexOf(a) + 1;
//            if (lrTable[s][aIndex].length() == 0) {
//                System.err.println("State " + s + ", stack top " + a);
//                break;
//            } else if (lrTable[s][aIndex].charAt(0) == 's') {
//                state.push(Integer.parseInt(lrTable[s][aIndex].substring(1)));
//                a = input.poll();
//            } else if (lrTable[s][aIndex].charAt(0) == 'r') {
//                Production nextProduction = getProductionFromLRTable(lrTable[s][aIndex]);
//                ans.add(nextProduction);
//                for (int i = 0; i < nextProduction.getRight().size(); i++)
//                    state.pop();
//                int t = state.peek() + 1;
//                String left = nextProduction.getLeft();
//                int leftIndex = lrTableHead.indexOf(left);
//                state.push(Integer.parseInt(lrTable[t][leftIndex]));
//            } else if (lrTable[s][aIndex].equals("acc")) {
//                break;
//            } else {
//                System.err.println("State " + s + ", stack top " + a);
//                break;
//            }
//        }
        Stack<Integer> stateStack = new Stack<>();
        Stack<String> symbolStack = new Stack<>();
        stateStack.push(0);
        symbolStack.push(STACK_BOTTOM_CHARACTER);

        int status = 0;
        int currentState;
        String currentSymbol;
        while (true) {
            currentState = stateStack.peek() + 1;
            currentSymbol = tokens.get(status).getTag().getValue();
            int aIndex = lrTableHead.indexOf(currentSymbol) + 1;
            if (lrTable[currentState][aIndex].length() == 0) {
                System.err.println("Error at line[" + tokens.get(status).getLine() + "]\nState "
                        + currentState + ", stack top " + currentSymbol);
                break;
            } else if (lrTable[currentState][aIndex].charAt(0) == 's') {
                // 移入
                stateStack.push(Integer.parseInt(lrTable[currentState][aIndex].substring(1)));
                symbolStack.push(tokens.get(status++).getTag().getValue());
            } else if (lrTable[currentState][aIndex].charAt(0) == 'r') {
                // 规约
                Production currentReduceProduction = getProductionFromLRTable(lrTable[currentState][aIndex]);
                int rightSize = currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals(EMPTY_STRING_CHARACTER) ? 0 : currentReduceProduction.getRight().size();
                for (int i = 0; i < rightSize; i++) {
                    stateStack.pop();
                    symbolStack.pop();
                }
                symbolStack.push(currentReduceProduction.getLeft());
                stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                ans.add(currentReduceProduction);
            } else if (lrTable[currentState][aIndex].equals("acc")) {
                break;
            } else {
                System.err.println("Error at line[" + tokens.get(status).getLine() + "]\nState "
                        + currentState + ", stack top " + currentSymbol);
                break;
            }
        }
        return ans;
    }

    private Production getProductionFromLRTable(String reduce) {
        String[] strings = reduce.substring(2, reduce.length() - 1).split("->");
        String left = strings[0].trim();
        String right = strings[1].charAt(0) == ' ' ? strings[1].substring(1) : strings[1];
        return new Production(left, right.split(" "));
    }

    public static void main(String[] args) throws FileNotFoundException {
        Lexer lexer = new Lexer("src/lexer/program/test.c");
        List<Token> tokens = lexer.getTokens();
        tokens.add(new Token(Tag.STACK_BOTTOM, lexer.getLines()));
//        List<String> tokenInput = new ArrayList<>();
        for (Token token : tokens) {
//            tokenInput.add(token.getTag().getValue());
            System.out.println(token);
        }
//        for (String s : tokenInput) {
//            System.out.println(s);
//        }
//        List<String> tokenInput = new ArrayList<>();
//        tokenInput.add("a");
//        tokenInput.add("b");
//        tokenInput.add("a");
//        tokenInput.add("b");
//        tokenInput.add(STACK_BOTTOM_CHARACTER);
//        for (String token : tokenInput)
//            System.out.println(token);
        String path = "src/parser/testParserFirst.txt";
        Utils utils = new Utils(path);
//        for (String non : utils.nonTerminators) {
//            System.out.println(non);
//        }
//        System.out.println("///////////////");
//        Map<String, Set<String>> ans = utils.getNonTerminatorsFirst();
//        for (String ter : utils.terminators) {
//            System.out.println(ter);
//        }
//        System.out.println("///////////////");
//        for (String key : ans.keySet()) {
//            System.out.println(key);
//            for (String s : ans.get(key)) {
//                if (s.length() == 0)
//                    continue;
//                System.out.print(s + "\t");
//            }
//            System.out.println();
//        }
//        Item startItem = new Item("C", new String[]{"c", "C"}, 1, "c");
//        Item item2 = new Item("C", new String[] {"c", "C"}, 1, "d");
//        Set<Item> items = utils.goTo(utils.getClosure(new HashSet<>(Arrays.asList(startItem, item2))), "C");
//        Set<Item> items = utils.getClosure(new HashSet<>(Collections.singletonList(new Item("Start", new String[]{"P"}, 0, "#"))));

        utils.items(new Item(Utils.START_STATE, new String[]{"P"}, 0, Utils.STACK_BOTTOM_CHARACTER));
        System.out.println(utils.itemSets.size());
//        for (ItemSet itemSet : utils.itemSets) {
//            System.out.println(itemSet);
//        }
//        List<Integer> index = new ArrayList<>(utils.itemSets);
//        Collections.sort(index);
//        for (int i = 0; i < utils.itemSets.size(); i++) {
//            if (i != index.get(i)) {
//                System.err.println(i);
//                break;
//            }
//        }
//        StringBuilder format = new StringBuilder();
//        for(int i = 0;i<utils.lrTable[0].length;i++){
//            format.append()
//        }

        PrintStream origin = System.out;
        PrintStream printStream = new PrintStream(new FileOutputStream("src/parser/LRTable.txt"));
        System.setOut(printStream);

        int len = 0;
        for (Production production : utils.productions) {
            if (len < production.toString().length())
                len = production.toString().length();
        }
        for (int i = 0; i < utils.lrTable.length; i++) {
            String[] row = utils.lrTable[i];
            for (String s : row) System.out.format("%-" + (len + 2) + "s", s);
            System.out.println();
        }
        System.setOut(origin);

        List<Production> productions = utils.reduce(tokens);
        for (Production production : productions)
            System.out.println(production);
    }
}
