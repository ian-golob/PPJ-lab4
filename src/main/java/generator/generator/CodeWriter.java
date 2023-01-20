package generator.generator;

import generator.model.variable.Variable;
import generator.scope.ScopeController;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static generator.generator.Constants.*;

public class CodeWriter {

    private final ScopeController scope;

    private final PrintStream out;

    private final List<String> functionDefinitions = new ArrayList<>();

    private final List<String> constantDefinitions = new ArrayList<>();

    private final List<String> globalVariableDefinitions = new ArrayList<>();

    public CodeWriter(ScopeController scope, PrintStream out) {
        this.scope = scope;
        this.out = out;
    }

    public void write(String command){
        out.print(command);
    }

    public void defineFunction(String name, String code){
        //write(name + "\n" + code);
        functionDefinitions.add("F_" + name + "\n" + code + "\n");
    }

    public void writeHeader() {
        String globalVariableDefinitionsString = String.join("", globalVariableDefinitions);
        String helperFunctions = MULTIPLICATION + '\n' + DIVISION + '\n' + MODULO;
        write(" MOVE 40000, R7\n" +
                globalVariableDefinitionsString +
                CodeGenerator.generateFunctionCALL("main") +
                " HALT\n\n" + helperFunctions + "\n");
    }

    public void writeFunctions(){
        functionDefinitions.forEach(out::print);
        out.println();
    }

    public void writeGlobalVariables(){
        List<Variable> variables = scope.getAllGlobalVariables();

        variables.forEach(variable -> {
            if(variable.isArray()){
                write("G_" + variable.getName() + " DW " +
                        "0" + ", 0".repeat(variable.getArraySize() - 1) + "\n");
            } else {
                write("G_" + variable.getName() + " DW %D 0\n");
            }
        });
    }

    public void writeConstants() {
        constantDefinitions.forEach(out::print);
        out.println();
    }

    public String defineConstant(Integer valueOf) {
        String constantAddress = "C_" + constantDefinitions.size();
        String constantDefinition = constantAddress + " DW %D " + valueOf + "\n";

        constantDefinitions.add(constantDefinition);

        return constantAddress;
    }

    public void addGlobalVariableDefinition(String kod) {
        globalVariableDefinitions.add(kod);
    }
}
