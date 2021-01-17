package generator;

import analyser.Analyser;
import analyser.IdentType;
import analyser.SymbolEntry;
import analyser.SymbolTable;
import instruction.Instruction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Generator {
    DataOutputStream output;
    Analyser analyser;
    int magic=0x72303b3e;
    int version=1;
    public Generator(DataOutputStream output, Analyser analyser){
        this.output=output;
        this.analyser=analyser;
    }
    public void generateo0() throws IOException {
        output.writeInt(this.magic);
        output.writeInt(this.version);
        generateGlobals();
        generateFn();
    }
    private void generateGlobals() throws IOException {
        int count=analyser.globalTable.getCount();
        output.writeInt(count);
        LinkedHashMap<String, SymbolEntry> table=analyser.globalTable.getSymbolTable();
        for(Map.Entry<String, SymbolEntry> entry : table.entrySet()) {
            output.writeBoolean(entry.getValue().isConstant());
            if(entry.getValue().getType()==IdentType.INT||entry.getValue().getType()==IdentType.DOUBLE){
                output.writeInt(8);
                output.writeLong((Long) entry.getValue().getValue());
            }
            else if(entry.getValue().getType()==IdentType.STRING){
                output.writeInt(((String)entry.getValue().getValue()).length());
                output.write(((String) entry.getValue().getValue()).getBytes());
            }
        }

    }

    private void generateFn() throws IOException{
        int count=analyser._startTable.getCount();
        output.writeInt(count);
        LinkedHashMap<String, SymbolEntry> table=analyser._startTable.getSymbolTable();
        for(Map.Entry<String, SymbolEntry> entry : table.entrySet()) {
            System.out.print("fn "+entry.getValue().getName()+" "+entry.getValue().getType());
            output.writeInt((int) entry.getValue().getStackOffset());
            if(entry.getValue().getType() == IdentType.VOID){
                output.writeInt(0);
            }
            else if(entry.getValue().getType() ==IdentType.INT||entry.getValue().getType() ==IdentType.DOUBLE){
                output.writeInt(1);
            }
            if(entry.getValue().getParam() == null){
                output.writeInt(0);
                System.out.print(" "+0);
            }
            else{
                output.writeInt(entry.getValue().getParam().getCount());
                System.out.print(" "+entry.getValue().getParam().getCount());
            }
            if(entry.getValue().getLoc()==null){
                output.writeInt(0);
                System.out.println(" "+0);
            }else{
                SymbolTable loc=entry.getValue().getLoc();
                output.writeInt(loc.getCount());
                System.out.println(" "+loc.getCount());
            }
            ArrayList<Instruction> instructions=entry.getValue().getInstruction();
            output.writeInt(instructions.size());
            generateInstruction(instructions);
        }
    }

    private void generateInstruction(ArrayList<Instruction> instructions) throws IOException {
        for(int i=0;i<instructions.size();i++){
            output.write(instructions.get(i).toByte());
            System.out.println(i+":"+instructions.get(i).toString());
        }
    }
}