package generator.rule;

import generator.generator.CodeWriter;
import generator.generator.ProgramStack;
import generator.semantic.SemanticException;
import generator.scope.ScopeController;
import generator.tree.Node;

@FunctionalInterface
public interface Rule {

    void run(Node node, RuleRunner checker, ScopeController scope, CodeWriter writer, ProgramStack stack) throws SemanticException;

}
