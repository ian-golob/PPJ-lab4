package generator.scope;

import generator.semantic.SemanticException;
import generator.model.function.Function;
import generator.model.variable.Variable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobalVariableScope implements VariableScope {


    private final Map<String, ScopeElement> valueMap = new HashMap<>();

    @Override
    public VariableScope getOuterScope() {
        throw new UnsupportedOperationException("The global scope has no outer scope.");
    }

    @Override
    public void defineNewVariable(Variable variable) throws SemanticException {
        if(valueMap.containsKey(variable.getName())){
            throw new SemanticException("Already defined in this scope");
        }

        valueMap.put(variable.getName(), variable);
    }

    @Override
    public void declareNewFunction(Function function) throws SemanticException {
        valueMap.put(function.getName(), function);
    }

    @Override
    public Variable getVariable(String variableName) throws SemanticException {
        if(valueMap.get(variableName) != null &&
                !(valueMap.get(variableName) instanceof Variable)){
            throw new SemanticException();
        }

        return (Variable) valueMap.get(variableName);
    }

    @Override
    public Function getFunction(String functionName) throws SemanticException {
        if(valueMap.get(functionName) != null &&
                !(valueMap.get(functionName) instanceof Function)){
            throw new SemanticException();
        }

        return (Function) valueMap.get(functionName);
    }

    @Override
    public boolean isDeclared(String sourceText) {
        return valueMap.containsKey(sourceText);
    }

    @Override
    public ScopeElement get(String sourceText) {
        return valueMap.get(sourceText);
    }

    @Override
    public boolean functionIsDeclaredLocally(String functionName) {
        return valueMap.containsKey(functionName) && valueMap.get(functionName) instanceof Function;
    }

    @Override
    public boolean isDeclaredGlobally(String functionName) {
        return valueMap.containsKey(functionName);
    }

    @Override
    public boolean variableIsGlobal(String variableName) {
        return valueMap.containsKey(variableName) && valueMap.get(variableName) instanceof Variable;
    }

    @Override
    public Function getGloballyDeclaredFunction(String functionName) throws SemanticException {
        if(valueMap.containsKey(functionName) && !(valueMap.get(functionName) instanceof Function)){
            throw new SemanticException();
        }

        return (Function) valueMap.get(functionName);
    }

    @Override
    public List<Variable> getAllGlobalVariables() {
        return valueMap.values().stream()
                .filter(scopeElement -> scopeElement instanceof Variable)
                .map(scopeElement -> (Variable) scopeElement)
                .collect(Collectors.toList());
    }
}
