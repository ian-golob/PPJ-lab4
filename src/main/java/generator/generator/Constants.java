package generator.generator;

public class Constants {

    public static final int STACK_TOP_ADDRESS = 40000;

    public static final int WORD_LENGTH = 4;

    public static final String MULTIPLICATION = "MNOZI\n" +
            " MOVE 0, R2\n" +
            " CMP R6, 0\n" +
            " JP_P POZM\n" +
            "NEGM SUB R2, R5, R2\n" +
            " ADD R6, 1, R6\n" +
            " JP_NZ NEGM\n" +
            " JP KRAJM\n" +
            "POZM ADD R2, R5, R2\n" +
            " SUB R6, 1, R6\n" +
            " JP_NZ POZM\n" +
            "KRAJM\n" +
            " ADD R2, 0, R6\n" +
            " RET\n";

    public static final String DIVISION = "DIJELI\n" +
            " MOVE 0, R2\n" +
            " XOR R5, R6, R3\n" +
            "TEST_1 OR R5, R5, R5\n" +
            " JR_P TEST_2\n" +
            "NEGAT_1 XOR R5, -1, R5\n" +
            " ADD R5, 1, R5\n" +
            "TEST_2 OR R6, R6, R6\n" +
            " JR_P PETLJA\n" +
            "NEGAT_2 XOR R6, -1, R6\n" +
            " ADD R6, 1, R6\n" +
            "PETLJA SUB R5, R6, R5\n" +
            " JR_ULT GOTOVAPETLJA\n" +
            " ADD R2, 1, R2\n" +
            " JR PETLJA\n" +
            "GOTOVAPETLJA\n" +
            " ROTL R3, 1, R3\n" +
            " JR_NC GOTOVO\n" +
            " XOR R2, -1, R2\n" +
            " ADD R2, 1, R2\n" +
            "GOTOVO\n" +
            " MOVE R2, R6\n" +
            " RET\n";

    public static final String MODULO = "OSTATAK SUB R5, R6, R5\n" +
            " JR_ULT GOTOVA_PETLJA\n" +
            " JR OSTATAK\n" +
            "GOTOVA_PETLJA\n" +
            " ADD R5, R6, R6\n" +
            " RET\n";
}
