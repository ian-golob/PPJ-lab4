public class CodeGenerator {


    private static int lineAddressCount = 0;
    public static String generateLineAddress(){
        return "L_" + lineAddressCount++;
    }

    public static String generateMOVE(Integer number, Register to){
        return " MOVE %D " + number + ", " + to.name() + "\n";
    }

    public static String generateFunctionCALL(String functionName){
        return " CALL F_" + functionName + "\n";
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

    public static String generateCMP(Register r1, int number){
        return " CMP " + r1.name() + ", " + numberToHex(number) + "\n";
    }

    public static String generateCMP(Register r1, Register r2){
        return " CMP " + r1.name() + ", " + r2.name() + "\n";
    }

    public static String generateJP(String to){
        return " JP " + to + "\n";
    }

    public static String generateJP_EQ(String to){
        return " JP_EQ " + to + "\n";
    }

    public static String generateJP_NE(String to){
        return " JP_NE " + to + "\n";
    }

    public static String generateJP_SLT(String to){
        return " JP_SLT " + to + "\n";
    }

    public static String generateJP_SLE(String to){
        return " JP_SLE " + to + "\n";
    }

    public static String generateJP_SGT(String to){
        return " JP_SGT " + to + "\n";
    }

    public static String generateJP_SGE(String to){
        return " JP_SGE " + to + "\n";
    }

    public static String generateRET() {
        return " RET\n";
    }

    //SUB R1, 1, R3
    public static String generateSUB(Register r1, int number, Register r3){
        return " SUB " + r1.name() + ", " + numberToHex(number) + ", " + r3.name() + "\n";
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
        return " ADD " + r1.name() + ", " + numberToHex(number) + ", " + r3.name() + "\n";
    }

    //OR R1, R2, R3
    public static String generateOR(Register r1, Register r2, Register r3){
        return " OR " + r1.name() + ", " + r2.name() + ", " + r3.name() + "\n";
    }

    //AND R1, R2, R3
    public static String generateAND(Register r1, Register r2, Register r3){
        return " AND " + r1.name() + ", " + r2.name() + ", " + r3.name() + "\n";
    }

    public static String generateAND(Register r1, int number, Register r3){
        return " AND " + r1.name() + ", " + numberToHex(number) + ", " + r3.name() + "\n";
    }

    public static String generateXOR(Register r1, Register r2, Register r3){
        return " XOR " + r1.name() + ", " + r2.name() + ", " + r3.name() + "\n";
    }

    public static String generateXOR(Register r1, int number, Register r3){
        return " XOR " + r1.name() + ", " + numberToHex(number) + ", " + r3.name() + "\n";
    }

    public static String generateMUL(){
        return " CALL MNOZI\n";
    }

    public static String generateDIV(){
        return " CALL DIJELI\n";
    }

    public static String generateMOD(){
        return " CALL OSTATAK\n";
    }

    private static String numberToHex(int number){
        return "0" + Integer.toHexString(number);
    }
}
