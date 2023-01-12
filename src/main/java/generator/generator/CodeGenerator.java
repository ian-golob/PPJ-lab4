package generator.generator;

public class CodeGenerator {


    public static String generateMOVE(Integer number, Register to){
        return " MOVE %D " + number + ", " + to.name() + "\n";
    }

    public static String generateMOVE(String from, Register to){
        return " MOVE " + from + ", " + to.name() + "\n";
    }

    public static String generateLOAD(String address, Register to){
        return " LOAD " + to.name() + ", (" + address + ")\n";
    }

    public static String generateSTORE(Register from, String addressTo){
        return " STORE " + from.name() + ", (" + addressTo + ")\n";
    }

    public static String generateRET() {
        return " RET";
    }

    //SUB R1, 1, R3
    public static String generateSUB(Register r1, int number, Register r3){
        return " SUB " + r1.name() + ", " + number + ", " + r3.name() + "\n";
    }

    //SUB R1, R2, R3
    public static String generateSUB(Register r1, Register r2, Register r3){
        return " SUB " + r1.name() + ", " + r2.name() + ", " + r3.name() + "\n";
    }

    //ADD R1, R2, R3
    public static String generateADD(Register r1, Register r2, Register r3){
        return " ADD " + r1.name() + ", " + r2.name() + ", " + r3.name() + "\n";
    }

    //ADD R1, R2, R3
    public static String generateADD(Register r1, int number, Register r3){
        return " ADD " + r1.name() + ", " + number + ", " + r3.name() + "\n";
    }
}
