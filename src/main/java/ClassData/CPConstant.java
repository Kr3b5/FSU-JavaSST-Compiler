package ClassData;

/**
 * Constant pool entry
 *
 * https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.4.2
 *
 * @author Kr3b5
 */
public class CPConstant {
    byte type;
    short bytefield1;
    short bytefield2;

    String sValue;
    int iValue;

    //Method, Name, NaT
    public CPConstant(byte type, short bytefield1, short bytefield2) {
        this.type = type;
        this.bytefield1 = bytefield1;
        this.bytefield2 = bytefield2;
        iValue = -1;
    }

    //UTF-8
    public CPConstant(byte type, short bytefield1, String sValue) {
        this.type = type;
        this.bytefield1 = bytefield1;
        this.sValue = sValue;
        iValue = -1;
    }

    //Integer
    public CPConstant(byte type, int iValue) {
        this.type = type;
        this.iValue = iValue;
    }

    //Class
    public CPConstant(byte type, short bytefield1) {
        this.type = type;
        this.bytefield1 = bytefield1;
        iValue = -1;
    }

    public byte getType() {
        return type;
    }

    public short getBytefield1() {
        return bytefield1;
    }

    public short getBytefield2() {
        return bytefield2;
    }

    public String getsValue() {
        return sValue;
    }

    public int getiValue() {
        return iValue;
    }
}
