package parser;

import GUI.Controller;
import dnl.utils.text.table.TextTable;
import lexer.*;
import symbols.SymbolBoard;
import symbols.SymbolItem;

import java.io.*;
import java.util.*;

// TODO 增加对错误信息的处理
public class Parser {
    public static final String PRODUCE_START_STATE = "Start";   // 增广文法的开始符号
    public static final String START_STATE = "P";   // 真正的开始符号
    public static final String ACCEPT_STRING = "acc";   // 接收符号
    public static final String STACK_BOTTOM_CHARACTER = "#";    // 栈底符号
    public static final String EMPTY_STRING_CHARACTER = "ε";    // 空串符号
    public static final String PRODUCTION_DELIMITER = "丨";  // 文法分隔符
    public static final String REDUCTION_SYMBOL = "->"; // 文法产生式推导符号

    public static final Item START_ITEM = new Item(Parser.PRODUCE_START_STATE, new String[]{START_STATE}, 0, Parser.STACK_BOTTOM_CHARACTER);
    // 开始项目

    private Map<String, Set<String>> first = new HashMap<>();
    private final Map<String, Set<String>> follow = new HashMap<>();
    private final Set<Production> productions = new HashSet<>();
    private final Set<String> nonTerminators = new HashSet<>();
    private final Set<String> terminators = new HashSet<>();
    private final Set<String> canLeadNullSet = new HashSet<>();
    private String grammarPath;

    private Set<ItemSet> itemSets = new HashSet<>();
    private Map<Integer, Map<String, Integer>> gotoTable = new HashMap<>();
    private String[][] lrTable;
    private List<String> lrTableHead;
    private Set<String> synchronizingTokens = new HashSet<>(Arrays.asList(Tag.SEMICOLON.getValue(), Tag.RP.getValue()));

    private final List<String> errorMessages = new ArrayList<>();

    private final SymbolBoard table = new SymbolBoard(null);
    private int offset = 0;

