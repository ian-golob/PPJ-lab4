package generator.generator;

import generator.scope.ScopeController;

import java.util.ArrayList;
import java.util.List;

import static generator.generator.Register.R7;

public class ProgramStack {

    private long tmpCounter = 0;

    List<StackEntry> stack = new ArrayList<>();

    private final ScopeController scope;

    public ProgramStack(ScopeController scope) {
        this.scope = scope;
    }


    private enum StackEntryType {
        VARIABLE, F_RETURN
    }

    private static class StackEntry {

        private final String name;

        private final StackEntryType type;


        private StackEntry(String name, StackEntryType type) {
            this.name = name;
            this.type = type;
        }
    }

    public void addVariable(String variableName){
        stack.add(new StackEntry(variableName, StackEntryType.VARIABLE));
    }

    public String generateLOADVariableAddress(String variableName, Register to){
        for(int i = stack.size() - 1; i >= 0; i--){
            if(stack.get(i).name.equals(variableName)){
                String code = "";

                int adjustment = (stack.size() - i) * Constants.WORD_LENGTH;

                return  CodeGenerator.generateADD(R7, adjustment, to);
            }
        }

        return CodeGenerator.generateMOVE("G_" + variableName, to);
    }

    public String getReturnAddress(){
        for(int i = stack.size()-1; i >= 0; i--){
            if(stack.get(i).type == StackEntryType.F_RETURN){
                return String.valueOf(Constants.STACK_TOP_ADDRESS + i * Constants.WORD_LENGTH);

            }
        }

        throw new IllegalArgumentException("Return address not found");
    }

    public String addTmpVariable(){
        String name = "_tmp_" + tmpCounter++;

        stack.add(new StackEntry(name, StackEntryType.VARIABLE));

        return name;
    }

    public void removeStackEntries(int number){
        stack.subList(0, stack.size() - number);
    }

}
