package generator.generator;

import generator.scope.ScopeController;

import java.util.LinkedList;
import java.util.List;

import static generator.generator.Register.R7;

public class ProgramStack {

    private long tmpCounter = 0;

    private List<StackEntry> stack = new LinkedList<>();

    private final ScopeController scope;

    public ProgramStack(ScopeController scope) {
        this.scope = scope;
    }

    public void defineTmpScope() {
        stack.add(new StackEntry("__start__", StackEntryType.SCOPE_START));
    }

    public void deleteLastTmpScope() {

        while(stack.size() > 0 && stack.get(stack.size()-1).type != StackEntryType.SCOPE_START){
            stack.remove(stack.size()-1);
        }
        stack.remove(stack.size()-1);

    }

    public int getVariableScopeOffset() {

        int offset = 0;

        int i = stack.size() - 1;
        while( i >= 0 && stack.get(i).type != StackEntryType.SCOPE_START){
            offset += Constants.WORD_LENGTH;

            i--;
        }

        return offset;
    }


    private enum StackEntryType {
        VARIABLE, F_RETURN, SCOPE_START
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

    public void addReturnAddress() {
        stack.add(new StackEntry("$returnAddress", StackEntryType.F_RETURN));
    }

    public String addTmpVariable(){
        String name = "_tmp_" + tmpCounter++;

        stack.add(new StackEntry(name, StackEntryType.VARIABLE));

        return name;
    }

    public String generateLOADVariableAddress(String variableName, Register to){
        int adjustment = 0;
        for(int i = stack.size() - 1; i >= 0; i--){

            if(stack.get(i).name.equals(variableName)){

                return  CodeGenerator.generateADD(R7, adjustment * Constants.WORD_LENGTH, to);

            } else if(stack.get(i).type != StackEntryType.SCOPE_START){

                adjustment++;

            }
        }

        return CodeGenerator.generateMOVE("G_" + variableName, to);
    }


    public void removeStackEntries(int number){
        stack = stack.subList(0, stack.size() - number);
    }

}
