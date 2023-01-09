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
        int offset = 0;
        for(int i = 0; i < stack.size(); i++){
            if(stack.get(i).name.equals(variableName)){
                break;
            } else {
                offset++;
            }

            if(i == stack.size() - 1){
                throw new IllegalArgumentException("Variable not found");
            }
        }

        return String.valueOf(Constants.STACK_TOP_ADDRESS + offset * Constants.WORD_LENGTH);
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
