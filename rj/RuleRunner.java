import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleRunner {

    private final ScopeController scope;

    private final Map<String, Map<List<String>, Rule>> rules = new HashMap<>();

    private final PrintStream out;

    private final CodeWriter writer;

    private final ProgramStack stack;


    public RuleRunner(ScopeController scopeController, PrintStream output, CodeWriter writer, ProgramStack stack) {
        this.out = output;
        this.scope = scopeController;
        this.writer = writer;
        this.stack = stack;

        RuleLoader rl = new RuleLoader(rules);
        rl.loadRules();
    }

    private Rule getRule(String nonTerminalSymbol, List<String> rightSideSymbols){
        if(rules.get(nonTerminalSymbol) == null ||
                rules.get(nonTerminalSymbol).get(rightSideSymbols) == null){
            throw new IllegalArgumentException("Production not found for:" + nonTerminalSymbol + " -> " + rightSideSymbols);
        }

        return rules.get(nonTerminalSymbol).get(rightSideSymbols);
    }

    public void run(Node node) throws SemanticException {
        List<String> childrenNames = node.getChildren().stream().map(TreeElement::getName).collect(Collectors.toList());
        Rule rule = getRule(node.getName(), childrenNames);

        try{

            rule.run(node, this, scope, writer, stack);

        } catch(SemanticException ex){
            throw ex;
        }
    }


    public void runRoot(Node root) throws SemanticException {

        try{
            run(root);

            // provjera main
            try {
                FunctionType mainType = new FunctionType(NumericType.INT, DataType.VOID);
                if(!(scope.functionIsDefined("main") &&
                        scope.getGloballyDeclaredFunction("main")
                                .getFunctionType().equals(mainType))){
                    throw new SemanticException();
                }
            } catch (SemanticException e) {
                throw e;
            }

            writer.writeHeader();
            writer.writeFunctions();
            writer.writeConstants();
            writer.writeGlobalVariables();

        } catch (SemanticFinishedException ex){
            System.err.println(ex.getMessage());
        }
    }
}
