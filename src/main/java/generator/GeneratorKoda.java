package generator;

import generator.generator.CodeWriter;
import generator.generator.ProgramStack;
import generator.rule.RuleRunner;
import generator.scope.ScopeController;
import generator.semantic.SemanticException;
import generator.tree.Node;

import java.io.*;

import static generator.tree.Parser.parseInput;

public class GeneratorKoda {

    private final RuleRunner runner;

    private final InputStream in;

    private final PrintStream out;

    public GeneratorKoda(InputStream in, PrintStream out) {
        this.in = in;
        this.out = out;

        ScopeController scopeController = new ScopeController();
        CodeWriter writer = new CodeWriter(scopeController, out);
        runner = new RuleRunner(scopeController, out, writer, new ProgramStack(scopeController));
    }

    public static void main(String[] args) throws SemanticException {
        GeneratorKoda sa = new GeneratorKoda(System.in, System.out);

        sa.analyzeInputAndGenerateCode();
    }

    public void analyzeInputAndGenerateCode() throws SemanticException {
        Node root = parseInput(in);
        runner.runRoot(root);
    }

}
