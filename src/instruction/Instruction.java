package instruction;

import java.nio.ByteBuffer;
import java.util.Objects;

public class Instruction {
    private Operation opt;
    long x;

    public Instruction(Operation opt) {
        this.opt = opt;
        this.x = 0;
    }

    public Instruction(Operation opt, Long x) {
        this.opt = opt;
        this.x = x;
    }

    public Instruction() {
        this.opt = Operation.nop;
        this.x = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Instruction that = (Instruction) o;
        return opt == that.opt && Objects.equals(x, that.x);
    }

    @Override
    public int hashCode() {
        return Objects.hash(opt, x);
    }

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public long getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }
//    nop,pop,stroe8,load8,addi, subi,muli,divi,ret,cmpi,
//    push,globa,loca, arga,br,brfalse,brtrue, call,
    public byte[] toByte() {

        switch (this.opt) {
            case nop:
            case pop:
            case stroe64:
            case load64:
            case addi:
            case subi:
            case muli:
            case divi:
            case ret:
            case cmpi:
            case dup:
            case setlt:
            case setgt:
            case load8:
            case addf:
            case subf:
            case mulf:
            case divf:
            case cmpf:
            case ftoi:
            case itof:
            case negi:
            case negf:
                byte[] bytes=new byte[1];
                bytes[0]=this.opt.toByte();
                return bytes;
            case push:
                ByteBuffer byteBuffer=ByteBuffer.allocate(9);
                byteBuffer.put((byte) 0x01);
                byteBuffer.putLong(x);
                return byteBuffer.array();
            case globa:
            case loca:
            case br:
            case arga:
            case call:
            case brtrue:
            case brfalse:
            case callname:
            case stackalloc:
                byteBuffer = ByteBuffer.allocate(5);
                byteBuffer.put(this.opt.toByte());
                byteBuffer.putInt((int)x);
                return byteBuffer.array();
            default:
                return new byte[]{(byte)0xfe};
        }
    }

    @Override
    public String toString() {

        switch (this.opt) {
            case nop:
            case pop:
            case stroe64:
            case load64:
            case addi:
            case subi:
            case muli:
            case divi:
            case ret:
            case cmpi:
            case dup:
            case setlt:
            case setgt:
            case load8:
            case itof:
            case ftoi:
            case cmpf:
            case divf:
            case mulf:
            case subf:
            case addf:
            case negi:
            case negf:
                return String.format("%s",this.opt);
            case push:

            case globa:
            case loca:
            case br:
            case arga:
            case call:
            case brtrue:
            case brfalse:
            case callname:
            case stackalloc:
                return String.format("%s %s",this.opt,this.x);
            default:
                return "";
        }
    }
}
