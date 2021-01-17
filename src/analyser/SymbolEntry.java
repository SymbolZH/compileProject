package analyser;

import instruction.Instruction;

import java.util.ArrayList;

public class SymbolEntry {
    boolean isInitialized;
    long stackOffset;
    SymbolKind kind;
    IdentType type;
    Object value;
    SymbolTable param;
    int paramnum;
    ArrayList<Instruction> instruction;
    String name;
    SymbolTable loc;
    boolean isparam=false;
    public SymbolEntry(int stackOffset){
        this.name =null;
        this.isInitialized = false;
        this.stackOffset = stackOffset;
        this.kind = null;
        this.type = null;
        this.value = null;
        this.param = null;
        this.paramnum=0;
        this.instruction = null;
        this.loc =null;
    }
    public SymbolEntry(String name,boolean isDeclared, int stackOffset, SymbolKind kind, IdentType type, Object value, SymbolTable param, int paramnum,ArrayList<Instruction>  instruction,SymbolTable loc) {
        this.name =name;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.kind = kind;
        this.type = type;
        this.value = value;
        this.param = param;
        this.paramnum=paramnum;
        this.instruction = instruction;
        this.loc = loc;
    }
    public SymbolEntry(String name,boolean isDeclared, int stackOffset, SymbolKind kind, IdentType type, Object value) {
        this.name =name;
        this.isInitialized = isDeclared;
        this.stackOffset = stackOffset;
        this.kind = kind;
        this.type = type;
        this.value = value;
        this.param = null;
        this.paramnum=0;
        this.instruction = null;
        this.loc = null;
    }
    public boolean isConstant(){
        if(this.kind ==SymbolKind.CONST){
            return true;
        }
        else{
            return false;
        }
    }
    /**
     * @return the stackOffset
     */
    public long getStackOffset() {
        return stackOffset;
    }


    /**
     * @return the isInitialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * @param isInitialized the isInitialized to set
     */
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(long stackOffset) {
        this.stackOffset = stackOffset;
    }

    public void setKind(SymbolKind kind) {
        this.kind = kind;
    }

    public void setType(IdentType type) {
        this.type = type;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setParam(SymbolTable param) {
        this.param = param;
    }

    public void setInstruction(ArrayList<Instruction> instruction) {
        this.instruction = instruction;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public IdentType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public SymbolTable getParam() {
        return param;
    }

    public ArrayList<Instruction> getInstruction() {
        return instruction;
    }

    public String getName() {
        return name;
    }

    public SymbolTable getLoc() {
        return loc;
    }

    public void setLoc(SymbolTable loc) {
        this.loc = loc;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isIsparam() {
        return isparam;
    }

    public void setIsparam(boolean isparam) {
        this.isparam = isparam;
    }
}
