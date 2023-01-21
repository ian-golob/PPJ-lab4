import java.io.*;

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
        try(PrintStream output = new PrintStream(new FileOutputStream("a.frisc"))){
            GeneratorKoda sa = new GeneratorKoda(System.in, output);

            sa.analyzeInputAndGenerateCode();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    public void analyzeInputAndGenerateCode() throws SemanticException {
        Node root = Parser.parseInput(in);
        runner.runRoot(root);
    }

}
