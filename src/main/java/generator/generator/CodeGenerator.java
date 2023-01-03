package generator.generator;

import generator.scope.ScopeController;

import java.io.PrintStream;

public class CodeGenerator {

    private final ScopeController scope;

    private final PrintStream out;

    public CodeGenerator(ScopeController scope, PrintStream out) {
        this.scope = scope;
        this.out = out;
    }
}
