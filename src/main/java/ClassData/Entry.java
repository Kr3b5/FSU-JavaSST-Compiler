package ClassData;

import java.util.List;

public abstract class Entry {

    private short accessFlag;
    private short nameIndex;
    private short signatureIndex;
    private short countAttributes;
    private List<Attribut> attributes;

    public Entry(short accessFlag, short nameIndex, short signatureIndex, short countAttributes, List<Attribut> attributes) {
        this.accessFlag = accessFlag;
        this.nameIndex = nameIndex;
        this.signatureIndex = signatureIndex;
        this.countAttributes = countAttributes;
        this.attributes = attributes;
    }

    public short getAccessFlag() {
        return accessFlag;
    }

    public short getNameIndex() {
        return nameIndex;
    }

    public short getSignatureIndex() {
        return signatureIndex;
    }

    public short getCountAttributes() {
        return countAttributes;
    }

    public List<Attribut> getAttributes() {
        return attributes;
    }
}
