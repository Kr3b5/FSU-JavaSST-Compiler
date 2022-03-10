package ClassFile;

import AbstractSyntaxTree.AST;
import AbstractSyntaxTree.ASTClass;
import AbstractSyntaxTree.ASTNode;
import AbstractSyntaxTree.ASTNodeContainer;
import ClassData.*;
import Data.ObjClass;
import Data.STObject;
import Data.STType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

// https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.4
public class CPGenerator {

    /**
     * The logger for this class.
     */
    private static final Logger logger = LogManager.getLogger(CPGenerator.class.getName());


    private short countConstantPool;
    private final HashMap<Short, CPConstant> constantPool;
    private final LinkedList<Field> fields;
    private final LinkedList<Method> methods;

    private final AST ast;

    private final List<String> called;

    private short classIndex;
    private short superclassIndex;
    private short sourcefileIndex;
    private short codeIndex;
    private short lntIndex;

    private final ByteBuffer codeBuffer = ByteBuffer.allocate(65536);
    private int cur;

    List<Short> field_ref;

    //debug
    boolean debugMode;

    public CPGenerator(AST ast) {
        this.countConstantPool = 1;
        this.constantPool = new HashMap<>();
        this.fields = new LinkedList<>();
        this.methods = new LinkedList<>();
        this.ast = ast;
        this.called = new LinkedList<>();
        debugMode = false;
    }

    //SETTER + GETTER
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public LinkedList<Field> getFields() { return fields; }
    public LinkedList<Method> getMethods() { return methods; }
    public short getClassIndex() { return classIndex; }
    public short getSuperclassIndex() { return superclassIndex; }
    public short getSourcefileIndex() { return sourcefileIndex; }
    public HashMap<Short, CPConstant> getConstantPool() { return constantPool;}
    // METHODS

    public void generate(){
        genConstantPool();
        genCode();
    }



    public void genConstantPool() {
        genPoolHead();
        genPoolClass();
        genPoolFinals();
        genPoolCalls();
        genPoolConstants();
        genNotCalledVars();
        genPoolCodeHead();
        genPoolCodeBody();
        genPoolEnd();

        if(debugMode) printConstantPool();
    }

