package Helper;

import AbstractSyntaxTree.AST;
import AbstractSyntaxTree.ASTClass;
import AbstractSyntaxTree.ASTNode;
import AbstractSyntaxTree.ASTNodeContainer;
import Data.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;



/**
 * Semantic Analyzer
 *
 * Tests:
 *      - Var is init
 *      - INT methods end with return (IF at end -> return at IF and ELSE branch)
 *      - new assignment to FINAL (not allowed)
 *      - WHILE + IF need BOOL condition
 *      - VAR has Values (nullcheck)
 *
 *      - init in IF-ELSE-WHILE -> not possible in JavaSST
 *      - Deadcode -> Parser ignore this
 *
 * @author Kr3b5
 */
public class SemanticAnalyzer {

    /**
     * The logger for this class.
     */
    private static final Logger logger = LogManager.getLogger(SemanticAnalyzer.class.getName());

    //globals
    private boolean errorFound = false;
    private ASTNode bufferNode;
    private String actualCheck;
    private LinkedList<STObject> checkList;

    // DEBUG Mode
    private boolean debugMode;

    /**
     * set debug mode
     * @param debugMode true/false
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * analyze code
     * @param ast AST
     * @param st symboltable
     * @return error found = true/false
     */
    public boolean analyze(AST ast , SymbolTable st) {
        ASTNodeContainer methods = ast.getMethods();
        for (ASTNode method : methods.getNodes()) {
            //Debug
            logMethod(method.getObject().getName());

            // get all initialized vars
            getInitVars(st.getObjects().get(0).getSymtab(), method.getObject());

            checkMethodReturn(method);
            checkallNodes(method);

            //get init values
            actualCheck = "VarHasValue";
            List<String> list = getInitValues(st.getObjects().get(0).getSymtab(), method.getObject());
            checkVarValue(method, list);
        }
        logSummary();
        return errorFound;
    }


    //-------------------------------------------------------------------------------------------------------
    /**
     * INT methods must end with return
     * @param method node
     */
    private void checkMethodReturn(ASTNode method) {
        actualCheck = "checkMethodReturn";
        if (method.getObject().getSTType().equals(STType.INT)) {
            logInfo("Check INT method for return: " + method.getObject().getName());
            getLastLinkNode(method.getLink());
            // if last node = IF_ELSE then both trees must end with RETURN node
            if (bufferNode.getNodeClass().equals(ASTClass.IF_ELSE)) {
                logInfo("IF_ELSE node found");
                ASTNode ifelseNode = bufferNode;

                // IF Node
                getLastLinkNode(ifelseNode.getLeft().getRight());
                if (bufferNode.getNodeClass().equals(ASTClass.RETURN)) {
                    logInfo("IF node - RETURN node found");
                } else {
                    logError("IF node - RETURN node not found [" + "Last node: " + bufferNode.getNodeClass() + "]");
                    errorFound = true;
                }

                // ELSE Node
                getLastLinkNode(ifelseNode.getRight());
                if (bufferNode.getNodeClass().equals(ASTClass.RETURN)) {
                    logInfo("ELSE node - RETURN node found");
                } else {
                    logError("ELSE node - RETURN node not found [" + "Last node: " + bufferNode.getNodeClass() + "]");
                    errorFound = true;
                }
            } else if (bufferNode.getNodeClass().equals(ASTClass.RETURN)) {
                logInfo("RETURN node found");
            } else {
                logError("RETURN node not found [" + "Last node: " + bufferNode.getNodeClass() + "]");
                errorFound = true;
            }
        }

    }

    /**
     * get last linked node
     * @param node node
     */
    private void getLastLinkNode(ASTNode node) {
        if (node.getLink() == null) {
            bufferNode = node;
        } else {
            getLastLinkNode(node.getLink());
        }
    }

    //-------------------------------------------------------------------------------------------------------

