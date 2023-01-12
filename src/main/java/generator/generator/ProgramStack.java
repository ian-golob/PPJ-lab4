package generator.generator;

import generator.scope.ScopeController;

import java.util.ArrayList;
import java.util.List;

public class ProgramStack {

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

        private final int value;

        private StackEntry(String name, StackEntryType type, int value) {
            this.name = name;
            this.type = type;
            this.value = value;
        }
    }

    public void addVariable(String variableName, Integer value){
        stack.add(new StackEntry(variableName, StackEntryType.VARIABLE, value));
    }

    public String getVariableAddress(String variableName){

        for(int i = stack.size() - 1; i >= 0; i--){
            if(stack.get(i).name.equals(variableName)){
                throw new UnsupportedOperationException();
                //String.valueOf("R6 + blabla");
            }

        }
        // assert variable is global
        return "G_" + variableName;
    }

    public String getReturnAddress(){
        for(int i = stack.size()-1; i >= 0; i--){
            if(stack.get(i).type == StackEntryType.F_RETURN){
                return String.valueOf(Constants.STACK_TOP_ADDRESS + i * Constants.WORD_LENGTH);

            }
        }

        throw new IllegalArgumentException("Return address not found");
    }

}