    /*
     #1 = Methodref          #2.#3          // java/lang/Object."<init>":()V
      #2 = Class              #4             // java/lang/Object
      #3 = NameAndType        #5:#6          // "<init>":()V
      #4 = Utf8               java/lang/Object
      #5 = Utf8               <init>
      #6 = Utf8               ()V
    */
    private void genPoolHead() {
        addToPool(new CPConstant((byte) CPTypes.METHOD.value, (short) 2, (short) 3));
        superclassIndex = countConstantPool;
        addToPool(new CPConstant((byte) CPTypes.CLASS.value, (short) 4));
        addToPool(new CPConstant((byte) CPTypes.NAMEANDTYPE.value, (short) 5, (short) 6));
        String obj = "java/lang/Object";
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) obj.length(), obj));
        String init = "<init>";
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) init.length(), init));
        String v = "()V";
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) v.length(), v));
    }

    /*
      #7 = Class              #8             // EmptyClass
      #8 = Utf8               EmptyClass
    */
    private void genPoolClass() {
        classIndex = countConstantPool;
        addToPool(new CPConstant((byte) CPTypes.CLASS.value, (short) (countConstantPool + 1)));
        methods.add(new Method((short)0, countConstantPool, (short) 6, (short) 0, null));
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) ast.getObject().getName().length(), ast.getObject().getName()));
    }

    /* FINALS
        #7 = Fieldref           #8.#9          // ClassTest2.fvar1:I
        #9 = NameAndType        #11:#12        // fvar1:I
        #11 = Utf8               fvar1
        #12 = Utf8               I
     */
    private void genPoolFinals() {
        field_ref = new LinkedList<>();

        ASTNodeContainer finals = ast.getFinals();
        for (ASTNode node : finals.getNodes()) {
            short nameIndex = 0;
            STObject stobject = node.getObject();
            field_ref.add(countConstantPool);
            addToPool(new CPConstant((byte) CPTypes.FIELD.value, classIndex , (short) (countConstantPool + 1)));
            short key = getKeyByStringValue("I");
            if( key != 0){
                addToPool(new CPConstant((byte) CPTypes.NAMEANDTYPE.value, (short) (countConstantPool + 1), (short) key ));
                nameIndex = countConstantPool;
                addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) stobject.getName().length(), stobject.getName()));
            }else{
                addToPool(new CPConstant((byte) CPTypes.NAMEANDTYPE.value, (short) (countConstantPool + 1), (short) (countConstantPool + 2)));
                nameIndex = countConstantPool;
                addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) stobject.getName().length(), stobject.getName()));
                key = countConstantPool;
                addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short)1, "I"));
            }

            // add Fields to Fieldlist
            // ACC_FINAL 0x0010
            Field field = new Field((short)0x10, nameIndex, key, (short) 0, null);
            fields.add(field);
        }
    }

    /* METHOD Called!
        #16 = Methodref          #8.#17         // ClassTest2.meth2:(II)I
        #17 = NameAndType        #18:#19        // meth2:(II)I
        #18 = Utf8               meth2
        #19 = Utf8               (II)I
     */
    /* Globals
        #20 = Fieldref           #8.#21         // ClassTest2.dyn1:I
        #21 = NameAndType        #22:#12        // dyn1:I
        #22 = Utf8               dyn1
     */
    private void genPoolCalls() {
        ASTNodeContainer methodsList = ast.getMethods();
        for (ASTNode n : methodsList.getNodes()) {
            findNextCalledNode(n.getLink());
        }

        for (ASTNode n : methodsList.getNodes()) {
            if(called.contains(n.getObject().getName())){
                String pKey = getPKey(n);
                short key = getKeyByStringValue(pKey);
                short nameIndex = (short) (countConstantPool + 2);
                if( key != 0){
                    addToPool(new CPConstant((byte) CPTypes.METHOD.value, classIndex , (short) (countConstantPool + 1)));
                    addToPool(new CPConstant((byte) CPTypes.NAMEANDTYPE.value, (short) (countConstantPool + 1), key));
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) n.getObject().getName().length(), n.getObject().getName()));
                }else{
                    addToPool(new CPConstant((byte) CPTypes.METHOD.value, classIndex , (short) (countConstantPool + 1)));
                    addToPool(new CPConstant((byte) CPTypes.NAMEANDTYPE.value, (short) (countConstantPool + 1), (short) (countConstantPool + 2)));
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) n.getObject().getName().length(), n.getObject().getName()));
                    key = countConstantPool;
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) pKey.length(), pKey));
                }
                methods.add(new Method((short)1, nameIndex, key, (short) 0, null));
            }
        }

        ASTNodeContainer globals = ast.getVars();
        for (ASTNode n : globals.getNodes()) {
            if(called.contains(n.getObject().getName())){
                short key = getKeyByStringValue("I");
                short nameIndex = (short) (countConstantPool + 2);
                if( key != 0){
                    addToPool(new CPConstant((byte) CPTypes.FIELD.value, classIndex , (short) (countConstantPool + 1)));
                    addToPool(new CPConstant((byte) CPTypes.NAMEANDTYPE.value, (short) (countConstantPool + 1), key));
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) n.getObject().getName().length(), n.getObject().getName()));
                }else{
                    addToPool(new CPConstant((byte) CPTypes.FIELD.value, classIndex , (short) (countConstantPool + 1)));
                    addToPool(new CPConstant((byte) CPTypes.NAMEANDTYPE.value, (short) (countConstantPool + 1), (short) (countConstantPool + 2)));
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) n.getObject().getName().length(), n.getObject().getName()));
                    key = countConstantPool;
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short)1, "I"));
                }
                fields.add(new Field((short)0, nameIndex, key, (short) 0, null));
            }
        }
    }



    private void findNextCalledNode(ASTNode node){
        if(node.getNodeClass().equals(ASTClass.VAR) ||
                node.getNodeClass().equals(ASTClass.PROD)){
            if(!called.contains(node.getName())){
                called.add(node.getName());
            }
        }
        if(node.getLeft() != null){
            findNextCalledNode(node.getLeft());
        }
        if(node.getRight() != null){
            findNextCalledNode(node.getRight());
        }
        if(node.getLink() != null){
            findNextCalledNode(node.getLink());
        }
    }


    /*
         #30 = Utf8               ConstantValue
         #31 = Integer            1
     */
    private void genPoolConstants(){
        ASTNodeContainer finals = ast.getFinals();
        if( !finals.getNodes().isEmpty()){
            String c = "ConstantValue";
            short constantValueIndex = countConstantPool;
            int i = 0;
            addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) c.length(), c));
            for (ASTNode node : finals.getNodes()) {
                STObject stobject = node.getObject();
                Attribut constantValue = new Attribut(constantValueIndex, 2, countConstantPool);
                addToPool(new CPConstant((byte) CPTypes.INTEGER.value, stobject.getIntValue()));

                fields.get(i).setCountAttributes((short)1);
                List attributes = new LinkedList<Attribut>();
                attributes.add(constantValue);
                fields.get(i).setAttributes(attributes);
                i++;
            }
        }
    }

    private void genNotCalledVars(){
        ASTNodeContainer globals = ast.getVars();
        for (ASTNode n : globals.getNodes()) {
            if(!called.contains(n.getObject().getName())){
                // add Globals to Fieldlist
                Field field = new Field((short)0, countConstantPool, getKeyByStringValue("I"), (short) 0, null);
                fields.add(field);

                addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) n.getObject().getName().length(), n.getObject().getName()));
            }
        }
    }


    /*
       #9 = Utf8               Code
      #10 = Utf8               LineNumberTable
     */
    private void genPoolCodeHead() {
        String c = "Code";
        codeIndex = countConstantPool;
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) c.length(), c));
        String lnt = "LineNumberTable";
        lntIndex = countConstantPool;
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) lnt.length(), lnt));
        String smt = "StackMapTable";                                                               //TODO
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) smt.length(), smt));
    }

    private void genPoolCodeBody() {
        ASTNodeContainer methodList = ast.getMethods();
        for (ASTNode n : methodList.getNodes()) {
            if(!called.contains(n.getObject().getName())){
                String pKey = getPKey(n);
                short key = getKeyByStringValue(pKey);
                short nameIndex = countConstantPool;
                if( key != 0){
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) n.getObject().getName().length(), n.getObject().getName()));
                }else{
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) n.getObject().getName().length(), n.getObject().getName()));
                    key = countConstantPool;
                    addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) pKey.length(), pKey));
                }
                methods.add(new Method((short)1, nameIndex, key, (short) 0, null));
            }
        }
    }

    /*
      #11 = Utf8               SourceFile
      #12 = Utf8               EmptyClass.java
     */
    private void genPoolEnd() {
        String sf = "SourceFile";
        String name = ast.getObject().getName() + ".java";
        sourcefileIndex = countConstantPool;
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) sf.length(), sf));
        addToPool(new CPConstant((byte) CPTypes.UTF8.value, (short) name.length(), name));
    }


    // HELPER

    private void addToPool(CPConstant c) {
        constantPool.put(countConstantPool, c);
        countConstantPool++;
    }



    private Short getKeyByStringValue(String value) {
        for (Map.Entry<Short,CPConstant> entry : constantPool.entrySet()) {
            if (entry.getValue().getsValue() != null && value.equals(entry.getValue().getsValue())) {
                return entry.getKey();
            }
        }
        return 0;
    }

    private Short getKeyByIntValue(int value) {
        for (Map.Entry<Short,CPConstant> entry : constantPool.entrySet()) {
            if (value == entry.getValue().getiValue()) {
                return entry.getKey();
            }
        }
        return 0;
    }


    private String getPKey(ASTNode n){
        int cInts = 0;
        for (STObject obj : n.getObject().getSymtab().getObjects()) {
            if (obj.getObjClass().equals(ObjClass.PAR)) {
                cInts++;
            }
        }
        //parameter key
        StringBuilder sb = new StringBuilder("(");
        for(int i = 0; i < cInts; i++ ){
            sb.append("I");
        }
        sb.append(")");
        if(n.getObject().getSTType().equals(STType.INT)){
            sb.append("I");
        }else{
            sb.append("V");
        }
        return sb.toString();
    }

    // DEBUG
    
    private void printConstantPool(){
        logger.info("Constant Pool");
        int count = 1;
        for (CPConstant c: constantPool.values()) {
            byte type = c.getType();
            String prestring = "   #" + count;
            if(constantPool.size() >= 10 && count < 10){
                prestring = "    #" + count;
            }
            if((byte) CPTypes.CLASS.value == type){
                logger.info(prestring +
                            " = Class \t\t\t#" + c.getBytefield1());
            }else if((byte) CPTypes.UTF8.value == type){
                logger.info(prestring +
                            " = UTF8 \t\t\t" + c.getsValue());
            }else if((byte) CPTypes.FIELD.value == type){
                logger.info(prestring +
                             " = Fieldref \t\t#" + c.getBytefield1() + ".#" + c.getBytefield2());
            }else if((byte) CPTypes.INTEGER.value == type){
                logger.info(prestring +
                            " = Integer \t\t\t" + c.getiValue());
            }else if((byte) CPTypes.METHOD.value == type){
                logger.info(prestring +
                            " = Methodref \t\t#" + c.getBytefield1() + ".#" + c.getBytefield2());
            }else if((byte) CPTypes.NAMEANDTYPE.value == type){
                logger.info(prestring +
                            " = NameAndType \t\t#" + c.getBytefield1() + ":#" + c.getBytefield2());
            }else{
                logger.error("Unknown Type");
            }
            count++;
        }
    }

    // CODEGEN


    public void genCode(){
        genClassCode();
    }


    private void genClassCode(){
        codeBuffer.clear();
        cur = 0;

        List<Attribut> attCode = new LinkedList<>();
        List<Attribut> attInfo = new LinkedList<>();
        List<Attribut> attLnt = new LinkedList<>();

        //GENHEAD
        insertByte(InsSet.ALOAD_0.bytes);
        insertByte(InsSet.INVOKESPECIAL.bytes);
        insertShort((short)1);

        //GENCODE
        int i = 0;
        ASTNodeContainer finals = ast.getFinals();
        for (ASTNode node : finals.getNodes()) {
            STObject stobject = node.getObject();

            insertByte(InsSet.ALOAD_0.bytes);

            byte cons = getConst(stobject.getIntValue());
            if( cons == InsSet.BIPUSH.bytes ){
                insertByte(cons);
                insertByte((byte)stobject.getIntValue());
            }else{
                insertByte(cons);
            }

            insertByte(InsSet.PUTFIELD.bytes);
            insertShort(field_ref.get(i));
            i++;
        }

        //GENEND
        insertByte(InsSet.RETURN.bytes);

        byte[] code = new byte[cur];
        codeBuffer.get(0, code, 0, code.length);

        //TODO line_number_table_length 6 + 1
        //attLnt.add(new Attribut((short) 0,(short) 1));
        //attInfo.add(new Attribut(lntIndex, (short)6, (short)1, attLnt));

        short size = (short)(12 + cur + (attInfo.size()*8) + (attLnt.size()*4));
        Attribut classCode = new Attribut(codeIndex, size, (short)2, (short)1, cur, code, (short)attInfo.size(), attInfo);
        attCode.add(classCode);

        methods.get(0).setCountAttributes((short)1);
        methods.get(0).setAttributes(attCode);
    }




    private void genMethodCode(){

    }



    private byte getConst(int z){
        return switch (z) {
            case 0 -> InsSet.ICONST_0.bytes;
            case 1 -> InsSet.ICONST_1.bytes;
            case 2 -> InsSet.ICONST_2.bytes;
            case 3 -> InsSet.ICONST_3.bytes;
            case 4 -> InsSet.ICONST_4.bytes;
            case 5 -> InsSet.ICONST_5.bytes;
            default -> InsSet.BIPUSH.bytes;
        };
    }



    void insertShort(short cp) {
        codeBuffer.putShort(cp);
        cur = cur + 2;
    }

    void insertInt(int cp) {
        codeBuffer.putInt(cp);
        cur = cur + 4;
    }

    void insertByte(byte cp) {
        codeBuffer.put(cp);
        cur++;
    }

}