    /**
     * itterate over nodes and check them
     * @param node next node
     */
    private void checkallNodes(ASTNode node) {
        //check IF & WHILE has bool condition
        if (node.getNodeClass() != null && (node.getNodeClass().equals(ASTClass.IF) ||
                node.getNodeClass().equals(ASTClass.WHILE))) {
            checkIfWhileCondition(node);
        }

        //checkVarsInit
        if (node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.VAR) ){
            checkVarInit(node);
        }

        //checkFinalgetAssign
        if (node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.ASSIGN) ){
            checkAssignFinal(node.getLeft());
        }

        if (node.getLeft() != null) {
            checkallNodes(node.getLeft());
        }
        if (node.getRight() != null) {
            checkallNodes(node.getRight());
        }
        if (node.getLink() != null) {
            checkallNodes(node.getLink());
        }
    }

    /**
     * check if Var is initialized
     * @param node node
     */
    private void checkVarInit(ASTNode node) {
        actualCheck = "checkVarInit";
        if( findVar(node.getName()) ){
            logInfo("Var " + node.getName() + " is initialized");
        }else{
            errorFound = true;
            logError("Var " + node.getName() + " is not initialized!");
        }
    }

    /**
     * check IF & WHILE condition = bool
     * @param ifWhileNode node IF/WHILE
     */
    private void checkIfWhileCondition(ASTNode ifWhileNode) {
        actualCheck = "checkIfWhileCondition";
        logInfo("Check " + ifWhileNode.getNodeClass() + " Node (ID: " + ifWhileNode.getId() + ")");
        ASTNode conditionNode = ifWhileNode.getLeft();
        if (conditionNode.getNodeSubclass() != null &&
                (conditionNode.getNodeSubclass().equals(TokenType.EQUAL)   ||
                 conditionNode.getNodeSubclass().equals(TokenType.NEQUAL)  ||
                 conditionNode.getNodeSubclass().equals(TokenType.GREATER) ||
                 conditionNode.getNodeSubclass().equals(TokenType.GR_EQ)   ||
                 conditionNode.getNodeSubclass().equals(TokenType.SMALLER) ||
                 conditionNode.getNodeSubclass().equals(TokenType.SM_EQ))) {
            logInfo("Bool condition found");
        } else {
            errorFound = true;
            logError("Bool condition not found [node: " + conditionNode.getNodeClass() + "]");
        }
    }

    /**
     * check if Var is initialized
     * @param node node
     */
    private void checkAssignFinal(ASTNode node) {
        actualCheck = "checkAssignFinal";
        for (STObject obj : checkList) {
            if (obj.getName().equals(node.getName())){
                if(obj.getObjClass().equals(ObjClass.CONST)){
                    errorFound = true;
                    logError( "cannot assign a value to final variable " + node.getName());
                }
            }
        }
    }

    //-------------------------------------------------------------------------------------------------------

    /**
     * check Var has value on use
     * @param node node
     * @param list list of variables with value
     */
    private void checkVarValue(ASTNode node, List<String> list) {
        if(node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.VAR)){
            if(node.getName()!= null) hasValue(node.getName(), list);
        }

        if(node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.PROD)){
            checkParameterValue(node.getObject().getSymtab(), list);
        }

        if (node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.ASSIGN) ){
            //add right Var
            if(!list.contains(node.getLeft().getName())) list.add(node.getLeft().getName());

            //check right side
            checkVarValue(node.getRight(), list);
        }

        if (node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.IF_ELSE)  ){
            //If Condition Check
            checkVarValue(node.getLeft().getLeft(), list);

            //If Statements with new List
            checkVarValue(node.getLeft().getRight(), new LinkedList<>(list));

            //Else Statements with new List
            checkVarValue(node.getRight(), new LinkedList<>(list));
        }

        if (node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.WHILE)){
            //While Condition Check
            checkVarValue(node.getLeft(), list );

            //While Statements with new List
            checkVarValue(node.getRight(), new LinkedList<>(list) );
        }

        if (node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.BINOP)){     //check left and right side of BINOP
            checkVarValue(node.getLeft(), list );
            checkVarValue(node.getRight(), list );
        }
        if (node.getNodeClass() != null && node.getNodeClass().equals(ASTClass.RETURN)){     //check Return
            if(node.getLeft() != null) checkVarValue(node.getLeft(), list );
        }


        if (node.getLink() != null) {
            checkVarValue(node.getLink(), list);
        }
    }

    /**
     * check if called parameter variables has value
     * @param symtab symboltable
     * @param list list of variables with value
     */
    private void checkParameterValue(SymbolTable symtab, List<String> list) {
        for (STObject s: symtab.getObjects()) {
            if(s.getObjClass().equals(ObjClass.PAR)){
                hasValue(s.getName(), list);
            }else if(s.getObjClass().equals(ObjClass.PROC)){
                checkParameterValue(s.getSymtab(), list);
            }
        }
    }

    /**
     * check if called variables has value
     * @param name name
     * @param list list of variables with value
     */
    private void hasValue(String name, List<String> list) {
        if(!list.contains(name)){
            logError("Error! Var " + name+ " is null");
            errorFound = true;
        }else{
            logInfo("Var " + name + " has value");
        }

    }

    /**
     * get init values from method (Parameter/Finals)
     * @param symbolTable symboltable
     * @param method node
     */
    private LinkedList<String> getInitValues(SymbolTable symbolTable, STObject method){
        LinkedList<String> objList = new LinkedList<>();
        //finals
        for (STObject obj : symbolTable.getObjects()) {
            if( obj.getObjClass().equals(ObjClass.CONST) ){
                objList.add(obj.getName());
            }
        }
        // parameter
        for (STObject obj : method.getSymtab().getObjects() ){
            if( obj.getObjClass().equals(ObjClass.PAR) ){
                objList.add(obj.getName());
            }
        }
        return objList;
    }

    //-------------------------------------------------------------------------------------------------------
    // Helper Methods
    /**
     * get all init variables (Parameter/Finals/Globals)
     * @param symbolTable symboltable
     * @param method node
     */
    private void getInitVars(SymbolTable symbolTable, STObject method){
        LinkedList<STObject> objList = new LinkedList<>();
        //finals
        for (STObject obj : symbolTable.getObjects()) {
            if( obj.getObjClass().equals(ObjClass.CONST) ){
                objList.add(obj);
            }
        }
        // vars
        for (STObject obj : symbolTable.getObjects()) {
            if( obj.getObjClass().equals(ObjClass.VAR) ){
                objList.add(obj);
            }
        }
        // vars method
        for (STObject obj : method.getSymtab().getObjects() ){
            if( obj.getObjClass().equals(ObjClass.VAR) || obj.getObjClass().equals(ObjClass.PAR) ){
                objList.add(obj);
            }
        }
        checkList = objList;
    }

    /**
     * find Var in list for init variables
     * @param name name
     * @return true/false
     */
    private boolean findVar(String name){
        for (STObject obj : checkList) {
            if(obj.getName().equals(name)){
                return true;
            }
        }
        return false;
    }

    //-------------------------------------------------------------------------------------------------------
    // Log Methods

    /**
     * print method name
     * @param methodname name
     */
    private void logMethod(String methodname) {
        if(debugMode) logger.info("------------------------------------ " + methodname + " ------------------------------------");
    }

    /**
     * print ERROR
     * @param message message
     */
    private void logError(String message) {
        logger.error("SemanticAnalyzer." + actualCheck + ": " + message);
    }

    /**
     * print INFO
     * @param message message
     */
    private void logInfo(String message) {
        if(debugMode) logger.info("SemanticAnalyzer." + actualCheck + ": " + message);
    }

    /**
     * print analyzer summary
     */
    private void logSummary() {
        if(errorFound){
            logger.error("SemanticAnalyzer.analyze: Error found!");
        }else {
            logger.info("SemanticAnalyzer.analyze: No Error found!");
        }
    }

}
