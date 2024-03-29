package generator.scope;

import generator.model.function.Function;
import generator.model.variable.Variable;
import generator.semantic.SemanticException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalVariableScope implements VariableScope {

    private final Map<String, ScopeElement> valueMap = new HashMap<>();

    private final VariableScope outerScope;

    public String breakAddress, continueAddress;

    public LocalVariableScope(VariableScope outerScope) {
        this.outerScope = outerScope;
    }

    @Override
    public VariableScope getOuterScope() {
        return outerScope;
    }

    @Override
    public void defineNewVariable(Variable variable) throws SemanticException {
        Object o = valueMap.get(variable.getName());

        if(o!= null){
            throw new SemanticException();
        }

        valueMap.put(variable.getName(), variable);
    }

    @Override
    public void declareNewFunction(Function function) throws SemanticException {
        Object o = valueMap.get(function.getName());

        if(o != null){
            throw new SemanticException();
        }

        valueMap.put(function.getName(), function);
    }

    @Override
    public Variable getVariable(String variableName) throws SemanticException {
        Object o = valueMap.get(variableName);

        if(o == null){
            return outerScope.getVariable(variableName);
        }

        if(!(o instanceof Variable)){
            throw new SemanticException();
        }

        return (Variable) o;
    }

    @Override
    public Function getFunction(String functionName) throws SemanticException {
        Object o = valueMap.get(functionName);

        if(o == null){
            return outerScope.getFunction(functionName);
        }

        if(!(o instanceof Function)){
            throw new SemanticException();
        }

        return (Function) o;
    }

    @Override
    public boolean isDeclared(String sourceText) {
        return valueMap.containsKey(sourceText) || outerScope.isDeclared(sourceText);
    }

    @Override
    public ScopeElement get(String name) {
        if(valueMap.containsKey(name)){
            return valueMap.get(name);
        }
        return outerScope.get(name);
    }

    @Override
    public void addBreakAddress(String name) {
        breakAddress = name;
    }

    @Override
    public void addContinueAddress(String name) {
        continueAddress = name;
    }

    @Override
    public String getBreakAddress() {
        if (breakAddress == null) {
            return outerScope.getBreakAddress();
        } else return breakAddress;
    }

    @Override
    public String getContinueAddress() {
        if (breakAddress == null) {
            return outerScope.getContinueAddress();
        } else return continueAddress;
    }

    @Override
    public boolean functionIsDeclaredLocally(String functionName) {
        return valueMap.containsKey(functionName) && valueMap.get(functionName) instanceof Function;
    }

    @Override
    public boolean isDeclaredGlobally(String functionName) {
        return outerScope.isDeclaredGlobally(functionName);
    }

    @Override
    public boolean variableIsGlobal(String variableName) {
        return outerScope.variableIsGlobal(variableName);
    }

    @Override
    public boolean variableIsOnlyGlobal(String variableName) {
        if(valueMap.containsKey(variableName) && valueMap.get(variableName) instanceof Variable){
            return false;
        }
        return outerScope.variableIsOnlyGlobal(variableName);
    }

    @Override
    public Function getGloballyDeclaredFunction(String functionName) throws SemanticException {
        return outerScope.getGloballyDeclaredFunction(functionName);
    }

    @Override
    public List<Variable> getAllGlobalVariables() {
        return outerScope.getAllGlobalVariables();
    }
    /*
    public void defineFunction(Function function) throws SemanticException {
        Object o = valueMap.get(function.getName());

        if( o!= null && (
                !(o instanceof Function) ||
                ((Function) o).isDefined() ||
                !((Function) o).matchesSignatureOf(function))){
            throw new SemanticException();
        }

        function.define();
        valueMap.put(function.getName(), function);
    }

     */
}
