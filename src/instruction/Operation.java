package instruction;

public enum Operation {
    nop,pop,stroe64,load64,addi, subi,muli,divi,ret,cmpi,dup,
    push,globa,loca, arga,br,brfalse,brtrue, call,	setlt,setgt,
    callname,stackalloc,load8,divf,mulf,subf,addf,cmpf,itof,ftoi,
    negi,negf;

    public byte toByte(){
        switch (this) {
            case setgt:
                return (byte)0x3a;
            case setlt:
                return (byte)0x39;
            case nop:
                return (byte)0x00;
            case pop:
                return (byte)0x02;
            case stroe64:
                return (byte)0x17;
            case load64:
                return (byte)0x13;
            case addi:
                return (byte)0x20;
            case subi:
                return (byte)0x21;
            case muli:
                return (byte)0x22;
            case divi:
                return (byte)0x23;
            case ret:
                return (byte)0x49;
            case cmpi:
                return (byte)0x30;
            case dup:
                return (byte)0x04;
            case push:
                return (byte)0x01;
            case globa:
                return (byte)0x0c;
            case loca:
                return (byte)0x0a;
            case br:
                return (byte)0x41;
            case arga:
                return (byte)0x0b;
            case call:
                return (byte)0x48;
            case brtrue:
                return (byte)0x43;
            case brfalse:
                return (byte)0x42;
            case callname:
                return (byte)0x4a;
            case stackalloc:
                return (byte)0x1a;
            case load8:
                return (byte)0x10;
            case addf:
                return (byte)0x24;
            case subf:
                return (byte)0x25;
            case mulf:
                return (byte)0x26;
            case divf:
                return (byte)0x27;
            case cmpf:
                return (byte)0x32;
            case ftoi:
                return (byte)0x37;
            case itof:
                return (byte)0x36;
            case negf:
                return (byte)0x35;
            case negi:
                return (byte)0x34;
            default:
                return (byte)0xfe;//panic
        }
    }
}