    public Parser(String grammarPath) throws IOException {
        this.grammarPath = grammarPath;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(grammarPath)));
        String string;
        while ((string = bufferedReader.readLine()) != null) {
            if (string.length() == 0)
                continue;
            String[] ss = string.split(REDUCTION_SYMBOL);
            String left = ss[0].trim();
            // 文法分割符为汉字 丨
            String[] productionList = ss[1].split(PRODUCTION_DELIMITER);
            for (String production : productionList) {
                production = production.charAt(0) == ' ' ? production.substring(1) : production;
                List<String> productionRight = new ArrayList<>();
                String syntaxAction = "";
                // Grammar Syntax Action start with "{{", ends with "}}" and splits with ";".
                // More important syntax action does not contains any spaces.
                for (String s : production.split(" ")) {
                    if (s.startsWith("{{") && s.endsWith("}}")) {
                        syntaxAction = s;
                        break;
                    } else {
                        productionRight.add(s);
                    }
                }
                Production currentProduction = new Production(left, productionRight);
                if (syntaxAction.length() > 0) {
                    String[] actions = syntaxAction.substring(2, syntaxAction.length() - 2).split(";");
                    for (String action : actions)
                        currentProduction.addSyntaxAction(action);
                }
                productions.add(currentProduction);
            }
            nonTerminators.add(left);
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
        getFirst();
        // TODO 认为输入的文法已经是拓广文法 且Start为文法的开始符号

        lrTableHead = new ArrayList<>(terminators);
        lrTableHead.addAll(nonTerminators);
        lrTableHead.remove(PRODUCE_START_STATE);
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
                        String rightFirstItem;
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

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    private static Map<String, Set<String>> deepCopyMap(Map<String, Set<String>> origin) {
        Map<String, Set<String>> answer = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : origin.entrySet()) {
            answer.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        return answer;
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
        String s = list.get(0);
        if (nonTerminators.contains(s) && !canLeadNullSet.contains(s)) {
            ans.addAll(first.get(s));
        } else if (nonTerminators.contains(s) && canLeadNullSet.contains(s)) {
            for (String string : first.get(s)) {
                if (!string.equals(EMPTY_STRING_CHARACTER)) {
                    ans.add(string);
                }
            }
            ans.addAll(getFirstFromString(list.subList(1, list.size())));
        } else {
            ans.add(s);
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

    public void items() {
        Set<Item> startItemSet = getClosure(new HashSet<>(Collections.singletonList(START_ITEM)));
        int index = 0;  // 项目集编号从0开始
        ItemSet startClosure = new ItemSet(startItemSet, index);
        itemSets.add(startClosure);

        Set<ItemSet> tempItemSet = new HashSet<>();
        tempItemSet.add(startClosure);

        index++;

        Set<String> characters = new HashSet<>(nonTerminators);
        characters.addAll(terminators);

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
        fillLrTable();
    }

    private void fillLrTable() {
        List<String> characters = new ArrayList<>(terminators);
        characters.addAll(nonTerminators);
        characters.remove(PRODUCE_START_STATE);

        // 初始化LR Table
        lrTable = new String[itemSets.size() + 1][characters.size() + 1];
        for (String[] row : lrTable)
            Arrays.fill(row, "");

        for (int i = 0; i < characters.size(); i++)
            lrTable[0][i + 1] = characters.get(i);

        for (int i = 1; i < lrTable.length; i++)
            lrTable[i][0] = "" + (i - 1);
        lrTable[0][0] = " ";

        Item startItem = new Item(PRODUCE_START_STATE, new String[]{"P"}, 1, STACK_BOTTOM_CHARACTER);

        for (ItemSet itemSet : itemSets) {
            Set<Item> items = itemSet.getItemSet();
            Map<String, Integer> gotoTable = itemSet.getGotoTable();
            int index = itemSet.getIndex() + 1;
            for (Item item : items) {
                if (item.equals(startItem)) {
                    int j = characters.indexOf(STACK_BOTTOM_CHARACTER) + 1;
                    if (lrTable[index][j].length() != 0) {
                        String errorMessage = "" + index + ", " + j + " " + lrTable[index][j];
                        System.err.println(errorMessage);
                        errorMessages.add(errorMessage);
                        lrTable[index][j] += ACCEPT_STRING;
                    } else {
                        lrTable[index][j] = ACCEPT_STRING;
                    }
                    continue;
                }
                if (!item.getLeft().equals(PRODUCE_START_STATE) &&
                        (item.getStatus() == item.getRight().length) || (item.getRight().length == 1 && item.getRight()[0].equals(EMPTY_STRING_CHARACTER))) {
                    int j = characters.indexOf(item.getSearch()) + 1;
                    if (lrTable[index][j].length() != 0) {
                        String errorMessage = "" + index + ", " + j + " " + lrTable[index][j];
                        if (lrTable[index][j].charAt(0) == 's') {
                            errorMessage = errorMessage + ", reduction and shift conflict, choose shift";
                            System.err.println(errorMessage);
                            errorMessages.add(errorMessage);
                            continue;
                        }
                        System.err.println(errorMessage);
                        errorMessages.add(errorMessage);
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
                            String errorMessage = "" + index + ", " + j + " " + lrTable[index][j];
                            System.err.println(errorMessage);
                            errorMessages.add(errorMessage);
                        }
                        lrTable[index][characters.indexOf(entry.getKey()) + 1] = "s" + entry.getValue();
                    } else {
                        if (!lrTable[index][j].equals("" + entry.getValue()) && lrTable[index][j].length() != 0) {
                            String errorMessage = "" + index + ", " + j + " " + lrTable[index][j];
                            System.err.println(errorMessage);
                            errorMessages.add(errorMessage);
                        }
                        lrTable[index][characters.indexOf(entry.getKey()) + 1] = "" + entry.getValue();
                    }
                }
            }
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
        errorMessages.clear();

        List<Production> ans = new ArrayList<>();
        Stack<Integer> stateStack = new Stack<>();
        Stack<Symbol> symbolStack = new Stack<>();
        stateStack.push(0);
        symbolStack.push(new Symbol(STACK_BOTTOM_CHARACTER));

        // 中间代码
        List<InterCode> interCodeList = new ArrayList<>();
        int nextInstr = 0;

        int status = 0;
        int errorStatus = status; // 用于避免出现死循环的错误处理 当任意状态处理连续处理两次 则跳过该状态
        int currentState;
        String currentSymbol;
        StringBuilder errorMessage = new StringBuilder();

        // 用于语义动作
        String declarations_t = "";
        String declarations_w = "";

        // 用于嵌套定义

        // 用于语义分析的临时变量
        String temp = "t";
        int countTemp = 1;

        while (true) {
            currentState = stateStack.peek() + 1;
            currentSymbol = tokens.get(status).getTag().getValue();

            // 用于语义动作
            if (symbolStack.peek().getName().equals("X")) {
                declarations_t = symbolStack.peek().getAttribute("type");
                declarations_w = symbolStack.peek().getAttribute("width");
            }
            

            int aIndex = lrTableHead.indexOf(currentSymbol) + 1;
            boolean syntaxErrorOccurred = false;
            boolean semanticErrorOccurred = false;
            if (lrTable[currentState][aIndex].length() == 0) {
                errorMessage.append("Error at line[").append(tokens.get(status).getLine()).append("]");
                syntaxErrorOccurred = true;
            } else if (lrTable[currentState][aIndex].charAt(0) == 's') {
                // 移入
                stateStack.push(Integer.parseInt(lrTable[currentState][aIndex].substring(1)));
                Token tempToken = tokens.get(status++);
                Symbol symbol = new Symbol(tempToken.getTag().getValue());
                if (tempToken instanceof Num) {
                    symbol.addAttribute("value", String.valueOf(((Num) tempToken).getValue()));
                } else if (tempToken instanceof Real) {
                    symbol.addAttribute("value", String.valueOf(((Real) tempToken).getValue()));
                } else if (tempToken instanceof Word) {
                    symbol.addAttribute("lexeme", ((Word) tempToken).getLexme());
                    symbol.addAttribute("line", String.valueOf(tempToken.getLine()));
                }
                symbolStack.push(symbol);
                if (status >= tokens.size())
                    syntaxErrorOccurred = true;
            } else if (lrTable[currentState][aIndex].charAt(0) == 'r') {
                // 规约
                Production currentReduceProduction = getProductionFromLRTable(lrTable[currentState][aIndex]);
                // 声明语句
                // D -> T id A ;
                if (currentReduceProduction.getLeft().equals("D") && currentReduceProduction.getRight().size() == 4) {
                    symbolStack.pop(); // pop ";"
                    stateStack.pop();
                    Symbol A = symbolStack.pop();
                    stateStack.pop();
                    Symbol id = symbolStack.pop();
                    stateStack.pop();
                    Symbol T = symbolStack.pop();
                    stateStack.pop();
                    String p = lookUpSymbolTable(id.getAttribute("lexeme"));
                    if (p == null) {
                        table.putSymbolItem(id.getAttribute("lexeme"), new SymbolItem(id.getAttribute("lexeme"), T.getAttribute("type"), Integer.parseInt(id.getAttribute("line")), offset));
                        offset += Integer.parseInt(T.getAttribute("width"));
                    } else {
                        semanticErrorOccurred = true;
                        errorMessage.append("Error at line[").append(id.getAttribute("line")).append("], ").append(id.getAttribute("lexeme")).append(" is defined early.");
                    }
                    Symbol symbol = new Symbol(currentReduceProduction.getLeft());
                    symbolStack.push(symbol);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // T -> X C
                else if (currentReduceProduction.getLeft().equals("T")) {
                    Symbol C = symbolStack.pop();
                    stateStack.pop();
                    Symbol X = symbolStack.pop();
                    stateStack.pop();
                    Symbol T = new Symbol(currentReduceProduction.getLeft());
                    T.addAttribute("type", C.getAttribute("type"));
                    T.addAttribute("width", C.getAttribute("width"));
                    symbolStack.push(T);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // X -> int
                else if (currentReduceProduction.getLeft().equals("X") && currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals("int")) {
                    Symbol X = new Symbol("X");
                    symbolStack.pop();
                    stateStack.pop();
                    X.addAttribute("type", "int");
                    X.addAttribute("width", "4");
                    symbolStack.push(X);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // X -> float
                else if (currentReduceProduction.getLeft().equals("X") && currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals("float")) {
                    Symbol X = new Symbol("X");
                    symbolStack.pop();
                    stateStack.pop();
                    X.addAttribute("type", "float");
                    X.addAttribute("width", "8");
                    symbolStack.push(X);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // C -> ε
                else if (currentReduceProduction.getLeft().equals("C") && currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals(EMPTY_STRING_CHARACTER)) {
                    Symbol C = new Symbol("C");
                    C.addAttribute("type", declarations_t);
                    C.addAttribute("width", declarations_w);
                    symbolStack.push(C);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // C -> [ num ] C
                else if (currentReduceProduction.getLeft().equals("C") && currentReduceProduction.getRight().size() == 4) {
                    Symbol C1 = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop(); // "]"
                    stateStack.pop();
                    Symbol num = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // "["
                    stateStack.pop();

                    Symbol C = new Symbol("C");
                    String c1_type = C1.getAttribute("type");
                    int last = c1_type.indexOf('[');
                    if (last > 0)
                        C.addAttribute("type", c1_type.substring(0, last) + "[" + num.getAttribute("value") + "]" + c1_type.substring(last));
                    else
                        C.addAttribute("type", c1_type + "[" + num.getAttribute("value") + "]");
                    C.addAttribute("width", String.valueOf(Integer.parseInt(num.getAttribute("value")) * Integer.parseInt(C1.getAttribute("width"))));
                    symbolStack.push(C);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // 赋值语句
                // S -> id = E ;
                else if (currentReduceProduction.getLeft().equals("S") && currentReduceProduction.getRight().size() == 4 && currentReduceProduction.getRight().get(0).equals("id")) {
                    symbolStack.pop(); // ";"
                    stateStack.pop();
                    Symbol E = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // "="
                    stateStack.pop();
                    Symbol id = symbolStack.pop();
                    stateStack.pop();

                    String p = lookUpSymbolTable(id.getAttribute("lexeme"));
                    if (p == null) {
                        semanticErrorOccurred = true;    // 使用未定义的标识符
                        errorMessage.append("Error at line[").append(id.getAttribute("line")).append("], ").append(id.getAttribute("lexeme")).append(" not defined");
                    } else {
                        InterCode interCode = new InterCode(new String[]{p, "=", E.getAttribute("addr")});
                        interCodeList.add(interCode);
                        nextInstr++;
                    }
                    symbolStack.push(new Symbol("S"));
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // E -> E + G
                else if (currentReduceProduction.getLeft().equals("E") && currentReduceProduction.getRight().size() == 3 && currentReduceProduction.getRight().get(2).equals("G")) {
                    Symbol G = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // "+"
                    stateStack.pop();
                    Symbol E1 = symbolStack.pop();
                    stateStack.pop();

                    Symbol E = new Symbol("E");
                    E.addAttribute("addr", temp + countTemp);
                    countTemp++;
                    InterCode interCode = new InterCode(new String[]{E.getAttribute("addr"), "=", E1.getAttribute("addr"), "+", G.getAttribute("addr")});
                    interCodeList.add(interCode);
                    nextInstr++;
                    symbolStack.push(E);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // E -> G
                else if (currentReduceProduction.getLeft().equals("E") && currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals("G")) {
                    Symbol G = symbolStack.pop();
                    stateStack.pop();
                    Symbol E = new Symbol("E");
                    E.addAttribute("addr", G.getAttribute("addr"));
                    symbolStack.push(E);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // G -> G * F
                else if (currentReduceProduction.getLeft().equals("G") && currentReduceProduction.getRight().size() == 3) {
                    Symbol F = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // "*"
                    stateStack.pop();
                    Symbol G1 = symbolStack.pop();
                    stateStack.pop();

                    Symbol G = new Symbol("G");
                    G.addAttribute("addr", temp + countTemp);
                    countTemp++;
                    InterCode interCode = new InterCode(new String[]{G.getAttribute("addr"), "=", G1.getAttribute("addr"), "*", F.getAttribute("addr")});
                    interCodeList.add(interCode);
                    nextInstr++;
                    symbolStack.push(G);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // G -> F
                else if (currentReduceProduction.getLeft().equals("G") && currentReduceProduction.getRight().size() == 1) {
                    Symbol F = symbolStack.pop();
                    stateStack.pop();
                    Symbol G = new Symbol("G");
                    G.addAttribute("addr", F.getAttribute("addr"));
                    symbolStack.push(G);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // F -> ( E )
                else if (currentReduceProduction.getLeft().equals("F") && currentReduceProduction.getRight().size() == 3) {
                    symbolStack.pop(); // "("
                    stateStack.pop();
                    Symbol E = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop(); // ")"
                    stateStack.pop();
                    Symbol F = new Symbol("F");
                    F.addAttribute("addr", E.getAttribute("addr"));
                    symbolStack.push(F);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // F -> id
                else if (currentReduceProduction.getLeft().equals("F") && currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals("id")) {
                    Symbol id = symbolStack.pop();
                    stateStack.pop();
                    String p = lookUpSymbolTable(id.getAttribute("lexeme"));
                    Symbol F = new Symbol("F");
                    F.addAttribute("addr", p);
                    if (p == null) {
                        semanticErrorOccurred = true;
                        errorMessage.append("Error at line[").append(id.getAttribute("line")).append("], ").append(id.getAttribute("lexeme")).append(" not defined");
                    }
                    symbolStack.push(F);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // F -> num
                else if (currentReduceProduction.getLeft().equals("F") && currentReduceProduction.getRight().size() == 1 && (currentReduceProduction.getRight().get(0).equals("num") || currentReduceProduction.getRight().get(0).equals("real"))) {
                    Symbol number = symbolStack.pop();
                    stateStack.pop();
                    Symbol F = new Symbol("F");
                    F.addAttribute("addr", number.getAttribute("value"));
                    symbolStack.push(F);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // I -> false
                else if (currentReduceProduction.getLeft().equals("I") && currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals("false")) {
                    symbolStack.pop();  // pop false
                    stateStack.pop();
                    Symbol I = new Symbol("I");
//                    I.addAttribute("falseList", nextInstr + "");
                    I.makeList(nextInstr, 0);
                    InterCode interCode = new InterCode(new String[]{"goto"});
                    interCodeList.add(interCode);
                    nextInstr++;
                    symbolStack.push(I);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // I -> true
                else if (currentReduceProduction.getLeft().equals("I") && currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals("true")) {
                    symbolStack.pop(); // pop true
                    stateStack.pop();
                    Symbol I = new Symbol("I");
//                    I.addAttribute("trueList", nextInstr + "");
                    I.makeList(nextInstr, 1);
                    InterCode interCode = new InterCode(new String[]{"goto"});
                    interCodeList.add(interCode);
                    nextInstr++;
                    symbolStack.push(I);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // I -> E Relop E
                else if (currentReduceProduction.getLeft().equals("I") && currentReduceProduction.getRight().size() == 3 && currentReduceProduction.getRight().get(0).equals("E")) {
                    Symbol E1 = symbolStack.pop();
                    stateStack.pop();
                    Symbol Relop = symbolStack.pop();
                    stateStack.pop();
                    Symbol E2 = symbolStack.pop();
                    stateStack.pop();

                    Symbol I = new Symbol("I");
                    I.makeList(nextInstr, 1);
                    I.makeList(nextInstr + 1, 0);
//                    I.addAttribute("trueList", nextInstr + "");
//                    I.addAttribute("falseList", (nextInstr + 1) + "");
                    InterCode interCode1 = new InterCode(new String[]{"if", E2.getAttribute("addr"), Relop.getAttribute("op"), E1.getAttribute("addr"), "goto"});
                    InterCode interCode2 = new InterCode(new String[]{"goto"});
                    interCodeList.add(interCode1);
                    interCodeList.add(interCode2);
                    nextInstr = nextInstr + 2;
                    symbolStack.push(I);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // I -> ( B )
                else if (currentReduceProduction.getLeft().equals("I") && currentReduceProduction.getRight().size() == 3 && currentReduceProduction.getRight().get(0).equals("(")) {
                    symbolStack.pop();  // "("
                    stateStack.pop();
                    Symbol B = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // ")"
                    stateStack.pop();

                    Symbol I = new Symbol("I");
                    I.addList(B.getTrueList(), 1);
                    I.addList(B.getFalseList(), 0);
//                    I.addAttribute("trueList", B.getAttribute("trueList"));
//                    I.addAttribute("falseList", B.getAttribute("falseList"));
                    symbolStack.push(I);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // I -> ! I
                else if (currentReduceProduction.getLeft().equals("I") && currentReduceProduction.getRight().size() == 2) {
                    Symbol I1 = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // "!"
                    stateStack.pop();

                    Symbol I = new Symbol("I");
                    I.addList(I1.getFalseList(), 1);
                    I.addList(I1.getTrueList(), 0);
//                    I.addAttribute("trueList", I1.getAttribute("trueList"));
//                    I.addAttribute("falseList", I1.getAttribute("falseList"));
                    symbolStack.push(I);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // Relop -> < | <= | > | >= | == | !=
                else if (currentReduceProduction.getLeft().equals("Relop") && currentReduceProduction.getRight().size() == 1) {
                    Symbol op = symbolStack.pop();
                    stateStack.pop();

                    Symbol Relop = new Symbol("Relop");
                    Relop.addAttribute("op", op.getName());
                    symbolStack.push(Relop);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // BM -> epsilon
                else if (currentReduceProduction.getLeft().equals("BM")) {
                    Symbol BM = new Symbol("BM");
                    BM.addAttribute("instr", nextInstr + "");

                    symbolStack.push(BM);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // H -> H && BM I
                else if (currentReduceProduction.getLeft().equals("H") && currentReduceProduction.getRight().size() > 1) {
                    Symbol I = symbolStack.pop();
                    stateStack.pop();
                    Symbol BM = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop(); // &&
                    stateStack.pop();
                    Symbol H1 = symbolStack.pop();
                    stateStack.pop();

                    // backpatch
                    for (int i : H1.getTrueList())
                        interCodeList.get(i).backPatch(BM.getAttribute("instr"));

                    Symbol H = new Symbol("H");
                    H.addList(I.getTrueList(), 1);
                    H.merge(H1.getFalseList(), I.getFalseList(), 0);
                    symbolStack.push(H);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // H -> I
                else if (currentReduceProduction.getLeft().equals("H") && currentReduceProduction.getRight().size() == 1) {
                    Symbol I = symbolStack.pop();
                    stateStack.pop();
                    Symbol H = new Symbol("H");
                    H.addList(I.getTrueList(), 1);
                    H.addList(I.getFalseList(), 0);

                    symbolStack.push(H);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // B -> B || BM H
                else if (currentReduceProduction.getLeft().equals("B") && currentReduceProduction.getRight().size() > 1) {
                    Symbol H = symbolStack.pop();
                    stateStack.pop();
                    Symbol BM = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // pop "||"
                    stateStack.pop();
                    Symbol B1 = symbolStack.pop();
                    stateStack.pop();
                    Symbol B = new Symbol("B");

                    for (int i : B1.getFalseList())
                        interCodeList.get(i).backPatch(BM.getAttribute("instr"));
                    B.merge(B1.getTrueList(), H.getTrueList(), 1);
                    B.addList(H.getFalseList(), 0);
                    symbolStack.push(B);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // B -> H
                else if (currentReduceProduction.getLeft().equals("B") && currentReduceProduction.getRight().size() == 1) {
                    Symbol H = symbolStack.pop();
                    stateStack.pop();

                    Symbol B = new Symbol("B");
                    B.addList(H.getFalseList(), 0);
                    B.addList(H.getTrueList(), 1);
                    symbolStack.push(B);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // S -> if ( B ) BM S N else BM S
                else if (currentReduceProduction.getLeft().equals("S") && currentReduceProduction.getRight().size() == 10) {
                    Symbol S2 = symbolStack.pop();
                    stateStack.pop();
                    Symbol BM2 = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop(); // pop "else"
                    stateStack.pop();
                    Symbol N = symbolStack.pop();
                    stateStack.pop();
                    Symbol S1 = symbolStack.pop();
                    stateStack.pop();
                    Symbol BM1 = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // pop ")"
                    stateStack.pop();
                    Symbol B = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // pop "("
                    stateStack.pop();
                    symbolStack.pop();  // pop "if"
                    stateStack.pop();

                    Symbol S = new Symbol("S");
                    for (int i : B.getTrueList())
                        interCodeList.get(i).backPatch(BM1.getAttribute("instr"));
                    for (int i : B.getFalseList())
                        interCodeList.get(i).backPatch(BM2.getAttribute("instr"));
                    List<Integer> tempIntegerList = new ArrayList<>();
                    tempIntegerList.addAll(new HashSet<>(S1.getNextList()));
                    tempIntegerList.addAll(new HashSet<>(N.getNextList()));
                    S.merge(tempIntegerList, S2.getNextList(), -1);

                    symbolStack.push(S);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // S -> if ( B ) BM S
                else if (currentReduceProduction.getLeft().equals("S") && currentReduceProduction.getRight().size() == 6 && currentReduceProduction.getRight().get(0).equals("if")) {
                    Symbol S1 = symbolStack.pop();
                    stateStack.pop();
                    Symbol BM = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // ")"
                    stateStack.pop();
                    Symbol B = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // "("
                    stateStack.pop();
                    symbolStack.pop();  // "if"
                    stateStack.pop();

                    for (int i : B.getTrueList())
                        interCodeList.get(i).backPatch(BM.getAttribute("instr"));
                    Symbol S = new Symbol("S");
                    S.merge(B.getFalseList(), S1.getNextList(), -1);
                    symbolStack.push(S);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // S -> while BM ( B ) BM S
                else if (currentReduceProduction.getLeft().equals("S") && currentReduceProduction.getRight().size() == 7 && currentReduceProduction.getRight().get(0).equals("while")) {
                    Symbol S1 = symbolStack.pop();
                    stateStack.pop();
                    Symbol BM2 = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop(); // pop ")"
                    stateStack.pop();
                    Symbol B = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // pop "("
                    stateStack.pop();
                    Symbol BM1 = symbolStack.pop();
                    stateStack.pop();
                    symbolStack.pop();  // pop "while"
                    stateStack.pop();

                    Symbol S = new Symbol("S");
                    for(int i : S1.getNextList())
                        interCodeList.get(i).backPatch(BM1.getAttribute("instr"));
                    for(int i : B.getTrueList())
                        interCodeList.get(i).backPatch(BM2.getAttribute("instr"));
                    S.addList(B.getFalseList(), -1);
                    InterCode interCode = new InterCode(new String[]{"goto", BM1.getAttribute("instr")});
                    interCodeList.add(interCode);
                    nextInstr++;
                    symbolStack.push(S);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // N -> epsilon
                else if(currentReduceProduction.getLeft().equals("N") && currentReduceProduction.getRight().size() == 1){
                    Symbol N = new Symbol("N");
                    N.makeList(nextInstr, -1);
                    InterCode interCode = new InterCode(new String[]{"goto"});
                    interCodeList.add(interCode);
                    nextInstr++;
                    symbolStack.push(N);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
                // without semantic action
                else {
                    int rightSize = currentReduceProduction.getRight().size() == 1 && currentReduceProduction.getRight().get(0).equals(EMPTY_STRING_CHARACTER) ? 0 : currentReduceProduction.getRight().size();
                    for (int i = 0; i < rightSize; i++) {
                        stateStack.pop();
                        symbolStack.pop();
                    }
                    Symbol symbol = new Symbol(currentReduceProduction.getLeft());
                    symbolStack.push(symbol);
                    stateStack.push(Integer.parseInt(lrTable[stateStack.peek() + 1][lrTableHead.indexOf(currentReduceProduction.getLeft()) + 1]));
                    ans.add(currentReduceProduction);
                }
            } else if (lrTable[currentState][aIndex].equals(ACCEPT_STRING)) {
                break;
            } else {
                errorMessage.append("Error at line[").append(tokens.get(status).getLine()).append("]");
                syntaxErrorOccurred = true;
            }

            if (syntaxErrorOccurred) {
                String searchSymbol;
                int recoveryState;
                int searchSymbolIndex;
                stateStack.pop();
                boolean foundA = false;
                boolean foundErrorReason = false;   // 标记是否找到错误原因
                while (true) {
                    searchSymbol = symbolStack.peek().getName();
                    recoveryState = stateStack.peek() + 1;
                    searchSymbolIndex = lrTableHead.indexOf(searchSymbol) + 1;
                    if (nonTerminators.contains(searchSymbol) && lrTable[recoveryState][searchSymbolIndex].length() > 0) {
                        stateStack.push(Integer.parseInt(lrTable[recoveryState][searchSymbolIndex]));
                        foundA = true;
                        break;
                    } else {
                        stateStack.pop();
                        symbolStack.pop();
                        if (stateStack.empty()) {
                            symbolStack.pop();
                            break;
                        }
                    }
                }

                while (status < tokens.size()) {
                    String token = tokens.get(status++).getTag().getValue();
                    recoveryState = stateStack.peek() + 1;
                    int tokenIndex = lrTableHead.indexOf(token) + 1;
                    if (foundA && lrTable[recoveryState][tokenIndex].length() > 0) {
                        foundErrorReason = true;
                        status--;
                        errorMessage.append(", ").append(probableSyntaxErrorMessage(currentSymbol, token));
                        if (errorStatus == status)
                            ++status;
                        else
                            errorStatus = status;
                        break;
                    } else if (!foundA && synchronizingTokens.contains(token)) {
                        status--;
                        foundErrorReason = true;
                        errorMessage.append(", Syntax Error");
                        if (errorStatus == status)
                            ++status;
                        else
                            errorStatus = status;
                        break;
                    }
                }
                if (status >= tokens.size()) {
                    if (!foundErrorReason && errorMessage.length() != 0) {
                        errorMessage.append(", Syntax error");
                        System.err.println(errorMessage.toString());
                        errorMessages.add(errorMessage.toString());
                    }
                    return ans;
                }
                System.err.println(errorMessage.toString());
                errorMessages.add(errorMessage.toString());
                errorMessage = new StringBuilder();
            }

            if (semanticErrorOccurred) {
                System.err.println(errorMessage.toString());
                errorMessages.add(errorMessage.toString());
            }
        }
        // TODO 表达式方向不对 M记录的位置不对
        for (int i = 0; i < interCodeList.size(); i++)
            System.out.println(i + " : " + interCodeList.get(i));

        return ans;
    }

    private Production getProductionFromLRTable(String reduce) {
        String[] strings = reduce.substring(2, reduce.length() - 1).split(REDUCTION_SYMBOL);
        String left = strings[0].trim();
        String right = strings[1].charAt(0) == ' ' ? strings[1].substring(1) : strings[1];
        return new Production(left, right.split(" "));
    }

    private String probableSyntaxErrorMessage(String before, String after) {
        if (before.equals("(") && after.equals("id"))
            return "Maybe missing a \")\"";
        else if (before.equals("[") && after.equals("id"))
            return "Maybe missing a \"]\"";
        else if (Lexer.getKeywords().contains(before) && (after.equals("id") || after.equals("=")))
            return "Maybe missing a \";\"";
        else if (before.equals("{") && after.equals("id"))
            return "Maybe missing a \"}\"";
        else if ((Lexer.getKeywords().contains(before) || before.equals("id") && (Lexer.getKeywords().contains(after) || after.equals("id"))))
            return "Maybe missing a delimiter";
        else
            return "Error around " + before + " and " + after;
    }

    private String lookUpSymbolTable(String lexeme) {
        for (SymbolBoard symbolBoard = table; symbolBoard != null; symbolBoard = symbolBoard.getPrev()) {
            if (symbolBoard.getSymbolItem(lexeme) != null)
                return lexeme;  // TODO 仅仅返回了该id对应的名字 而未真正返回地址
        }
        return null;
    }

    public void outputLRTableToFile() throws IOException {
        outputLRTableToFile(Controller.lrTablePath);
    }

    public void outputLRTableToFile(String lrTableFilePath) throws IOException {
        Controller.lrTablePath = lrTableFilePath;
        PrintStream origin = System.out;
        PrintStream printStream = new PrintStream(new FileOutputStream(lrTableFilePath));
        System.setOut(printStream);
        TextTable textTable = new TextTable(lrTable[0], Arrays.copyOfRange(lrTable, 1, lrTable.length));
        textTable.printTable();
        System.setOut(origin);
    }


    public static void main(String[] args) throws IOException {
        Lexer lexer = new Lexer("src/lexer/program/test");
        List<Token> tokens = lexer.getTokens();
        for (Token token : tokens) {
            System.out.println(token);
        }
        String path = "src/parser/testParserFirst.txt";
        Parser parser = new Parser(path);
        parser.items();
//        for (ItemSet itemSet : parser.itemSets)
//            System.out.println(itemSet);
//        parser.outputLRTableToFile();
//        System.out.println(parser.itemSets.size());
        List<Production> productions = parser.reduce(tokens);
        for (Production production : productions)
            System.out.println(production);
    }
}
