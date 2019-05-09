package parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class InterCode {
    private final List<String> interCode = new ArrayList<>();

    public InterCode(String[] interCode) {
        Collections.addAll(this.interCode, interCode);
    }

    public void backPatch(String back) {
        interCode.add(back);
    }

    public List<String> getInterCode() {
        return new ArrayList<>(interCode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
//        sb.append("(");
        for (int i = 0; i < interCode.size(); i++) {
            if (i != 0)
                sb.append(" ");
            sb.append(interCode.get(i));
        }
//        sb.append(")");
        return sb.toString();
    }
}
