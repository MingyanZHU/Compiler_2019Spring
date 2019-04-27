package symbols;

import lexer.Tag;

public class ProcSymbolItem extends SymbolItem {
    private final SymbolBoard procSymbolTable;
    private final int paramsSize;

    public ProcSymbolItem(String identifier, int position, int offset, SymbolBoard table, int paramsSize) {
        super(identifier, Tag.PROC.getValue(), position, offset);
        this.procSymbolTable = table;
        this.paramsSize = paramsSize;
    }

    public SymbolBoard getProcSymbolTable() {
        return procSymbolTable;
    }

    public int getParamsSize() {
        return paramsSize;
    }
}
