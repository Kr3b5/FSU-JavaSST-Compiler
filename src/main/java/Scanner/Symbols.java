package Scanner;

/**
 * Enum of symbols
 * @author Kr3b5
 */
public enum Symbols {

    IDENT       (0),        // ident
    NUMBER      (1),        // number
    LPAREN      (2),        // (
    RPAREN      (3),        // )
    PLUS        (4),        // +
    MINUS       (5),        // -
    TIMES       (6),        // *
    SLASH       (7),        // /
    COMMA       (8),       // ,
    SEMI        (9),       // ;
    ASSIGN      (10),       // =
    EQUAL       (11),       // ==
    NEQUAL      (12),       // !=
    GREATER     (13),       // >
    SMALLER     (14),       // <
    GR_EQ       (15),       // >=
    SM_EQ       (16),       // <=
    LCBRACKET   (17),       // {
    RCBRACKET   (18),       // }

    // Schlüsselworte
    OTHER       (99);


    /**
     * The ID of the symbols
     */
    public final int id;

    Symbols(int symbol) {
        this.id = symbol;
    }
}
