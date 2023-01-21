@FunctionalInterface
public interface Rule {

    void run(Node node, RuleRunner checker, ScopeController scope, CodeWriter writer, ProgramStack stack) throws SemanticException;

}
