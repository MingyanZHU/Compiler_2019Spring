package GUI;

import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import lexer.Lexer;
import parser.Parser;
import parser.Production;
import parser.Symbol;
import symbols.SymbolBoard;

import java.io.*;
import java.util.List;

public class Controller {
    public TextArea tokenArea = new TextArea();
    public TextArea lexerErrorArea = new TextArea();
    public Tab sourceTab = new Tab();
    public TextArea sourceCode = new TextArea();
    public Tab lexerTab = new Tab();
    public Tab parserTab = new Tab();

    public static String sourcePath = "src/lexer/program/test.c";
    public static String grammarPath = "src/parser/Grammar_Good.txt";
    public static String lrTablePath = "src/parser/LRTable.txt";
    private static final String errorCSS = "-fx-font-family: \"Noto Sans CJK SC Bold\";" +
            "-fx-text-fill: #ff4a1c;";

    public Tab grammarTab = new Tab();
    public TextArea grammarArea = new TextArea();
    public Tab lrTableTab = new Tab();
    public TextArea lrTableArea = new TextArea();
    public TextArea parserProductions = new TextArea();
    public TextArea parserErrorArea = new TextArea();
    public Tab semanticTab = new Tab();
    public TextArea interCodeArea = new TextArea();
    public TextArea semanticErrorArea = new TextArea();
    public TextArea symbolTable = new TextArea();
    private Lexer lexer;
    private Parser parser;

    public Controller() {
        try {
            lexer = new Lexer(sourcePath);
            lexer.scan();
            parser = new Parser(grammarPath);
            parser.items();
            parser.outputLRTableToFile();
        } catch (IOException e) {
            e.printStackTrace();
            fileNotExistAlert(grammarPath + " or " + lrTablePath);
        }
    }

    private String readFromFile(String path) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));
        int s;
        while ((s = bufferedReader.read()) != -1) {
            stringBuilder.append((char) s);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }

    private void fileNotExistAlert(String filePath) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("源文件不存在");
        alert.setContentText(filePath + "文件不存在");
        alert.showAndWait();
    }

    private void lexerReadSourceCode() throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sourcePath)));
        bufferedWriter.write(sourceCode.getText());
        bufferedWriter.close();

        lexer = new Lexer(sourcePath);
    }

    public void tabChanged() {
        try {
            if (sourceTab.isSelected()) {
                String code = readFromFile(sourcePath);
                sourceCode.setText(code);
            } else if (lexerTab.isSelected()) {
                lexerReadSourceCode();
                lexerErrorArea.setStyle(errorCSS);

                tokenArea.setEditable(false);
                tokenArea.setText(lexer.getTokensList());

                lexerErrorArea.setEditable(false);
                lexerErrorArea.setText(lexer.getErrors());
            } else if (grammarTab.isSelected()) {
                String grammar = readFromFile(grammarPath);
                grammarArea.setEditable(false);
                grammarArea.setText(grammar);
            } else if (lrTableTab.isSelected()) {
                String lrTable = readFromFile(lrTablePath);
                lrTableArea.setEditable(false);
                lrTableArea.setText(lrTable);
            } else if (parserTab.isSelected()) {
                lexerReadSourceCode();
                List<Production> productions = parser.reduce(lexer.getTokens());
                StringBuilder productionOutput = new StringBuilder();
                for (Production production : productions)
                    productionOutput.append(production.toString()).append("\n");
                parserProductions.setText(productionOutput.toString());
                parserProductions.setEditable(false);

                parserErrorArea.setStyle(errorCSS);
                StringBuilder parserError = new StringBuilder();
                for (String error : parser.getErrorMessages())
                    parserError.append(error).append("\n");
                parserErrorArea.setText(parserError.toString());
                parserErrorArea.setEditable(false);
            } else if (semanticTab.isSelected()){
                lexerReadSourceCode();
                PrintStream origin = System.out;
                PrintStream printStream = new PrintStream(new FileOutputStream("src/parser/interCode.txt"));
                System.setOut(printStream);
                parser.reduce(lexer.getTokens());
                System.setOut(origin);
                interCodeArea.setText(readFromFile("src/parser/interCode.txt"));
                interCodeArea.setEditable(false);

                symbolTable.setEditable(false);
                symbolTable.setText(parser.getTable().toString());

                semanticErrorArea.setStyle(errorCSS);
                StringBuilder parserError = new StringBuilder();
                for (String error : parser.getErrorMessages())
                    parserError.append(error).append("\n");
                semanticErrorArea.setText(parserError.toString());
                semanticErrorArea.setEditable(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            fileNotExistAlert(sourcePath);
        }
    }
}
