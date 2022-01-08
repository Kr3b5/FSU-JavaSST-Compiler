package Data;

public class STObject {

    private final String name;
    private final ObjClass objClass;
    private STType STType;

    private int intValue;


    private STObject parameterList;
    private STObject varList;

    private STObject varDef;
    private STObject methodDef;

    private SymbolTable symtab;

    //CLASS
    public STObject(String name, ObjClass objClass, SymbolTable symtab) {
        this.name = name;
        this.objClass = objClass;
        this.symtab = symtab;
    }

    // VAR & PAR
    public STObject(String name, ObjClass objClass, STType STType) {
        this.name = name;
        this.objClass = objClass;
        this.STType = STType;
    }

    // CONS
    public STObject(String name, ObjClass objClass, STType STType, int intValue) {
        this.name = name;
        this.objClass = objClass;
        this.STType = STType;
        this.intValue = intValue;
    }

    // METHOD
    public STObject(String name, ObjClass objClass, STType resultType, SymbolTable symtab) {
        this.name = name;
        this.objClass = objClass;
        this.STType = resultType;
        this.symtab = symtab;
    }


    public String getName() {
        return name;
    }

    public SymbolTable getSymtab() {
        return symtab;
    }

    @Override
    public String toString() {
        return "STObject{" +
                "name='" + name + '\'' +
                ", objClass=" + objClass +
                ", type=" + STType +
                ", intValue=" + intValue +
                ", symtab=" + symtab +
                '}';
    }
}
