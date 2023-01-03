package generator.rule;

import generator.generator.CodeGenerator;
import generator.semantic.SemanticException;
import generator.scope.ScopeController;
import generator.tree.Node;

@FunctionalInterface
public interface Rule {

    void run(Node node, RuleRunner checker, ScopeController scope, CodeGenerator generator) throws SemanticException;

}
