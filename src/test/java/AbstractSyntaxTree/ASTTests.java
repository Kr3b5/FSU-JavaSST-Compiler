package AbstractSyntaxTree;

import Parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;

public class ASTTests {

    ASTPrinter printer = new ASTPrinter();

    // files
    private static final String FSU_TEST            = "./src/test/resources/pass_test/Test.java";

    @Test
    public void FSUTest_complete() throws FileNotFoundException {
        Parser parser = new Parser(FSU_TEST);
        parser.parseFile();

        printer.printDot(parser.getAst());
    }

}
