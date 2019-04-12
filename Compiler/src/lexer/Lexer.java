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
    private static final Set<Character> ambiguousSymbol = new HashSet<>(Arrays.asList('!', '&', '|', '>', '<', '/', '='));
    private static final Set<String> relationOp = new HashSet<>(Arrays.asList("!=", ">", ">=", "<", "<=", "=="));
    private static final Set<String> logicalOp = new HashSet<>(Arrays.asList("&", "|", "||", "&&", "^", "!"));
    private static final Set<Character> arithmeticOp = new HashSet<>(Arrays.asList('+', '-', '*', '/', '%'));

    // TODO 布尔型变量 符号表的处理 增加错误处理

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
        // TODO 区分Integer和Float类型的正数
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(c);
        int x;
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
                            buffer.add(cc);
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
                            buffer.add(cc);
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
                            buffer.add(cc);
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
    }

    private void reconComment(String start) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(start);
        int state = 2;
        int x;
        try {
            while ((x = bufferedReader.read()) != -1) {
                char c = (char) x;
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

    private Token reconString(char c) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(c);

        int x;
        try {
            while ((x = bufferedReader.read()) != -1) {
                char cc = (char) x;
                stringBuilder.append(cc);
                if (cc == '"') {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Token(stringBuilder.toString(), Tag.STRING, stringBuilder.toString());
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
                } else if (c == '"') {
                    token = reconString(c);
                    System.out.println(token);
                    lex.add(token);
                } else if (ambiguousSymbol.contains(c)) {
                    char c2 = !buffer.isEmpty() ? buffer.poll() : (char) bufferedReader.read();
                    String temp = String.valueOf(new char[]{c, c2});
                    if (relationOp.contains(temp) || logicalOp.contains(temp)) {
                        token = new Token(temp, Tag.fromString(temp), "");
                        System.out.println(token);
                        lex.add(token);
                    } else if (temp.equals("/*")) {
                        reconComment(temp);
                    } else {
                        token = new Token(String.valueOf(c), Tag.fromString(String.valueOf(c)), "");
                        System.out.println(token);
                        lex.add(token);
                        buffer.add(c2);
                    }
                } else if (arithmeticOp.contains(c)) {
                    token = new Token(String.valueOf(c), Tag.fromString(String.valueOf(c)), "");
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
