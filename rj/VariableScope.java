import java.util.List;

public interface VariableScope {

    VariableScope getOuterScope();

    void defineNewVariable(Variable variable) throws SemanticException;

    void declareNewFunction(Function function) throws SemanticException;

    Variable getVariable(String variableName) throws SemanticException;

    Function getFunction(String functionName) throws SemanticException;

    boolean isDeclared(String name);

    ScopeElement get(String name);

    public void addBreakAddress(String name);

    public void addContinueAddress(String name);

    public String getBreakAddress();

    public String getContinueAddress();

    boolean functionIsDeclaredLocally(String functionName);

    boolean isDeclaredGlobally(String functionName);

    boolean variableIsGlobal(String variableName);

    boolean variableIsOnlyGlobal(String variableName);

    Function getGloballyDeclaredFunction(String functionName) throws SemanticException;

    List<Variable> getAllGlobalVariables();
}
