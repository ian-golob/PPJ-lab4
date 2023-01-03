package generator.rule;

import generator.generator.CodeGenerator;
import generator.semantic.SemanticException;
import generator.semantic.SemanticFinishedException;
import generator.model.function.Function;
import generator.model.type.DataType;
import generator.model.type.FunctionType;
import generator.model.type.NumericType;
import generator.scope.ScopeController;
import generator.tree.Node;
import generator.tree.TreeElement;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RuleRunner {

    private final ScopeController scope;

    private final Map<String, Map<List<String>, Rule>> rules = new HashMap<>();

    private final PrintStream out;

    private final CodeGenerator generator;


    public RuleRunner(ScopeController scopeController, PrintStream output, CodeGenerator generator) {
        this.out = output;
        this.scope = scopeController;
        this.generator = generator;

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

    public void run(Node node){
        List<String> childrenNames = node.getChildren().stream().map(TreeElement::getName).collect(Collectors.toList());
        Rule rule = getRule(node.getName(), childrenNames);

        try{

            rule.run(node, this, scope, generator);

        } catch(SemanticException ex){
            throw new SemanticFinishedException(node.getProductionErrorString());
        }
    }


    public void runRoot(Node root) {

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
                throw new SemanticFinishedException("main");
            }

            // provjera funkcija
            try {
                for(Function function: scope.getFunctionHistory()){
                    if(!function.isDefined()){
                        throw new SemanticException();
                    }
                }
            } catch (SemanticException e) {
                throw new SemanticFinishedException("funkcija");
            }

        } catch (SemanticFinishedException ex){
            System.err.println(ex.getMessage());
        }
    }
}
