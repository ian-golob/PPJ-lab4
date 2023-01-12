package generator.generator;

import generator.model.variable.Variable;
import generator.scope.ScopeController;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        write(" MOVE 40000, R7\n" +
                globalVariableDefinitionsString +
                " CALL F_main\n" +
                " HALT\n\n");
    }

    public void writeFunctions(){
        functionDefinitions.forEach(out::print);
        out.println();
    }

    public void writeGlobalVariables(){
        List<Variable> variables = scope.getAllGlobalVariables();

        variables.forEach(variable -> {
            if(variable.isArray()){
                // TODO
                throw new UnsupportedOperationException();
            }

            write("G_" + variable.getName() + " DW %D 0\n");
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
