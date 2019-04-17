package parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Utils {
    private Map<String, Set<String>> first = new HashMap<>();
    private final Map<String, Set<String>> follow = new HashMap<>();
    //    private final Map<String, List<String>> production = new HashMap<>();
    private final Set<Production> productions = new HashSet<>();
    private final Set<String> nonTerminators = new HashSet<>();
    private final Set<String> canLeadNullSet = new HashSet<>();
    private String grammarPath;
    private BufferedReader bufferedReader;

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
//        nonTerminators.addAll(production.keySet());
        initFirst();
        getFirstNonTerminatorStep(1);
    }

    private void initFirst() {
        for (String key : nonTerminators) {
            first.put(key, new HashSet<>());
        }
    }

    private void getFirstNonTerminatorStep(int step) {
        while (true) {
            boolean unChanged = true;
            Map<String, Set<String>> tempFirst = deepCopy(first);
            for (String key : nonTerminators) {
                for (Production production : productions) {
                    if (production.getLeft().equals(key)) {
                        String rightFirstItem = "";
                        if(step == 1)
                            rightFirstItem = production.getRight().get(0);
                        else{
                            rightFirstItem = "";
                            for(String right : production.getRight()){
                                if(!canLeadNull(right)){
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
                first = deepCopy(tempFirst);
        }
    }

    public Map<String, Set<String>> getFirst() {
        for (String key : nonTerminators) {
            if (first.get(key).contains("$"))
                canLeadNullSet.add(key);
        }
        getFirstNonTerminatorStep(2);
        return deepCopy(first);
    }

    private static Map<String, Set<String>> deepCopy(Map<String, Set<String>> origin) {
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
                    if (production.getRight().get(0).equals("$")) {
                        canLeadNullSet.add(rightString);
                        return true;
                    } else {
                        boolean flag = true;
                        List<String> rights = production.getRight();
                        for (String right : rights) {
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

//    public Set<String> getFirst(String string){
//        if (nonTerminators.contains(string))
//            return first.get(string);
//        else if(!string.contains(" "))
//            return new HashSet<>(Collections.singletonList(string));
//        else {
//            // 串
//            String firstRight =
//        }
//    }

    public static void main(String[] args) {
        String path = "src/parser/testParserFirst.txt";
        Utils utils = new Utils(path);
        Map<String, Set<String>> ans = utils.getFirst();
        for (String key : ans.keySet()) {
            System.out.println(key);
            for (String s : ans.get(key))
                System.out.print(s + "\t");
            System.out.println();
        }

    }
}
