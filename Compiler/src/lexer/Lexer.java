package lexer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Lexer {
    private final String path;
    private final Queue<Character> buffer = new LinkedList<>();
    private final List<Token> lex = new ArrayList<>();
    private BufferedReader bufferedReader;
    private static final Set<String> keywords = new HashSet<>(Arrays.asList("int", "double", "bool", "if", "else", "while", "do"));
    private static final Set<Character> skipSymbol = new HashSet<>(Arrays.asList(' ', '\t', '\n'));
    private static final Set<Character> delimiters = new HashSet<>(Arrays.asList('=', ';', '[', ']', '{', '}', '(', ')'));
    private static final Set<Character> ambiguousSymbol = new HashSet<>(Arrays.asList('!', '&', '|', '>', '<'));
    private static final Set<String> relationOp = new HashSet<>(Arrays.asList("!=", ">", ">=", "<", "<="));
    private static final Set<String> logicalOp = new HashSet<>(Arrays.asList("&", "|", "||", "&&", "^", "!"));
    private static final Set<Character> arithmeticOp = new HashSet<>(Arrays.asList('+', '-', '*', '/', '%'));

    // TODO 布尔型变量 符号表的处理

    public Lexer(String path) {
        this.path = path;
        try {
            this.bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            return new Token(ans, Tag.valueOf(ans.toUpperCase()), "");
        else
            return new Token(ans, Tag.ID, ans);
    }

    private Token reconNumber(char c) throws LexerException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(c);
        int x = -2;
        int state = 2;
        try {
            while ((x = bufferedReader.read()) != -1) {
                char cc = (char) x;
                switch (state) {
                    case 2:
                        if (isDigit(cc)) {
                            state = 4;
                            stringBuilder.append(cc);
                        } else if (cc == '.') {
                            state = 3;
                            stringBuilder.append(cc);
                        } else {
                            state = 8;
                        }
                        break;
                    case 3:
                        if (isDigit(cc)) {
                            state = 4;
                            stringBuilder.append(cc);
                        } else {
                            throw new LexerException(stringBuilder.toString() + "数字格式错误");
                        }
                        break;
                    case 4:
                        if (isDigit(cc)) {
                            state = 4;
                            stringBuilder.append(cc);
                        } else if (cc == 'E' || cc == 'e') {
                            state = 5;
                            stringBuilder.append(cc);
                        } else {
                            state = 8;
                        }
                        break;
                    case 5:
                        if (isDigit(cc)) {
                            state = 7;
                            stringBuilder.append(cc);
                        } else if (cc == '+' || cc == '-') {
                            state = 6;
                            stringBuilder.append(cc);
                        } else {
                            throw new LexerException(stringBuilder.toString() + " 数字格式错误");
                        }
                        break;
                    case 6:
                        if (isDigit(cc)) {
                            state = 7;
                            stringBuilder.append(cc);
                        } else {
                            throw new LexerException(stringBuilder.toString() + " 数字格式错误");
                        }
                        break;
                    case 7:
                        if (isDigit(cc)) {
                            state = 7;
                            stringBuilder.append(cc);
                        } else {
                            state = 8;
                        }
                        break;
                    case 8:
                        break;
                }
                if (state == 8)
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Token(stringBuilder.toString(), Tag.REAL, stringBuilder.toString());
//        System.out.println("<" + Tag.REAL + ", " + stringBuilder.toString() + ">");
//        return x;
    }

    public void scan() {
        try {
            int x = -2;
            Token token;
            while (!buffer.isEmpty() || (x = bufferedReader.read()) != -1) {
                char c = !buffer.isEmpty() ? buffer.poll() : (char) x;
                if (skipSymbol.contains(c)) {
                } else if (isLetter(c) || x == '_') {
                    token = reconID(c);
                    System.out.println(token);
                    lex.add(token);
                } else if (isDigit(c)) {
                    token = reconNumber(c);
                    System.out.println(token);
                    lex.add(token);
                } else if (arithmeticOp.contains(c)) {
                    token = new Token(String.valueOf(c), Tag.fromString(String.valueOf(c)), "");
                    System.out.println(token);
                    lex.add(token);
                } else if (ambiguousSymbol.contains(c)) {
                    char c2 = !buffer.isEmpty() ? buffer.poll() : (char) bufferedReader.read();
                    String temp = String.valueOf(new char[]{c, c2});
                    if (relationOp.contains(temp) || logicalOp.contains(temp)) {
                        token = new Token(temp, Tag.fromString(temp), "");
                    } else {
                        token = new Token(String.valueOf(c), Tag.fromString(String.valueOf(c)), "");
                        buffer.add(c2);
                    }
                    System.out.println(token);
                    lex.add(token);
                } else if (delimiters.contains(c)) {
                    token = new Token(String.valueOf(c), Tag.fromString(String.valueOf(c)), "");
                    System.out.println(token);
                    lex.add(token);
                } else {
                    System.out.println(c);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (LexerException e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Lexer lexer = new Lexer("src/lexer/program/test.c");
        lexer.scan();
    }
}
