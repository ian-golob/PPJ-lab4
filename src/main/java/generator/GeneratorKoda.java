package generator;

import generator.generator.CodeGenerator;
import generator.rule.RuleRunner;
import generator.scope.ScopeController;
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
        CodeGenerator generator = new CodeGenerator(scopeController, out);
        runner = new RuleRunner(scopeController, out, generator);
    }

    public static void main(String[] args) {
        GeneratorKoda sa = new GeneratorKoda(System.in, System.out);

        sa.analyzeInputAndGenerateCode();
    }

    public void analyzeInputAndGenerateCode() {
        Node root = parseInput(in);
        runner.runRoot(root);
    }

}
