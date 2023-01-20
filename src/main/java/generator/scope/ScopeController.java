package generator.scope;

import generator.semantic.SemanticException;
import generator.model.function.Function;
import generator.model.variable.Variable;

import java.util.ArrayList;
import java.util.List;

public class ScopeController {

    private final List<Function> functionHistory = new ArrayList<>();

    private VariableScope currentVariableScope;

    private Function currentFunction;

    private int loopCounter = 0;

    public ScopeController(){
        currentVariableScope = new GlobalVariableScope();
    }

    public void defineNewScope(){
        currentVariableScope = new LocalVariableScope(currentVariableScope);
    }

    public void exitLastScope(){
        currentVariableScope = currentVariableScope.getOuterScope();
    }

    public void declareVariable(Variable variable) throws SemanticException {
        currentVariableScope.defineNewVariable(variable);
    }

    public Variable getVariable(String variableName) throws SemanticException {
        return currentVariableScope.getVariable(variableName);
    }

    public boolean variableIsDeclared(String variableName) throws SemanticException {
        Variable variable = getVariable(variableName);

        return variable != null;
    }

    public boolean isDeclaredGlobally(String functionName) {
        return currentVariableScope.isDeclaredGlobally(functionName);
    }

    public Function getGloballyDeclaredFunction(String functionName) throws SemanticException {
        return currentVariableScope.getGloballyDeclaredFunction(functionName);
    }

    public void requireDeclaredVariable(String variableName) throws SemanticException{
        if(!variableIsDeclared(variableName)){
            throw new SemanticException("Variable not declared");
        }
    }

    public void declareFunction(Function function) throws SemanticException {
        functionHistory.add(function);
        currentVariableScope.declareNewFunction(function);
    }

    public List<Function> getFunctionHistory() {
        return functionHistory;
    }

    public void startFunctionDefinition(Function function) throws SemanticException {
        currentVariableScope.declareNewFunction(function);
        function.define();
        functionHistory.add(function);

        currentFunction = function;

        defineNewScope();
    }

    public void endFunctionDefinition() {
        currentFunction = null;
        exitLastScope();
    }

    public Function getCurrentFunction() {
        return currentFunction;
    }

    public void enterLoop(){
        loopCounter++;
    }

    public void exitLoop(){
        loopCounter--;
    }

    public boolean inLoop(){
        return loopCounter > 0;
    }

    public void addBreakAddress(String name){
        currentVariableScope.addBreakAddress(name);
    }

    public void addContinueAddress(String name){
        currentVariableScope.addContinueAddress(name);
    }

    public String getBreakAddress(){
        return currentVariableScope.getBreakAddress();
    }

    public String getContinueAddress(){
        return currentVariableScope.getContinueAddress();
    }

    public void requireDeclared(String sourceText) throws SemanticException {
        if(!currentVariableScope.isDeclared(sourceText)) {
            throw new SemanticException();
        }
    }

    public boolean functionIsDeclared(String functionName) throws SemanticException {
        Function function = currentVariableScope.getFunction(functionName);

        return function != null;
    }

    public boolean functionIsDefined(String functionName) throws SemanticException {
        Function function = currentVariableScope.getFunction(functionName);

        return function != null && function.isDefined();
    }

    public Function getFunction(String functionName) throws SemanticException {
        return currentVariableScope.getFunction(functionName);
    }

    public ScopeElement get(String name){
        return currentVariableScope.get(name);
    }

    public boolean functionIsDeclaredLocally(String functionName) {
        return currentVariableScope.functionIsDeclaredLocally(functionName);
    }

    public List<Variable> getAllGlobalVariables(){
        return currentVariableScope.getAllGlobalVariables();
    }

    public boolean variableIsGlobal(String name) {
        return currentVariableScope.variableIsGlobal(name);
    }
}
