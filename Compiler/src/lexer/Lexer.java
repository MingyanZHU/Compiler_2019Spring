package lexer;

import symbols.SymbolBoard;
import symbols.SymbolItem;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Lexer {
    private final String path;
    private int lines = 1;
    private final Queue<Character> buffer = new LinkedList<>();
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();
    private BufferedReader bufferedReader;
    private static final Set<String> keywords = new HashSet<>(Arrays.asList("int", "float", "bool", "char", "record", "if", "else", "while", "do", "break", "continue", "true", "false", "proc", "call", "return"));
    private static final Set<Character> skipSymbol = new HashSet<>(Arrays.asList(' ', '\t', '\n'));
    private static final Set<Character> delimiters = new HashSet<>(Arrays.asList(',', '=', ';', '[', ']', '{', '}', '(', ')'));
    private static final Set<Character> ambiguousSymbol = new HashSet<>(Arrays.asList('!', '&', '|', '>', '<', '/', '='));
    private static final Set<String> relationOp = new HashSet<>(Arrays.asList("!=", ">", ">=", "<", "<=", "=="));
    private static final Set<String> logicalOp = new HashSet<>(Arrays.asList("&", "|", "||", "&&", "^", "!"));
    private static final Set<Character> arithmeticOp = new HashSet<>(Arrays.asList('+', '-', '*', '/', '%'));

    public Lexer(String path) {
        this.path = path;
        try {
            this.bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        scan();
        tokens.add(new Token(Tag.STACK_BOTTOM, lines));
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private Token reconID(char c) {
        StringBuilder builder = new StringBuilder();
        builder.append(c);
        int x;
        try {
            while ((x = bufferedReader.read()) != -1) {
                char cc = (char) x;
                if (isLetter(cc) || isDigit(cc) || cc == '_')
                    builder.append(cc);
                else {
                    buffer.add(cc);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String ans = builder.toString();
        if (keywords.contains(ans))
            return new Token(Tag.valueOf(ans.toUpperCase()), lines);
        else {
            return new Word(Tag.ID, ans, lines);
        }
    }

    private Token reconNumber(char c) throws LexerException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(c);
        int x;
        int state = c == '.' ? 12 : 3;
        try {
            while ((x = bufferedReader.read()) != -1) {
                char cc = (char) x;
                switch (state) {
                    case 3:
                        if (isDigit(cc)) {
                            stringBuilder.append(cc);
                            state = 3;
                        } else if (cc == '.') {
                            state = 4;
                            stringBuilder.append(cc);
                        } else if (cc == 'E' || cc == 'e') {
                            state = 6;
                            stringBuilder.append(cc);
                        } else {
                            state = 10;
                            buffer.add(cc);
                        }
                        break;
                    case 4:
                        if (isDigit(cc)) {
                            state = 5;
                            stringBuilder.append(cc);
                        } else {
                            state = 11;
                            buffer.add(cc);
                        }
                        break;
                    case 5:
                        if (isDigit(cc)) {
                            // not change state
                            stringBuilder.append(cc);
                        } else if (cc == 'e' || cc == 'E') {
                            state = 6;
                            stringBuilder.append(cc);
                        } else {
                            state = 11;
                            buffer.add(cc);
                        }
                        break;
                    case 6:
                        if (cc == '+' || cc == '-') {
                            state = 7;
                            stringBuilder.append(cc);
                        } else if (isDigit(cc)) {
                            state = 8;
                            stringBuilder.append(cc);
                        } else {
                            throw new LexerException(stringBuilder.toString() + "数字格式错误");
                        }
                        break;
                    case 7:
                        if (isDigit(cc)) {
                            state = 8;
                            stringBuilder.append(cc);
                        } else {
                            throw new LexerException(stringBuilder.toString() + "数字格式错误");
                        }
                        break;
                    case 8:
                        if (isDigit(cc)) {
                            // not change state
                            stringBuilder.append(cc);
                        } else {
                            state = 9;
                            buffer.add(cc);
                        }
                        break;
                    case 12:
                        if (isDigit(cc)) {
                            state = 5;
                            stringBuilder.append(cc);
                        } else {
                            throw new LexerException(stringBuilder.toString() + "数字格式错误");
                        }
                        break;

                }
                if (state == 9 || state == 10 || state == 11)
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (state == 10)
            return new Num(Tag.NUM, Integer.valueOf(stringBuilder.toString()), lines);
        else
            return new Real(Tag.REAL, Double.valueOf(stringBuilder.toString()), lines);
    }

    private void reconComment(String start) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(start);
        int state = 2;
        int x;
        try {
            while ((x = bufferedReader.read()) != -1) {
                char c = (char) x;
                if (c == '\n')
                    lines++;
                switch (state) {
                    case 2:
                        if (c == '*') {
                            state = 3;
                            stringBuilder.append(c);
                        } else {
                            state = 2;
                            stringBuilder.append(c);
                        }
                        break;
                    case 3:
                        if (c == '*') {
                            state = 3;
                            stringBuilder.append(c);
                        } else if (c == '/') {
                            state = 4;
                            stringBuilder.append(c);
                        } else {
                            state = 2;
                            stringBuilder.append(c);
                        }
                        break;
                    case 4:
                        break;
                }
                if (state == 4)
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("///////////////////////////\n" + stringBuilder.toString() + "\n//////////////////////////");
    }

    private Token reconString(char c) throws LexerException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(c);

        int x;
        try {
            while ((x = bufferedReader.read()) != -1) {
                char cc = (char) x;
                if (cc == '\n') {
                    throw new LexerException(stringBuilder.toString() + "\\n" + "字符串格式错误");
                }
                stringBuilder.append(cc);
                if (cc == '"') {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Word(Tag.STRING, stringBuilder.toString(), lines);
    }

    private void panicMode() {
        int x = -2;
        try {
            while (!buffer.isEmpty() || (x = bufferedReader.read()) != -1) {
                char c = !buffer.isEmpty() ? buffer.poll() : (char) x;
                if (c == '\n') {
                    lines++;
                    break;
                } else if (delimiters.contains(c)) {
                    buffer.add(c);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scan() {
        try {
            int x = -2;
            Token token;

            while (!buffer.isEmpty() || (x = bufferedReader.read()) != -1) {
                try {
                    char c = !buffer.isEmpty() ? buffer.poll() : (char) x;
                    if (skipSymbol.contains(c)) {
                        if (c == '\n')
                            lines++;
                    } else if (isLetter(c) || x == '_') {
                        token = reconID(c);
//                        System.out.println(token);
                        tokens.add(token);
                    } else if (isDigit(c) || c == '.') {
                        token = reconNumber(c);
//                        System.out.println(token);
                        tokens.add(token);
                    } else if (c == '"') {
                        token = reconString(c);
//                        System.out.println(token);
                        tokens.add(token);
                    } else if (ambiguousSymbol.contains(c)) {
                        char c2 = !buffer.isEmpty() ? buffer.poll() : (char) bufferedReader.read();
                        String temp = String.valueOf(new char[]{c, c2});
                        if (relationOp.contains(temp) || logicalOp.contains(temp)) {
                            token = new Token(Tag.fromString(temp), lines);
//                            System.out.println(token);
                            tokens.add(token);
                        } else if (temp.equals("/*")) {
                            reconComment(temp);
                        } else {
                            token = new Token(Tag.fromString(String.valueOf(c)), lines);
//                            System.out.println(token);
                            tokens.add(token);
                            buffer.add(c2);
                        }
                    } else if (arithmeticOp.contains(c)) {
                        token = new Token(Tag.fromString(String.valueOf(c)), lines);
//                        System.out.println(token);
                        tokens.add(token);
                    } else if (delimiters.contains(c)) {
                        token = new Token(Tag.fromString(String.valueOf(c)), lines);
//                        System.out.println(token);
                        tokens.add(token);
                    } else {
//                        System.out.println(c);
                        throw new LexerException("\"" + c + "\"为未定义的字符");
                    }
                } catch (LexerException e) {
                    String errorMessage = "[ERROR " + lines + "]" + e.getMessage();
                    System.err.println(errorMessage);
                    errors.add(errorMessage);
                    panicMode();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        System.out.println("//////Token序列//////");
//        for (Token token : tokens)
//            System.out.println(token);
    }

    public String getTokensList() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Token token : tokens)
            stringBuilder.append(token).append("\n");
        return stringBuilder.toString();
    }

    public List<Token> getTokens() {
        return new ArrayList<>(tokens);
    }

    public String getErrors() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String error : errors) {
            stringBuilder.append(error).append("\n");
        }
        return stringBuilder.toString();
    }

    public int getLines() {
        return lines;
    }

    public static Set<String> getKeywords() {
        return keywords;
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("src/lexer/program/test.c");
    }
}
