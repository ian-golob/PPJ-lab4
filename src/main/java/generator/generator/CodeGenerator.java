package generator.generator;

public class CodeGenerator {


    public static String generateMOVE(Integer number, Register to){
        return " MOVE %D " + number + ", " + to.name() + "\n";
    }

    public static String generateLOAD(String address, Register to){
        return " LOAD " + to.name() + ", " + "(" + address + ")" + "\n";
    }

    public static String generateRET() {
        return " RET";
    }
}
