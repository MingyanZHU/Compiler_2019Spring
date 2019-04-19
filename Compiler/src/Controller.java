import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import lexer.Lexer;

import java.io.*;

public class Controller {
    public TextArea tokenArea = new TextArea();
    public TextArea lexerErrorArea = new TextArea();
    public Tab sourceTab = new Tab();
    public TextArea sourceCode = new TextArea();
    public Tab lexerTab = new Tab();
    public Tab parserTab = new Tab();

    public String sourcePath = "src/lexer/program/test.c";
    private Lexer lexer;

    public void tabChanged() {
        try {
            if (sourceTab.isSelected()) {
                StringBuilder code = new StringBuilder();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(sourcePath)));
                int s;
                while ((s = bufferedReader.read()) != -1) {
                    code.append((char) s);
                }
                bufferedReader.close();
                sourceCode.setText(code.toString());
            } else if (lexerTab.isSelected()) {
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(sourcePath)));
                bufferedWriter.write(sourceCode.getText());
                bufferedWriter.close();

                lexer = new Lexer(sourcePath);
                lexer.scan();
                lexerErrorArea.setStyle("-fx-font-family: \"Noto Sans CJK SC Bold\";" +
                        "-fx-text-fill: #ff4a1c;");

                tokenArea.setEditable(false);
                tokenArea.setText(lexer.getTokensList());

                lexerErrorArea.setEditable(false);
                lexerErrorArea.setText(lexer.getErrors());
            }
        } catch (FileNotFoundException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("源文件不存在");
            alert.setContentText("src/lexer/program/test.c 文件不存在");
            alert.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
