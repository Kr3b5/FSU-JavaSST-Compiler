package ClassFile;

import AbstractSyntaxTree.AST;
import ClassData.*;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * Writer for class file
 * https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html
 *
 * @author Kr3b5
 */
@SuppressWarnings("FieldCanBeLocal")
public class ClassWriter {

    /**
     * The logger for this class.
     */
    private static final Logger logger = LogManager.getLogger(ClassWriter.class.getName());

    // classfile
    private final int magicnumber = 0xCAFEBABE;
    private final short major = 52;                     // Java 8 = 52 (0x34)| Java 15 = 59 (0x3B)
    private final short minor = 0;

    private short this_class;
    private short super_class;

    private HashMap<Short, CPConstant> constantPool;

    private final short accessflags = 0x0001; // Public

    private final short countInterfaces = 0;   // Interfaces not allowed
    // interface table not used

    private List<Field> fields;

    private List<Method> methods;



    // globals
    private final AST ast;

    private byte[] code;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos);
    private short sourcefile;

    //debug
    boolean debugMode;

    //constructor
    public ClassWriter(AST ast) {
        this.ast = ast;
        constantPool = new HashMap<>();
        fields = new LinkedList<>();
        methods = new LinkedList<>();
        debugMode = false;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }


    /**
     * generate .class file
     */
    public void genClass() {

        //generate ConstantPool
        ClassGenerator classGenerator =  new ClassGenerator(ast);
        classGenerator.setDebugMode(debugMode);
        classGenerator.generate();
        getCPValues(classGenerator);

        code = genByteCode();

        if(debugMode)printByteCode();

        writeByteCodeToFile();
    }

    /**
     * get parameter from generator
     * @param classGenerator generator
     */
    private void getCPValues(ClassGenerator classGenerator) {
        constantPool = classGenerator.getConstantPool();
        this_class = classGenerator.getClassIndex();
        super_class = classGenerator.getSuperclassIndex();
        sourcefile = classGenerator.getSourcefileIndex();
        fields = classGenerator.getFields();
        methods = classGenerator.getMethods();
    }

    /**
     * write bytecode to file
     */
    private void writeByteCodeToFile() {
        String filename = ast.getObject().getName() + ".class";
        try {
            FileUtils.writeByteArrayToFile(new File(filename), code);
            logger.info("Bytecode written to file: "+ filename );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * generate bytecode
     * @return code
     */
    private byte[] genByteCode() {
        //u4 - add magic number
        insertInt(magicnumber);

        //u2 - minor
        insertShort(minor);
        //u2 - major
        insertShort(major);

        //u2 - constant_pool_count +  constant_pool
        insertShort((short)(constantPool.size() + 1));
        genByteCodeFromCP();

        //u2 - access_flags;
        insertShort(accessflags);
        //u2 - this_class;
        insertShort(this_class);
        //u2 - super_class;
        insertShort(super_class);

        //u2 - interfaces count
        insertShort(countInterfaces);

        //Fields
        insertShort((short)fields.size());
        insertFields();

        //Methods
        insertShort((short)methods.size());
        insertMethods();

        //Attributes
        insertAttributes();

        logger.info("Bytecode generated!");

        return baos.toByteArray();
    }

    /**
     * generate bytecode from Constantpool
     */
    private void genByteCodeFromCP(){
        for (CPConstant c: constantPool.values()) {
            byte type = c.getType();
            /*
                u1 tag
                u2 name_index
             */
            if((byte) CPTypes.CLASS.value == type){
                insertByte(c.getType());
                insertShort(c.getBytefield1());
            }
            /*
                u1 tag
                u2 length
                u[length] value
             */
            else if((byte) CPTypes.UTF8.value == type){
                insertByte(c.getType());
                insertShort(c.getBytefield1());
                insertString(c.getsValue());
            }
            /*
                u1 tag
                u2 class_index | name_index
                u2 name_and_type_index | descriptor_index
             */
            else if((byte) CPTypes.FIELD.value == type ||
                     (byte) CPTypes.METHOD.value == type ||
                     (byte) CPTypes.NAMEANDTYPE.value == type ){
                insertByte(c.getType());
                insertShort(c.getBytefield1());
                insertShort(c.getBytefield2());
            }
            /*
                u1 tag
                u4 bytes
             */
            else if((byte) CPTypes.INTEGER.value == type){
                insertByte(c.getType());
                insertInt(c.getiValue());
            }else{
                logger.error("Error - Type not found!");
            }
        }
    }

    /**
     * insert fields into code
     *
     * https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.5
     *      u2             access_flags;
     *      u2             name_index;
     *      u2             descriptor_index;
     *      u2             attributes_count;
     *      attribute_info attributes[attributes_count];
     */
    private void insertFields() {
        for (Field f : fields) {
            insertShort(f.getAccessFlag());
            insertShort(f.getNameIndex());
            insertShort(f.getSignatureIndex());
            insertShort(f.getCountAttributes());
            if(f.getAttributes() != null){
                for (Attribut a : f.getAttributes()) {
                    insertShort(a.getNameIndex());
                    insertInt(a.getLength());
                    insertShort(a.getIndex());
                }
            }
        }
    }

    /**
     * insert methods into code
     *
     * https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.6
     *      u2             access_flags;
     *      u2             name_index;
     *      u2             descriptor_index;
     *      u2             attributes_count;
     *      attribute_info attributes[attributes_count];
     */
    private void insertMethods() {
        for (Method m : methods) {
            insertShort(m.getAccessFlag());
            insertShort(m.getNameIndex());
            insertShort(m.getSignatureIndex());
            insertShort(m.getCountAttributes());

            if(m.getAttributes() != null ){
                /* https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.7.3
                    u2 attribute_name_index;
                    u4 attribute_length;
                    u2 max_stack;
                    u2 max_locals;
                    u4 code_length;
                    u1 code[code_length];
                    u2 exception_table_length;
                    u2 attributes_count;
                    attribute_info attributes[attributes_count];
                */
                for (Attribut codeAtt : m.getAttributes()) {
                    insertShort(codeAtt.getNameIndex());
                    insertInt(codeAtt.getLength());
                    insertShort(codeAtt.getStackSize());
                    insertShort(codeAtt.getCountLocalVars());
                    insertInt(codeAtt.getCodeLength());
                    insertByteArray(codeAtt.getCode());
                    insertShort(codeAtt.getCountExceptionTable());
                    insertShort(codeAtt.getCountAttributes());

                    if(codeAtt.getAttributes() != null ){
                        /* https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.7.12
                            u2 attribute_name_index;
                            u4 attribute_length;
                            u2 line_number_table_length;
                            {   u2 start_pc;
                                u2 line_number;
                            } line_number_table[line_number_table_length];
                        */
                        for (Attribut attribut: codeAtt.getAttributes()) {
                            insertShort(attribut.getNameIndex());
                            insertInt(attribut.getLength());
                            insertShort(attribut.getCountAttributes());

                            if(attribut.getAttributes() != null ) {
                                /* https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.7.12
                                    u2 start_pc;
                                    u2 line_number;
                                */
                                for (Attribut lnt : attribut.getAttributes()) {
                                    insertShort(lnt.getStart_pc());
                                    insertShort(lnt.getLine_number());
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    /**
     * insert attributes into code
     *
     * https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.7
     *      u2 attributes_count
     *      u2 attribute_name_index
     *      u4 attribute_length
     *      u2 sourcefile
     */
    private void insertAttributes() {
        insertShort((short) 1);
        insertShort(sourcefile);
        insertInt(2);
        insertShort((short)(sourcefile + 1));
    }


    //-----------------------------------------------------------------------------------------------------------------

    /**
     * insert int into code
     * @param cp codepart
     */
    void insertInt(int cp) {
        try {
            dos.writeInt(cp);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * insert short into code
     * @param cp codepart
     */
    void insertShort(short cp) {
        try {
            dos.writeShort(cp);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * insert byte into code
     * @param cp codepart
     */
    void insertByte(byte cp) {
        try {
            dos.writeByte(cp);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * insert bytearray into code
     * @param cp codepart array
     */
    void insertByteArray(byte[] cp) {
        try {
            dos.write(cp);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * insert string into code
     * @param s codepart
     */
    void insertString(String s) {
        try {
            dos.write(s.getBytes());
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }


    //-----------------------------------------------------------------------------------------------------------------

    /**
     * Debug - print code
     */
    private void printByteCode() {
        int i = 0;
        for (byte b : code) {
            if(i == 16){
                i = 0;
                System.out.print('\n');
            }
            System.out.format("%x ", b);
            i++;
        }
        System.out.print('\n');
    }

}
