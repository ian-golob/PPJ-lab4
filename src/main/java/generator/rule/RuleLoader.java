package generator.rule;

import generator.generator.Constants;
import generator.semantic.SemanticException;
import generator.model.function.Function;
import generator.model.type.ArrayType;
import generator.model.type.DataType;
import generator.model.type.FunctionType;
import generator.model.type.NumericType;
import generator.model.util.TreeUtil;
import generator.model.variable.Variable;
import generator.scope.ScopeElement;
import generator.tree.Leaf;
import generator.tree.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static generator.generator.CodeGenerator.*;
import static generator.generator.Register.*;
import static generator.model.type.NumericType.*;
import static generator.model.util.ConstantUtil.*;

public class RuleLoader {


    private final Map<String, Map<List<String>, Rule>> rules;


    public RuleLoader(Map<String, Map<List<String>, Rule>> rules) {
        this.rules = rules;
    }

    private void addRule(String productionLeftSide, List<String> productionRightSide, Rule rule){
        Map<List<String>, Rule> innerMap = rules.getOrDefault(productionLeftSide, new HashMap<>());

        innerMap.put(productionRightSide, rule);

        rules.put(productionLeftSide, innerMap);
    }

    public void loadRules(){

        // <primarni_izraz>>
        {
            addRule("<primarni_izraz>", List.of(
                    "IDN"
            ), (node, checker, scope, writer, stack) -> {
                Leaf IDN = (Leaf) node.getChild(0);

                String variableName = IDN.getSourceText();

                scope.requireDeclared(variableName);
                ScopeElement idn = scope.get(variableName);

                String code;
                if(idn instanceof Variable){
                    Variable var = (Variable) idn;

                    if(var.isArray()){
                        code = stack.generateLOADVariableAddress(variableName, R6);
                        node.setProperty("variableName", variableName);
                    } else {
                        code = stack.generateLOADVariableAddress(variableName, R6) +
                                generateLOAD(R6.name(), R6);
                        node.setProperty("variableName", variableName);
                    }


                } else {
                    // function
                    code = generateFunctionCALL(variableName);
                }

                node.setProperty("tip", idn.getType());
                node.setProperty("l-izraz", idn.isLValue());
                node.setProperty("kod", code);
            });


            addRule("<primarni_izraz>", List.of(
                    "BROJ"
            ), (node, checker, scope, writer, stack) -> {
                Leaf BROJ = (Leaf) node.getChild(0);

                String number = BROJ.getSourceText();

                requireIntValue(number);

                String constantAddress = writer.defineConstant(Integer.valueOf(number));
                String code = generateLOAD(constantAddress, R6);

                //String code = generateMOVE(Integer.valueOf(number), R6);

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<primarni_izraz>", List.of(
                    "ZNAK"
            ), (node, checker, scope, writer, stack) -> {
                Leaf ZNAK = (Leaf) node.getChild(0);

                requireValidChar(ZNAK.getSourceText());

                node.setProperty("tip", CHAR);
                node.setProperty("l-izraz", Boolean.FALSE);

                int c = ZNAK.getSourceText().charAt(1);
                String constantAddress = writer.defineConstant(c);
                String code = generateLOAD(constantAddress, R6);

                node.setProperty("kod", code);
            });

            addRule("<primarni_izraz>", List.of(
                    "NIZ_ZNAKOVA"
            ), (node, checker, scope, writer, stack) -> {
                Leaf NIZ_ZNAKOVA = (Leaf) node.getChild(0);

                requireValidString(NIZ_ZNAKOVA.getSourceText());


                node.setProperty("tip", ArrayType.of(CHAR));
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("niz", true);

                String code = NIZ_ZNAKOVA.getSourceText().substring(1, NIZ_ZNAKOVA.getSourceText().length()-1)
                        .chars().mapToObj(c -> {
                    String tmpCode = "";
                    String constantAddress = writer.defineConstant(c);
                    tmpCode = tmpCode + generateLOAD(constantAddress, R6);
                    tmpCode = tmpCode + generateLOAD(constantAddress, R6);
                    tmpCode = tmpCode + generateSTORE(R6, R5.name());
                    tmpCode = tmpCode + generateADD(R5, 4, R5);
                    return tmpCode;
                }).collect(Collectors.joining());

                node.setProperty("kod", code);
            });


            addRule("<primarni_izraz>", List.of(
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz = (Node) node.getChild(1);

                checker.run(izraz);

                node.setProperty("tip", izraz.getProperty("tip"));
                node.setProperty("l-izraz", izraz.getProperty("l-izraz"));
                node.setProperty("kod", izraz.getProperty("kod"));
            });

        }

        // <postfiks_izraz>
        {
            addRule("<postfiks_izraz>", List.of(
                    "<primarni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node primarni_izraz = (Node) node.getChild(0);

                checker.run(primarni_izraz);

                node.setProperty("tip", primarni_izraz.getProperty("tip"));
                node.setProperty("l-izraz", primarni_izraz.getProperty("l-izraz"));
                node.setProperty("kod", primarni_izraz.getProperty("kod"));

                if(primarni_izraz.hasProperty("variableName")){
                    node.setProperty("variableName", primarni_izraz.getProperty("variableName"));
                }

                if(primarni_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }

            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "L_UGL_ZAGRADA",
                    "<izraz>",
                    "D_UGL_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node postfiks_izraz = (Node) node.getChild(0);
                Node izraz = (Node) node.getChild(2);


                stack.defineTmpScope();
                checker.run(postfiks_izraz);
                stack.deleteLastTmpScope();

                if(!(postfiks_izraz.getProperty("tip") instanceof ArrayType)){
                    throw new SemanticException();
                }
                checker.run(izraz);
                if(!((NumericType) izraz.getProperty("tip")).implicitlyCastableTo(INT)){
                    throw new SemanticException();
                }

                DataType postfiks_izraz_tip = (DataType) postfiks_izraz.getProperty("tip");

                node.setProperty("tip", ((ArrayType) postfiks_izraz.getProperty("tip")).getNumericType());
                node.setProperty("l-izraz", !(postfiks_izraz_tip instanceof NumericType &&
                        ((NumericType) postfiks_izraz_tip).isConst()));

                String variableName = (String) postfiks_izraz.getProperty("variableName");

                String adresa = (String) izraz.getProperty("kod");

                adresa = adresa + stack.generateLOADVariableAddress(variableName, R5);
                adresa = adresa + generateADD(R5, R6, R5).repeat(4);
                adresa = adresa + generateADD(R5, 0, R6);

                String code = adresa + generateLOAD(R6.name(), R6);

                node.setProperty("kod", code);
                node.setProperty("adresa", adresa);
                node.setProperty("variableName", variableName);
            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                     "L_ZAGRADA",
                    "D_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node postfiks_izraz = (Node) node.getChild(0);

                checker.run(postfiks_izraz);

                FunctionType functionType = (FunctionType) postfiks_izraz.getProperty("tip");
                List<DataType> functionParameters = functionType.getParameters();

                if(!(functionParameters.size() == 0 ||
                    functionParameters.size() == 1 && functionParameters.get(0) == VOID)){
                    throw new SemanticException();
                }

                node.setProperty("tip", functionType.getReturnType());
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", postfiks_izraz.getProperty("kod"));
            });


            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "L_ZAGRADA",
                    "<lista_argumenata>",
                    "D_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {

                Node postfiks_izraz = (Node) node.getChild(0);
                Node lista_argumenata = (Node) node.getChild(2);

                checker.run(postfiks_izraz);

                stack.defineTmpScope();
                checker.run(lista_argumenata);

                if(!(postfiks_izraz.getProperty("tip") instanceof FunctionType)){
                    throw new SemanticException();
                }

                FunctionType functionType = (FunctionType) postfiks_izraz.getProperty("tip");
                List<DataType> tipovi = (List<DataType>) lista_argumenata.getProperty("tipovi");

                if(tipovi.size() != functionType.getParameters().size()){
                    throw new SemanticException();
                }

                for (int i = 0; i < tipovi.size(); i++){
                    DataType argTip = tipovi.get(i);
                    DataType paramTip = functionType.getParameters().get(i);

                    if(!argTip.implicitlyCastableTo(paramTip)){
                        throw new SemanticException();
                    }
                }

                List<String> codes = (List<String>) lista_argumenata.getProperty("kodovi");

                String code = "";
                for(int i = 0; i < codes.size(); i++){
                    code += codes.get(i) +
                            generateSUB(R7, 4, R7) +
                            generateADD(R7, 0, R5) +
                            generateSTORE(R6, R5.name());
                }

                /*
                code += (String) postfiks_izraz.getProperty("kod") +
                        generateADD(R7, 4 * codes.size(), R7);

                 */
                int localVariableOffset = stack.getVariableScopeOffset();
                code = code + postfiks_izraz.getProperty("kod") +
                        generateADD(R7, localVariableOffset, R7);
                stack.deleteLastTmpScope();

                node.setProperty("kod", code);
                node.setProperty("tip", functionType.getReturnType());
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "OP_INC"
            ), (node, checker, scope, writer, stack) -> {

                Node postfiks_izraz = (Node) node.getChild(0);
                checker.run(postfiks_izraz);

                if (!(Boolean) postfiks_izraz.getProperty("l-izraz").equals(Boolean.TRUE)) throw new SemanticException();
                if (postfiks_izraz.getProperty("tip") != INT) throw new SemanticException();

                String variableName = (String) postfiks_izraz.getProperty("variableName");
                String code = stack.generateLOADVariableAddress(variableName, R5) +
                        generateLOAD(R5.name(), R6) +
                        generateADD(R6, 1, R4) +
                        generateSTORE(R4, R5.name());
                node.setProperty("variableName", variableName);

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "OP_DEC"
            ), (node, checker, scope, writer, stack) -> {

                Node postfiks_izraz = (Node) node.getChild(0);
                checker.run(postfiks_izraz);

                if (!(Boolean) postfiks_izraz.getProperty("l-izraz").equals(Boolean.TRUE)) throw new SemanticException();
                if (postfiks_izraz.getProperty("tip") != INT) throw new SemanticException();

                String variableName = (String) postfiks_izraz.getProperty("variableName");
                String code = stack.generateLOADVariableAddress(variableName, R5) +
                        generateLOAD(R5.name(), R6) +
                        generateSUB(R6, 1, R4) +
                        generateSTORE(R4, R5.name());
                node.setProperty("variableName", variableName);

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });
        }

        // <lista_argumenata>
        {

            addRule("<lista_argumenata>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);

                checker.run(izraz_pridruzivanja);

                stack.addTmpVariable();

                List<DataType> tipovi = new ArrayList<>();
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));

                node.setProperty("tipovi", tipovi);

                String code = (String) izraz_pridruzivanja.getProperty("kod");
                node.setProperty("kodovi", new ArrayList<>(List.of(code)));
            });

            addRule("<lista_argumenata>", List.of(
                    "<lista_argumenata>",
                    "ZAREZ",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scoper, writer, stack) -> {

                Node lista_argumenata = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                checker.run(lista_argumenata);

                checker.run(izraz_pridruzivanja);

                stack.addTmpVariable();

                List<DataType> tipovi = (List<DataType>) lista_argumenata.getProperty("tipovi");
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));

                node.setProperty("tipovi", tipovi);

                ArrayList<String> codes = (ArrayList<String>) lista_argumenata.getProperty("kodovi");
                String code = (String) izraz_pridruzivanja.getProperty("kod");

                codes.add(code);

                node.setProperty("kodovi", codes);
            });
        }

        // <unarni_izraz>
        {
            addRule("<unarni_izraz>", List.of(
                    "<postfiks_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node postfiks_izraz = (Node) node.getChild(0);

                checker.run(postfiks_izraz);

                if(postfiks_izraz.hasProperty("variableName")){
                    node.setProperty("variableName", postfiks_izraz.getProperty("variableName"));
                }

                node.setProperty("tip", postfiks_izraz.getProperty("tip"));
                node.setProperty("l-izraz", postfiks_izraz.getProperty("l-izraz"));
                node.setProperty("kod", postfiks_izraz.getProperty("kod"));

                if(postfiks_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });


            addRule("<unarni_izraz>", List.of(
                    "OP_INC",
                    "<unarni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node unarni_izraz = (Node) node.getChild(1);

                checker.run(unarni_izraz);

                if (unarni_izraz.getProperty("l-izraz").equals(Boolean.FALSE)
                        || !((DataType) unarni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                String variableName = (String) unarni_izraz.getProperty("variableName");
                String code = stack.generateLOADVariableAddress(variableName, R5) +
                        generateLOAD(R5.name(), R6) +
                        generateADD(R6, 1, R6) +
                        generateSTORE(R6, R5.name());
                node.setProperty("variableName", variableName);

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<unarni_izraz>", List.of(
                    "OP_DEC",
                    "<unarni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node unarni_izraz = (Node) node.getChild(1);

                checker.run(unarni_izraz);

                if (unarni_izraz.getProperty("l-izraz").equals(Boolean.FALSE)
                        || !((DataType) unarni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                String variableName = (String) unarni_izraz.getProperty("variableName");
                String code = stack.generateLOADVariableAddress(variableName, R5) +
                        generateLOAD(R5.name(), R6) +
                        generateSUB(R6, 1, R6) +
                        generateSTORE(R6, R5.name());
                node.setProperty("variableName", variableName);

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<unarni_izraz>", List.of(
                    "<unarni_operator>",
                    "<cast_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node unarni_operator = (Node) node.getChild(0);
                Node cast_izraz = (Node) node.getChild(1);

                checker.run(cast_izraz);

                if (!((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);

                String code = (String) cast_izraz.getProperty("kod");

                switch (unarni_operator.getChild(0).getName()){
                    case "PLUS":
                        break;
                    case "MINUS":
                        code = code + generateMOVE(0, R0);
                        code = code + generateSUB(R0, R6, R6);
                        break;
                    case "OP_TILDA":
                        code = code + generateXOR(R6, -1, R6);
                        break;
                    case "OP_NEG":
                        String lineAddress = generateLineAddress();
                        code = code + generateCMP(R6, 0);
                        code = code + generateJP_EQ(lineAddress);
                        code = code + generateMOVE(1, R6);
                        code = code + lineAddress;
                        code = code + generateXOR(R6, 1, R6);
                        break;
                }

                node.setProperty("kod", code);
            });
        }

        // <unarni_operator>
        {
            /*
            addRule("<unarni_operator>", List.of(
                    "PLUS"
            ), (node, checker, scope) -> {

            });

            addRule("<unarni_operator>", List.of(
                    "MINUS"
            ), (node, checker, scope) -> {

            });

            addRule("<unarni_operator>", List.of(
                    "OP_TILDA"
            ), (node, checker, scope) -> {

            });

            addRule("<unarni_operator>", List.of(
                    "OP_NEG"
            ), (node, checker, scope) -> {

            });
            */
        }

        // <cast_izraz>
        {
            addRule("<cast_izraz>", List.of(
                    "<unarni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node unarni_izraz = (Node) node.getChild(0);

                checker.run(unarni_izraz);

                node.setProperty("tip", unarni_izraz.getProperty("tip"));
                node.setProperty("l-izraz", unarni_izraz.getProperty("l-izraz"));
                node.setProperty("kod", unarni_izraz.getProperty("kod"));
                if(unarni_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<cast_izraz>", List.of(
                    "L_ZAGRADA",
                    "<ime_tipa>",
                    "D_ZAGRADA",
                    "<cast_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node ime_tipa = (Node) node.getChild(1);
                Node cast_izraz = (Node) node.getChild(3);

                checker.run(ime_tipa);
                checker.run(cast_izraz);

                if (!((DataType) cast_izraz.getProperty("tip")).explicitlyCastableTo((DataType) ime_tipa.getProperty("tip")))
                    throw new SemanticException();

                node.setProperty("tip", ime_tipa.getProperty("tip"));
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", cast_izraz.getProperty("kod"));
            });
        }

        // <ime_tipa>
        {
            addRule("<ime_tipa>", List.of(
                    "<specifikator_tipa>"
            ), (node, checker, scope, writer, stack) -> {
                Node specifikator_tipa = (Node) node.getChild(0);

                checker.run(specifikator_tipa);

                node.setProperty("tip", specifikator_tipa.getProperty("tip"));
            });


            addRule("<ime_tipa>", List.of(
                    "KR_CONST",
                    "<specifikator_tipa>"
            ), (node, checker, scope, writer, stack) -> {
                Node specifikator_tipa = (Node) node.getChild(1);

                checker.run(specifikator_tipa);

                if (specifikator_tipa.getProperty("tip") == VOID) throw new SemanticException();

                node.setProperty("tip", constOf((DataType) specifikator_tipa.getProperty("tip")));
                node.setProperty("kod", specifikator_tipa.getProperty("kod"));
            });

        }

        // <specifikator_tipa>
        {

            addRule("<specifikator_tipa>", List.of(
                    "KR_VOID"
            ), (node, checker, scope, writer, stack) -> {
                node.setProperty("tip", VOID);
            });

            addRule("<specifikator_tipa>", List.of(
                    "KR_CHAR"
            ), (node, checker, scope, writer, stack) -> {
                node.setProperty("tip", CHAR);
            });

            addRule("<specifikator_tipa>", List.of(
                    "KR_INT"
            ), (node, checker, scope, writer, stack) -> {
                node.setProperty("tip", INT);
            });
        }

        // <multiplikativni_izraz>
        {
            addRule("<multiplikativni_izraz>", List.of(
                    "<cast_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node cast_izraz = (Node) node.getChild(0);

                checker.run(cast_izraz);

                node.setProperty("tip", cast_izraz.getProperty("tip"));
                node.setProperty("l-izraz", cast_izraz.getProperty("l-izraz"));
                node.setProperty("kod", cast_izraz.getProperty("kod"));
                if(cast_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<multiplikativni_izraz>", List.of(
                    "<multiplikativni_izraz>",
                    "OP_PUTA",
                    "<cast_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node multiplikativni_izraz = (Node) node.getChild(0);
                Node cast_izraz = (Node) node.getChild(2);

                checker.run(multiplikativni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + multiplikativni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(cast_izraz);

                code = code + cast_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateMUL();

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("kod", code);
                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<multiplikativni_izraz>", List.of(
                    "<multiplikativni_izraz>",
                    "OP_DIJELI",
                    "<cast_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node multiplikativni_izraz = (Node) node.getChild(0);
                Node cast_izraz = (Node) node.getChild(2);

                checker.run(multiplikativni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + multiplikativni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(cast_izraz);

                code = code + cast_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateDIV();

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("kod", code);
                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<multiplikativni_izraz>", List.of(
                    "<multiplikativni_izraz>",
                    "OP_MOD",
                    "<cast_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node multiplikativni_izraz = (Node) node.getChild(0);
                Node cast_izraz = (Node) node.getChild(2);

                checker.run(multiplikativni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + multiplikativni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(cast_izraz);

                code = code + cast_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateMOD();

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("kod", code);
                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
        }

        // <aditivni_izraz>
        {
            addRule("<aditivni_izraz>", List.of(
                    "<multiplikativni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node multiplikativni_izraz = (Node) node.getChild(0);

                checker.run(multiplikativni_izraz);

                node.setProperty("tip", multiplikativni_izraz.getProperty("tip"));
                node.setProperty("l-izraz", multiplikativni_izraz.getProperty("l-izraz"));
                node.setProperty("kod", multiplikativni_izraz.getProperty("kod"));
                if(multiplikativni_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<aditivni_izraz>", List.of(
                    "<aditivni_izraz>",
                    "PLUS",
                    "<multiplikativni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node aditivni_izraz = (Node) node.getChild(0);
                Node multiplikativni_izraz = (Node) node.getChild(2);

                checker.run(aditivni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + aditivni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(multiplikativni_izraz);

                code = code + multiplikativni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateADD(R5, R6, R6);

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<aditivni_izraz>", List.of(
                    "<aditivni_izraz>",
                    "MINUS",
                    "<multiplikativni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node aditivni_izraz = (Node) node.getChild(0);
                Node multiplikativni_izraz = (Node) node.getChild(2);

                checker.run(aditivni_izraz);

                String code = "";

                String tmpVariable1 = stack.addTmpVariable();

                code = code + aditivni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(multiplikativni_izraz);

                code = code + multiplikativni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateSUB(R5, R6, R6);

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });
        }

        // <odnosni_izraz>
        {
            addRule("<odnosni_izraz>", List.of(
                    "<aditivni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node aditivni_izraz = (Node) node.getChild(0);

                checker.run(aditivni_izraz);

                node.setProperty("tip", aditivni_izraz.getProperty("tip"));
                node.setProperty("l-izraz", aditivni_izraz.getProperty("l-izraz"));
                node.setProperty("kod", aditivni_izraz.getProperty("kod"));

                if(aditivni_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });
            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_LT",
                    "<aditivni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + odnosni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(aditivni_izraz);

                code = code + aditivni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                code = code + generateCMP(R5, R6);
                code = code + generateJP_SLT(lineAddress1);
                //false
                code = code + generateMOVE(0, R6);
                code = code + generateJP(lineAddress2);
                code = code + lineAddress1 + "\n";
                //true
                code = code + generateMOVE(1, R6);
                code = code + lineAddress2 +"\n";

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });


            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_GT",
                    "<aditivni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + odnosni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(aditivni_izraz);

                code = code + aditivni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                code = code + generateCMP(R5, R6);
                code = code + generateJP_SGT(lineAddress1);
                //false
                code = code + generateMOVE(0, R6);
                code = code + generateJP(lineAddress2);
                code = code + lineAddress1 + "\n";
                //true
                code = code + generateMOVE(1, R6);
                code = code + lineAddress2 +"\n";

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_LTE",
                    "<aditivni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + odnosni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(aditivni_izraz);

                code = code + aditivni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                code = code + generateCMP(R5, R6);
                code = code + generateJP_SLE(lineAddress1);
                //false
                code = code + generateMOVE(0, R6);
                code = code + generateJP(lineAddress2);
                code = code + lineAddress1 + "\n";
                //true
                code = code + generateMOVE(1, R6);
                code = code + lineAddress2 +"\n";

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_GTE",
                    "<aditivni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + odnosni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(aditivni_izraz);

                code = code + aditivni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                code = code + generateCMP(R5, R6);
                code = code + generateJP_SGE(lineAddress1);
                //false
                code = code + generateMOVE(0, R6);
                code = code + generateJP(lineAddress2);
                code = code + lineAddress1 + "\n";
                //true
                code = code + generateMOVE(1, R6);
                code = code + lineAddress2 +"\n";

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

        }

        // <jednakosni_izraz>
        {
            addRule("<jednakosni_izraz>", List.of(
                    "<odnosni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node odnosni_izraz = (Node) node.getChild(0);

                checker.run(odnosni_izraz);

                node.setProperty("tip", odnosni_izraz.getProperty("tip"));
                node.setProperty("l-izraz", odnosni_izraz.getProperty("l-izraz"));
                node.setProperty("kod", odnosni_izraz.getProperty("kod"));
                if(odnosni_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<jednakosni_izraz>", List.of(
                    "<jednakosni_izraz>",
                    "OP_EQ",
                    "<odnosni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node jednakosni_izraz = (Node) node.getChild(0);
                Node odnosni_izraz = (Node) node.getChild(2);


                checker.run(jednakosni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + jednakosni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(odnosni_izraz);

                code = code + odnosni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                code = code + generateCMP(R5, R6);
                code = code + generateJP_EQ(lineAddress1);
                code = code + generateMOVE(0, R6);
                code = code + generateJP(lineAddress2);
                code = code + lineAddress1 + "\n";
                code = code + generateMOVE(1, R6);
                code = code + lineAddress2 +"\n";


                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) jednakosni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

            addRule("<jednakosni_izraz>", List.of(
                    "<jednakosni_izraz>",
                    "OP_NEQ",
                    "<odnosni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node jednakosni_izraz = (Node) node.getChild(0);
                Node odnosni_izraz = (Node) node.getChild(2);

                checker.run(jednakosni_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + jednakosni_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(odnosni_izraz);

                code = code + odnosni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                code = code + generateCMP(R5, R6);
                code = code + generateJP_EQ(lineAddress1);
                code = code + generateMOVE(1, R6);
                code = code + generateJP(lineAddress2);
                code = code + lineAddress1 + "\n";
                code = code + generateMOVE(0, R6);
                code = code + lineAddress2 +"\n";

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) jednakosni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });
        }

        // <bin_i_izraz>
        {
            addRule("<bin_i_izraz>", List.of(
                    "<jednakosni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node jednakosni_izraz = (Node) node.getChild(0);

                checker.run(jednakosni_izraz);

                node.setProperty("tip", jednakosni_izraz.getProperty("tip"));
                node.setProperty("l-izraz", jednakosni_izraz.getProperty("l-izraz"));
                node.setProperty("kod", jednakosni_izraz.getProperty("kod"));

                if(jednakosni_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<bin_i_izraz>", List.of(
                    "<bin_i_izraz>",
                    "OP_BIN_I",
                    "<jednakosni_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node bin_i_izraz = (Node) node.getChild(0);
                Node jednakosni_izraz = (Node) node.getChild(2);

                checker.run(bin_i_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + bin_i_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(jednakosni_izraz);

                code = code + jednakosni_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateAND(R5, R6, R6);

                if (!((DataType) bin_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) jednakosni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });
        }

        // <bin_xili_izraz>
        {
            addRule("<bin_xili_izraz>", List.of(
                    "<bin_i_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node bin_i_izraz = (Node) node.getChild(0);

                checker.run(bin_i_izraz);

                node.setProperty("tip", bin_i_izraz.getProperty("tip"));
                node.setProperty("l-izraz", bin_i_izraz.getProperty("l-izraz"));
                node.setProperty("kod", bin_i_izraz.getProperty("kod"));

                if(bin_i_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<bin_xili_izraz>", List.of(
                    "<bin_xili_izraz>",
                    "OP_BIN_XILI",
                    "<bin_i_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node bin_xili_izraz = (Node) node.getChild(0);
                Node bin_i_izraz = (Node) node.getChild(2);

                checker.run(bin_i_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + bin_i_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(bin_xili_izraz);

                code = code + bin_xili_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateXOR(R5, R6, R6);

                if (!((DataType)bin_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType)bin_xili_izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });
        }

        // <bin_ili_izraz>
        {
            addRule("<bin_ili_izraz>", List.of(
                    "<bin_xili_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node bin_xili_izraz = (Node) node.getChild(0);

                checker.run(bin_xili_izraz);

                node.setProperty("tip", bin_xili_izraz.getProperty("tip"));
                node.setProperty("l-izraz", bin_xili_izraz.getProperty("l-izraz"));
                node.setProperty("kod", bin_xili_izraz.getProperty("kod"));

                if(bin_xili_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<bin_ili_izraz>", List.of(
                    "<bin_ili_izraz>",
                    "OP_BIN_ILI",
                    "<bin_xili_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node bin_ili_izraz = (Node) node.getChild(0);
                Node bin_xili_izraz = (Node) node.getChild(2);

                checker.run(bin_ili_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String code = "";
                code = code + bin_ili_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                checker.run(bin_xili_izraz);

                code = code + bin_xili_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);
                code = code + generateOR(R5, R6, R6);

                if (!((DataType) bin_ili_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) bin_xili_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });

        }

        // <log_i_izraz>
        {
            addRule("<log_i_izraz>", List.of(
                    "<bin_ili_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node bin_ili_izraz = (Node) node.getChild(0);

                checker.run(bin_ili_izraz);

                node.setProperty("tip", bin_ili_izraz.getProperty("tip"));
                node.setProperty("l-izraz", bin_ili_izraz.getProperty("l-izraz"));
                node.setProperty("kod", bin_ili_izraz.getProperty("kod"));

                if(bin_ili_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<log_i_izraz>", List.of(
                    "<log_i_izraz>",
                    "OP_I",
                    "<bin_ili_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node log_i_izraz = (Node) node.getChild(0);
                Node bin_ili_izraz = (Node) node.getChild(2);

                checker.run(log_i_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String skipLine = generateLineAddress();

                String code = "";
                code = code + log_i_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                code = code + generateCMP(R6, 0);
                code = code + generateJP_EQ(skipLine);

                checker.run(bin_ili_izraz);

                code = code + bin_ili_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                code = code + generateCMP(R5, 0);
                code = code + generateJP_EQ(lineAddress1);
                code = code + generateMOVE(1, R5);
                code = code + lineAddress1;

                String lineAddress2 = generateLineAddress();
                code = code + generateCMP(R6, 0);
                code = code + generateJP_EQ(lineAddress2);
                code = code + generateMOVE(1, R6);
                code = code + lineAddress2;

                code = code + generateAND(R5, R6, R6);

                code = code + skipLine;

                if (!((DataType) log_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) bin_ili_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);

                node.setProperty("kod", code);
            });
        }

        // <log_ili_izraz>
        {
            addRule("<log_ili_izraz>", List.of(
                    "<log_i_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node log_i_izraz = (Node) node.getChild(0);

                checker.run(log_i_izraz);

                node.setProperty("tip", log_i_izraz.getProperty("tip"));
                node.setProperty("l-izraz", log_i_izraz.getProperty("l-izraz"));
                node.setProperty("kod", log_i_izraz.getProperty("kod"));

                if(log_i_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<log_ili_izraz>", List.of(
                    "<log_ili_izraz>",
                    "OP_ILI",
                    "<log_i_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node log_ili_izraz = (Node) node.getChild(0);
                Node log_i_izraz = (Node) node.getChild(2);

                checker.run(log_ili_izraz);

                String tmpVariable1 = stack.addTmpVariable();

                String skipLine = generateLineAddress();

                String code = "";
                code = code + log_ili_izraz.getProperty("kod");
                code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateSTORE(R6, R5.name());

                code = code + generateCMP(R6, 0);
                code = code + generateJP_NE(skipLine);

                checker.run(log_i_izraz);

                code = code + log_i_izraz.getProperty("kod");
                code = code + stack.generateLOADVariableAddress(tmpVariable1, R5);
                code = code + generateLOAD(R5.name(), R5);

                String lineAddress1 = generateLineAddress();
                code = code + generateCMP(R5, 0);
                code = code + generateJP_EQ(lineAddress1);
                code = code + generateMOVE(1, R5);
                code = code + lineAddress1;

                String lineAddress2 = generateLineAddress();
                code = code + generateCMP(R6, 0);
                code = code + generateJP_EQ(lineAddress2);
                code = code + generateMOVE(1, R6);
                code = code + lineAddress2;

                code = code + generateOR(R5, R6, R6);

                code = code + skipLine;

                if (!((DataType) log_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) log_ili_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });
        }

        // <izraz_pridruzivanja>
        {
            addRule("<izraz_pridruzivanja>", List.of(
                    "<log_ili_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node log_ili_izraz = (Node) node.getChild(0);

                checker.run(log_ili_izraz);

                node.setProperty("tip", log_ili_izraz.getProperty("tip"));
                node.setProperty("l-izraz", log_ili_izraz.getProperty("l-izraz"));
                node.setProperty("kod", log_ili_izraz.getProperty("kod"));

                if(log_ili_izraz.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }
            });

            addRule("<izraz_pridruzivanja>", List.of(
                    "<postfiks_izraz>",
                    "OP_PRIDRUZI",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node postfiks_izraz = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                checker.run(postfiks_izraz);

                String variableName = (String) postfiks_izraz.getProperty("variableName");
                Variable variable = scope.getVariable(variableName);

                String code = "";
                String tmpVariable = "";
                if(variable.isArray()){
                    String addressCode = (String) postfiks_izraz.getProperty("adresa");

                    tmpVariable = stack.addTmpVariable();

                    code = code + addressCode;
                    code = code + generateSUB(R7, Constants.WORD_LENGTH, R7);
                    code = code + stack.generateLOADVariableAddress(tmpVariable, R5);
                    code = code + generateSTORE(R6, R5.name());

                }

                checker.run(izraz_pridruzivanja);


                if(variable.isArray()){

                    code = code + izraz_pridruzivanja.getProperty("kod");
                    code = code + stack.generateLOADVariableAddress(tmpVariable, R5);
                    code = code + generateLOAD(R5.name(), R5);
                    code = code + generateSTORE(R6, R5.name());

                }else {
                    code = izraz_pridruzivanja.getProperty("kod") +
                            stack.generateLOADVariableAddress(variableName, R5) +
                            generateSTORE(R6, R5.name());
                }

                if (!postfiks_izraz.getProperty("l-izraz").equals(Boolean.TRUE) ||
                        !((DataType) izraz_pridruzivanja.getProperty("tip"))
                                .implicitlyCastableTo((DataType) postfiks_izraz.getProperty("tip")))
                    throw new SemanticException();

                node.setProperty("tip", postfiks_izraz.getProperty("tip"));
                node.setProperty("l-izraz", Boolean.FALSE);

                node.setProperty("kod", code);
            });
        }

        // <izraz>
        {
            addRule("<izraz>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);

                stack.defineTmpScope();

                checker.run(izraz_pridruzivanja);

                int localVariableOffset = stack.getVariableScopeOffset();
                String code = izraz_pridruzivanja.getProperty("kod") +
                        generateADD(R7, localVariableOffset, R7);
                stack.deleteLastTmpScope();

                node.setProperty("tip", izraz_pridruzivanja.getProperty("tip"));
                node.setProperty("l-izraz", izraz_pridruzivanja.getProperty("l-izraz"));
                node.setProperty("kod", code);
            });

            addRule("<izraz>", List.of(
                    "<izraz>",
                    "ZAREZ",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                stack.defineTmpScope();

                checker.run(izraz);

                int localVariableOffset = stack.getVariableScopeOffset();
                String code = izraz.getProperty("kod") +
                        generateADD(R7, localVariableOffset, R7);
                stack.deleteLastTmpScope();

                checker.run(izraz_pridruzivanja);

                code = code + izraz_pridruzivanja.getProperty("kod");

                node.setProperty("tip", izraz_pridruzivanja.getProperty("tip"));
                node.setProperty("l-izraz", Boolean.FALSE);
                node.setProperty("kod", code);
            });
        }

        // <slozena_naredba>
        {
            addRule("<slozena_naredba>", List.of(
                    "L_VIT_ZAGRADA",
                    "<lista_naredbi>",
                    "D_VIT_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_naredbi = (Node) node.getChild(1);

                checker.run(lista_naredbi);

                node.setProperty("kod", lista_naredbi.getProperty("kod"));
            });

            addRule("<slozena_naredba>", List.of(
                    "L_VIT_ZAGRADA",
                    "<lista_deklaracija>",
                    "<lista_naredbi>",
                    "D_VIT_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_deklaracija = (Node) node.getChild(1);
                Node lista_naredbi = (Node) node.getChild(2);

                scope.defineNewScope();

                checker.run(lista_deklaracija);
                checker.run(lista_naredbi);

                scope.exitLastScope();

                String code = (String) lista_deklaracija.getProperty("kod") + lista_naredbi.getProperty("kod");
                node.setProperty("kod", code);
            });
        }

        // <lista_naredbi>
        {
            addRule("<lista_naredbi>", List.of(
                    "<naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node naredba = (Node) node.getChild(0);

                checker.run(naredba);
                node.setProperty("kod", naredba.getProperty("kod"));
            });

            addRule("<lista_naredbi>", List.of(
                    "<lista_naredbi>",
                    "<naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_naredbi = (Node) node.getChild(0);
                Node naredba = (Node) node.getChild(1);

                checker.run(lista_naredbi);
                checker.run(naredba);

                String code = lista_naredbi.getProperty("kod") + "\n" + naredba.getProperty("kod");

                node.setProperty("kod", code);
            });
        }

        // <naredba>
        {
            addRule("<naredba>", List.of(
                    "<slozena_naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node slozena_naredba = (Node) node.getChild(0);

                checker.run(slozena_naredba);

                node.setProperty("kod", slozena_naredba.getProperty("kod"));
            });

            addRule("<naredba>", List.of(
                    "<izraz_naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_naredba = (Node) node.getChild(0);

                checker.run(izraz_naredba);

                node.setProperty("kod", izraz_naredba.getProperty("kod"));
            });

            addRule("<naredba>", List.of(
                    "<naredba_grananja>"
            ), (node, checker, scope, writer, stack) -> {
                Node naredba_grananja = (Node) node.getChild(0);

                checker.run(naredba_grananja);

                node.setProperty("kod", naredba_grananja.getProperty("kod"));
            });


            addRule("<naredba>", List.of(
                    "<naredba_petlje>"
            ), (node, checker, scope, writer, stack) -> {
                Node naredba_petlje = (Node) node.getChild(0);
                checker.run(naredba_petlje);
                node.setProperty("kod", naredba_petlje.getProperty("kod"));
            });


            addRule("<naredba>", List.of(
                    "<naredba_skoka>"
            ), (node, checker, scope, writer, stack) -> {
                Node naredba_skoka = (Node) node.getChild(0);

                checker.run(naredba_skoka);

                node.setProperty("kod", naredba_skoka.getProperty("kod"));
            });
        }

        // <izraz_naredba>
        {
            addRule("<izraz_naredba>", List.of(
                    "TOCKAZAREZ"
            ), (node, checker, scope, writer, stack) -> {
                node.setProperty("tip", INT);
                node.setProperty("kod", "");
            });

            addRule("<izraz_naredba>", List.of(
                    "<izraz>",
                    "TOCKAZAREZ"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz = (Node) node.getChild(0);

                checker.run(izraz);

                node.setProperty("tip", izraz.getProperty("tip"));
                node.setProperty("kod", izraz.getProperty("kod"));
            });
        }

        // <naredba_grananja>
        {
            addRule("<naredba_grananja>", List.of(
                    "KR_IF",
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA",
                    "<naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz = (Node) node.getChild(2);
                Node naredba = (Node) node.getChild(4);

                checker.run(izraz);
                checker.run(naredba);

                if (!((DataType) izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();

                String lineAddress = generateLineAddress();

                String code = izraz.getProperty("kod") +
                        generateCMP(R6, 0) +
                        generateJP_EQ(lineAddress) +
                        naredba.getProperty("kod") +
                        lineAddress + "\n";

                node.setProperty("kod", code);
            });

            addRule("<naredba_grananja>", List.of(
                    "KR_IF",
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA",
                    "<naredba>",
                    "KR_ELSE",
                    "<naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz = (Node) node.getChild(2);
                Node naredba1 = (Node) node.getChild(4);
                Node naredba2 = (Node) node.getChild(6);

                checker.run(izraz);
                checker.run(naredba1);
                checker.run(naredba2);

                if (!((DataType) izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                String code = izraz.getProperty("kod") +
                        generateCMP(R6, 0) +
                        generateJP_EQ(lineAddress1) +
                        naredba1.getProperty("kod") +
                        generateJP(lineAddress2) +
                        lineAddress1 + "\n"+
                        naredba2.getProperty("kod") +
                        lineAddress2 + "\n";

                node.setProperty("kod", code);
            });

        }

        // <naredba_petlje>
        {

            addRule("<naredba_petlje>", List.of(
                    "KR_WHILE",
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA",
                    "<naredba>"
            ), (node, checker, scope, writer, check) -> {
                Node izraz = (Node) node.getChild(2);
                Node naredba = (Node) node.getChild(4);

                checker.run(izraz);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                scope.enterLoop();
                scope.addBreakAddress(lineAddress2);
                scope.addContinueAddress(lineAddress1);

                checker.run(naredba);
                String code = (String) naredba.getProperty("kod");

                code = lineAddress1 + code;

                String izrazCode = (String) izraz.getProperty("kod");


                code = code + izrazCode +
                        generateCMP(R6, 0) +
                        generateJP_EQ(lineAddress2) +
                        generateJP(lineAddress1) +
                        lineAddress2 + "\n";


                scope.exitLoop();

                node.setProperty("kod", code);
                if (!((DataType) izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();
            });

            addRule("<naredba_petlje>", List.of(
                    "KR_FOR",
                    "L_ZAGRADA",
                    "<izraz_naredba>",
                    "<izraz_naredba>",
                    "D_ZAGRADA",
                    "<naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_naredba1 = (Node) node.getChild(2);
                Node izraz_naredba2 = (Node) node.getChild(3);
                Node naredba = (Node) node.getChild(5);

                checker.run(izraz_naredba1);
                checker.run(izraz_naredba2);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();

                scope.enterLoop();
                scope.addBreakAddress(lineAddress2);
                scope.addContinueAddress(lineAddress1);

                checker.run(naredba);

                String code = (String) naredba.getProperty("kod");
                String izrazCode = (String) izraz_naredba1.getProperty("kod");

                code = izrazCode + lineAddress1 + code;

                izrazCode = (String) izraz_naredba2.getProperty("kod");
                if (izrazCode.equals("")){
                    code = code + generateJP(lineAddress1) + lineAddress2 + "\n";
                } else {
                    code = code + izrazCode +
                            generateCMP(R6, 0) +
                            generateJP_EQ(lineAddress2) +
                            generateJP(lineAddress1) +
                            lineAddress2 + "\n";
                }


                scope.exitLoop();

                node.setProperty("kod", code);
                if (!((DataType) izraz_naredba2.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();
            });

            addRule("<naredba_petlje>", List.of(
                    "KR_FOR",
                    "L_ZAGRADA",
                    "<izraz_naredba>",
                    "<izraz_naredba>",
                    "<izraz>",
                    "D_ZAGRADA",
                    "<naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_naredba1 = (Node) node.getChild(2);
                Node izraz_naredba2 = (Node) node.getChild(3);
                Node izraz = (Node) node.getChild(4);
                Node naredba = (Node) node.getChild(6);

                checker.run(izraz_naredba1);
                checker.run(izraz_naredba2);
                checker.run(izraz);

                String lineAddress1 = generateLineAddress();
                String lineAddress2 = generateLineAddress();
                scope.enterLoop();
                scope.addBreakAddress(lineAddress2);
                scope.addContinueAddress(lineAddress1);

                checker.run(naredba);

                String code = (String) naredba.getProperty("kod");
                String izrazCode = (String) izraz_naredba1.getProperty("kod");

                code = izrazCode + lineAddress1 + code + izraz.getProperty("kod");

                izrazCode = (String) izraz_naredba2.getProperty("kod");

                if (izrazCode.equals("")){
                    code = code + generateJP(lineAddress1) + lineAddress2 + "\n";
                }
                else {
                    code = code + izrazCode +
                            generateCMP(R6, 0) +
                            generateJP_EQ(lineAddress2) +
                            generateJP(lineAddress1) +
                            lineAddress2 + "\n";
                }

                scope.exitLoop();

                node.setProperty("kod", code);
                if (!((DataType) izraz_naredba2.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();
            });

        }

        // <naredba_skoka>
        {

            addRule("<naredba_skoka>", List.of(
                    "KR_CONTINUE",
                    "TOCKAZAREZ"
            ), (node, checker, scope, writer, stack) -> {
                if (!scope.inLoop()) throw new SemanticException();
                node.setProperty("kod", generateJP(scope.getContinueAddress()));
            });

            addRule("<naredba_skoka>", List.of(
                    "KR_BREAK",
                    "TOCKAZAREZ"
            ), (node, checker, scope, writer, stack) -> {
                if (!scope.inLoop()) throw new SemanticException();
                node.setProperty("kod", generateJP(scope.getBreakAddress()));
            });

            addRule("<naredba_skoka>", List.of(
                    "KR_RETURN",
                    "TOCKAZAREZ"
            ), (node, checker, scope, writer, stack) -> {
                int localVariableOffset = stack.getVariableScopeOffset();
                String code = generateADD(R7, localVariableOffset, R7) +
                        generateRET();

                node.setProperty("kod", code);
                Function currentFunction = scope.getCurrentFunction();
                if (currentFunction.getReturnType() != VOID) throw new SemanticException();
            });


            addRule("<naredba_skoka>", List.of(
                    "KR_RETURN",
                    "<izraz>",
                    "TOCKAZAREZ"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz = (Node) node.getChild(1);

                checker.run(izraz);

                Function currentFunction = scope.getCurrentFunction();

                if (!((DataType) izraz.getProperty("tip")).implicitlyCastableTo(currentFunction.getReturnType())) {
                    throw new SemanticException();
                }

                int localVariableOffset = stack.getVariableScopeOffset();
                String code = izraz.getProperty("kod") +
                        generateADD(R7, localVariableOffset, R7) +
                        generateRET();

                node.setProperty("kod", code);
            });
        }

        // <prijevodna_jedinica>
        {
            addRule("<prijevodna_jedinica>", List.of(
                    "<vanjska_deklaracija>"
            ), (node, checker, scope, writer, stack) -> {
                Node vanjska_deklaracija = (Node) node.getChild(0);

                checker.run(vanjska_deklaracija);

                node.setProperty("kod", vanjska_deklaracija.getProperty("kod"));
            });

            addRule("<prijevodna_jedinica>", List.of(
                    "<prijevodna_jedinica>",
                    "<vanjska_deklaracija>"
            ), (node, checker, scope, writer, stack) -> {
                Node prijevodna_jedinica = (Node) node.getChild(0);
                Node vanjska_deklaracija = (Node) node.getChild(1);

                checker.run(prijevodna_jedinica);
                checker.run(vanjska_deklaracija);

                String code = prijevodna_jedinica.getProperty("kod").toString() +
                        vanjska_deklaracija.getProperty("kod").toString();

                node.setProperty("kod", code);
            });
        }

        // <vanjska_deklaracija>
        {
            addRule("<vanjska_deklaracija>", List.of(
                    "<definicija_funkcije>"
            ), (node, checker, scope, writer, stack) -> {
                Node definicija_funkcije = (Node) node.getChild(0);

                checker.run(definicija_funkcije);

                node.setProperty("kod", definicija_funkcije.getProperty("kod"));
            });

            addRule("<vanjska_deklaracija>", List.of(
                    "<deklaracija>"
            ), (node, checker, scope, writer, stack) -> {
                Node deklaracija = (Node) node.getChild(0);

                checker.run(deklaracija);

                writer.addGlobalVariableDefinition((String) deklaracija.getProperty("kod"));
                node.setProperty("kod", deklaracija.getProperty("kod"));
            });
        }

        // <definicija_funkcije>
        {
            addRule("<definicija_funkcije>", List.of(
                    "<ime_tipa>",
                    "IDN",
                    "L_ZAGRADA",
                    "KR_VOID",
                    "D_ZAGRADA",
                    "<slozena_naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node ime_tipa = (Node) node.getChild(0);
                Leaf IDN = (Leaf) node.getChild(1);
                Node slozena_naredba = (Node) node.getChild(5);

                checker.run(ime_tipa);

                if (NumericType.isConst((DataType) ime_tipa.getProperty("tip"))) {
                    throw new SemanticException();
                }

                if (scope.functionIsDefined(IDN.getSourceText())) {
                    throw new SemanticException();
                }

                FunctionType functionType = new FunctionType((DataType) ime_tipa.getProperty("tip"));

                Function declaredFunction = scope.getFunction(IDN.getSourceText());
                if (declaredFunction != null) {
                    if (!declaredFunction.getFunctionType().equals(functionType)) {
                        throw new SemanticException();
                    }
                }

                scope.startFunctionDefinition(new Function(IDN.getSourceText(), functionType));

                stack.defineTmpScope();
                checker.run(slozena_naredba);
                stack.deleteLastTmpScope();

                scope.endFunctionDefinition();

                String code = (String) slozena_naredba.getProperty("kod") + generateRET();

                writer.defineFunction(IDN.getSourceText(), code);

                node.setProperty("kod", code);
            });

            addRule("<definicija_funkcije>", List.of(
                    "<ime_tipa>",
                    "IDN",
                    "L_ZAGRADA",
                    "<lista_parametara>",
                    "D_ZAGRADA",
                    "<slozena_naredba>"
            ), (node, checker, scope, writer, stack) -> {
                Node ime_tipa = (Node) node.getChild(0);
                Leaf IDN = (Leaf) node.getChild(1);
                Node lista_parametara = (Node) node.getChild(3);
                Node slozena_naredba = (Node) node.getChild(5);

                checker.run(ime_tipa);

                checker.run(lista_parametara);

                List<DataType> lista_tipova = (List<DataType>) lista_parametara.getProperty("tipovi");
                List<String> lista_imena_tipova = (List<String>) lista_parametara.getProperty("imena");
                FunctionType functionType = new FunctionType((DataType) ime_tipa.getProperty("tip"), lista_tipova.toArray(new DataType[0]));

                scope.startFunctionDefinition(new Function(IDN.getSourceText(), functionType));

                for (int i = 0; i < lista_imena_tipova.size(); i++) {
                    DataType type = lista_tipova.get(i);

                    if(type instanceof ArrayType){
                        stack.addArrayAddress(lista_imena_tipova.get(i));
                    } else {
                        stack.addVariable(lista_imena_tipova.get(i));
                    }

                    scope.declareVariable(new Variable(lista_imena_tipova.get(i),
                            type,
                            false,
                            type instanceof ArrayType
                    ));
                }
                stack.addReturnAddress();

                stack.defineTmpScope();

                checker.run(slozena_naredba);
                String code = (String) slozena_naredba.getProperty("kod") + generateRET();

                stack.deleteLastTmpScope();

                writer.defineFunction(IDN.getSourceText(), code);
                node.setProperty("kod", code);

                stack.removeStackEntries(lista_tipova.size() + 1);

                scope.endFunctionDefinition();
            });

        }

        // <lista_parametara>
        {

            addRule("<lista_parametara>", List.of(
                    "<deklaracija_parametra>"
            ), (node, checker, scope, writer, stack) -> {
                Node deklaracija_parametra = (Node) node.getChild(0);

                checker.run(deklaracija_parametra);

                List<DataType> tipovi = new ArrayList<>();
                tipovi.add((DataType) deklaracija_parametra.getProperty("tip"));
                List<String> imena = new ArrayList<>();
                imena.add((String) deklaracija_parametra.getProperty("ime"));

                node.setProperty("tipovi", tipovi);
                node.setProperty("imena", imena);
            });

            addRule("<lista_parametara>", List.of(
                    "<lista_parametara>",
                    "ZAREZ",
                    "<deklaracija_parametra>"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_parametara = (Node) node.getChild(0);
                Node deklaracija_parametra = (Node) node.getChild(2);

                checker.run(lista_parametara);
                checker.run(deklaracija_parametra);

                List<DataType> tipovi = (List<DataType>) lista_parametara.getProperty("tipovi");
                tipovi.add((DataType) deklaracija_parametra.getProperty("tip"));
                List<String> imena = (List<String>) lista_parametara.getProperty("imena");
                imena.add((String) deklaracija_parametra.getProperty("ime"));

                for (int i = 0; i < imena.size() - 1; i++)
                    if (imena.get(i).equals(imena.get(imena.size() - 1))) throw new SemanticException();

                node.setProperty("tipovi", tipovi);
                node.setProperty("imena", imena);
            });

        }

        // <deklaracija_parametra>
        {

            addRule("<deklaracija_parametra>", List.of(
                    "<ime_tipa>",
                    "IDN"
            ), (node, checker, scope, writer, stack) -> {
                Node ime_tipa = (Node) node.getChild(0);
                Leaf idn = (Leaf) node.getChild(1);

                checker.run(ime_tipa);

                if (ime_tipa.getProperty("tip") == VOID) throw new SemanticException();

                node.setProperty("tip", ime_tipa.getProperty("tip"));
                node.setProperty("ime", idn.getSourceText());
            });

            addRule("<deklaracija_parametra>", List.of(
                    "<ime_tipa>",
                    "IDN",
                    "L_UGL_ZAGRADA",
                    "D_UGL_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node ime_tipa = (Node) node.getChild(0);
                Leaf idn = (Leaf) node.getChild(1);

                checker.run(ime_tipa);

                if (ime_tipa.getProperty("tip") == VOID) throw new SemanticException();

                node.setProperty("tip", ArrayType.of((DataType) ime_tipa.getProperty("tip")));
                node.setProperty("ime", idn.getSourceText());
            });
        }

        // <lista_deklaracija>
        {
            addRule("<lista_deklaracija>", List.of(
                    "<deklaracija>"
            ), (node, checker, scope, writer, stack) -> {
                Node deklaracija = (Node) node.getChild(0);

                checker.run(deklaracija);

                node.setProperty("kod", deklaracija.getProperty("kod"));
            });

            addRule("<lista_deklaracija>", List.of(
                    "<lista_deklaracija>",
                    "<deklaracija>"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_deklaracija = (Node) node.getChild(0);
                Node deklaracija = (Node) node.getChild(1);

                checker.run(lista_deklaracija);
                checker.run(deklaracija);

                String code = (String) lista_deklaracija.getProperty("kod") + deklaracija.getProperty("kod");
                node.setProperty("kod", code);
            });
        }

        // <deklaracija>
        {
            addRule("<deklaracija>", List.of(
                    "<ime_tipa>",
                    "<lista_init_deklaratora>",
                    "TOCKAZAREZ"
            ), (node, checker, scope, writer, stack) -> {
                Node ime_tipa = (Node) node.getChild(0);
                Node lista_init_deklaratora = (Node) node.getChild(1);

                checker.run(ime_tipa);
                lista_init_deklaratora.setProperty("ntip", ime_tipa.getProperty("tip"));
                checker.run(lista_init_deklaratora);

                node.setProperty("kod", lista_init_deklaratora.getProperty("kod"));
            });
        }

        // <lista_init_deklaratora>
        {

            addRule("<lista_init_deklaratora>", List.of(
                    "<init_deklarator>"
            ), (node, checker, scope, writer, stack) -> {
                Node init_deklarator = (Node) node.getChild(0);

                init_deklarator.setProperty("ntip", node.getProperty("ntip"));
                checker.run(init_deklarator);

                node.setProperty("kod", init_deklarator.getProperty("kod"));
            });


            addRule("<lista_init_deklaratora>", List.of(
                    "<lista_init_deklaratora>",
                    "ZAREZ",
                    "<init_deklarator>"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_init_deklaratora = (Node) node.getChild(0);
                Node init_deklarator = (Node) node.getChild(2);

                lista_init_deklaratora.setProperty("ntip", node.getProperty("ntip"));
                init_deklarator.setProperty("ntip", node.getProperty("ntip"));

                checker.run(lista_init_deklaratora);
                checker.run(init_deklarator);

                node.setProperty("kod", (String) lista_init_deklaratora.getProperty("kod") +
                        (String) init_deklarator.getProperty("kod"));
            });

        }

        // <init_deklarator>
        {
            addRule("<init_deklarator>", List.of(
                    "<izravni_deklarator>"
            ), (node, checker, scope, writer, stack) -> {
                Node izravni_deklarator = (Node) node.getChild(0);

                izravni_deklarator.setProperty("ntip", node.getProperty("ntip"));
                checker.run(izravni_deklarator);

                DataType tip = (DataType) izravni_deklarator.getProperty("tip");

                if (tip instanceof NumericType && ((NumericType) tip).isConst()) throw new SemanticException();
                if (tip instanceof ArrayType && ((ArrayType) tip).getNumericType().isConst())
                    throw new SemanticException();

                node.setProperty("kod", izravni_deklarator.getProperty("kod"));
            });

            addRule("<init_deklarator>", List.of(
                    "<izravni_deklarator>",
                    "OP_PRIDRUZI",
                    "<inicijalizator>"
            ), (node, checker, scope, writer, stack) -> {
                Node izravni_deklarator = (Node) node.getChild(0);
                Node inicijalizator = (Node) node.getChild(2);

                izravni_deklarator.setProperty("ntip", node.getProperty("ntip"));
                checker.run(izravni_deklarator);

                if(izravni_deklarator.hasProperty("variable")){
                    inicijalizator.setProperty("variable", izravni_deklarator.getProperty("variable"));
                }

                checker.run(inicijalizator);

                DataType tip_dekl = (DataType) izravni_deklarator.getProperty("tip");

                if (tip_dekl instanceof NumericType) {
                    DataType tip_inic = (DataType) inicijalizator.getProperty("tip");
                    if (!tip_inic.implicitlyCastableTo(tip_dekl)) {
                        throw new SemanticException();
                    }

                } else if (tip_dekl instanceof ArrayType) {

                    int inic_br = (Integer) inicijalizator.getProperty("br-elem");
                    int dekl_br = (Integer) izravni_deklarator.getProperty("br-elem");

                    if (inic_br > dekl_br) {
                        throw new SemanticException();
                    }

                    ArrayType arrayType = (ArrayType) tip_dekl;
                    DataType T = arrayType.getNumericType();
                    List<DataType> tipovi = (List<DataType>) inicijalizator.getProperty("tipovi");
                    for (var tip : tipovi) {
                        if (!tip.implicitlyCastableTo(T)) {
                            throw new SemanticException();
                        }
                    }

                } else {
                    throw new SemanticException();
                }

                Variable variable = (Variable) izravni_deklarator.getProperty("variable");

                String code = (String) izravni_deklarator.getProperty("kod");


                if(inicijalizator.hasProperty("niz")){
                    code = code + stack.generateLOADVariableAddress(variable.getName(), R5);
                }
                code = code + inicijalizator.getProperty("kod");


                if(!variable.isArray()){
                    code = code + stack.generateLOADVariableAddress(variable.getName(), R5) +
                            generateSTORE(R6, R5.name());
                }

                node.setProperty("kod", code);
            });
        }

        // <izravni_deklarator>
        {
            addRule("<izravni_deklarator>", List.of(
                    "IDN"
            ), (node, checker, scope, writer, stack) -> {
                Leaf idn = (Leaf) node.getChild(0);

                DataType tip = (DataType) node.getProperty("ntip");

                node.setProperty("tip", tip);

                Variable variable = new Variable(
                        idn.getSourceText(),
                        tip,
                        tip instanceof NumericType && ((NumericType) tip).isConst(),
                        tip instanceof ArrayType //uvijek false valjda idk
                );

                scope.declareVariable(variable);

                if(!scope.variableIsOnlyGlobal(variable.getName())){
                    stack.addVariable(variable.getName());
                    node.setProperty("kod", generateSUB(R7, 4, R7));
                } else {
                    node.setProperty("kod", "");
                }

                node.setProperty("variable", variable);
            });

            addRule("<izravni_deklarator>", List.of(
                    "IDN",
                    "L_UGL_ZAGRADA",
                    "BROJ",
                    "D_UGL_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Leaf idn = (Leaf) node.getChild(0);
                Leaf broj = (Leaf) node.getChild(2);

                DataType ntip = (DataType) node.getProperty("ntip");
                ArrayType tip = ArrayType.of(ntip);

                node.setProperty("tip", tip);

                if (ntip == VOID) throw new SemanticException();
                if (scope.variableIsDeclared(idn.getSourceText())) throw new SemanticException();

                requireArraySize(broj.getSourceText());

                Variable variable = new Variable(
                        idn.getSourceText(),
                        tip,
                        tip.getNumericType().isConst(),
                        true
                );

                int size = Integer.parseInt(broj.getSourceText());
                variable.setArraySize(size);

                scope.declareVariable(variable);

                if(!scope.variableIsGlobal(variable.getName())){
                    stack.addArray(variable.getName(), size);
                    node.setProperty("kod", generateSUB(R7, 4 * size, R7));
                } else {
                    node.setProperty("kod", "");
                }

                node.setProperty("br-elem", Integer.valueOf(broj.getSourceText()));
                node.setProperty("variable", variable);
            });

            addRule("<izravni_deklarator>", List.of(
                    "IDN",
                    "L_ZAGRADA",
                    "KR_VOID",
                    "D_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Leaf idn = (Leaf) node.getChild(0);

                DataType ntip = (DataType) node.getProperty("ntip");
                FunctionType tip = new FunctionType(ntip);

                if (scope.functionIsDeclared(idn.getSourceText()) &&
                        !scope.getFunction(idn.getSourceText()).getReturnType().equals(tip)) {
                    throw new SemanticException();
                }

                if (!scope.functionIsDeclared(idn.getSourceText())) {
                    scope.declareFunction(new Function(idn.getSourceText(), tip));
                }

                node.setProperty("tip", tip);
                node.setProperty("kod", "");
            });

            addRule("<izravni_deklarator>", List.of(
                    "IDN",
                    "L_ZAGRADA",
                    "<lista_parametara>",
                    "D_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Leaf idn = (Leaf) node.getChild(0);
                Node lista_parametara = (Node) node.getChild(2);

                checker.run(lista_parametara);

                DataType ntip = (DataType) node.getProperty("ntip");
                List<DataType> tipovi = (List<DataType>) lista_parametara.getProperty("tipovi");

                FunctionType tip = new FunctionType(ntip, tipovi.toArray(new DataType[0]));

                if (scope.functionIsDeclaredLocally(idn.getSourceText()) &&
                        !scope.getFunction(idn.getSourceText()).getReturnType().equals(tip)) {
                    throw new SemanticException();
                }

                if (!scope.functionIsDeclaredLocally(idn.getSourceText())) {
                    scope.declareFunction(new Function(idn.getSourceText(), tip));
                }

                node.setProperty("tip", tip);
                node.setProperty("kod", "");
            });
        }

        // <inicijalizator>
        {
            addRule("<inicijalizator>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);

                stack.defineTmpScope();

                checker.run(izraz_pridruzivanja);

                int localVariableOffset = stack.getVariableScopeOffset();
                String code = izraz_pridruzivanja.getProperty("kod") +
                        generateADD(R7, localVariableOffset, R7);
                stack.deleteLastTmpScope();

                int charArraySize = TreeUtil.charArraySize(node);

                if (charArraySize > 0) {
                    node.setProperty("br-elem", charArraySize + 1);
                    List<DataType> tipovi = new ArrayList<>();
                    for (int i = 0; i < charArraySize + 1; i++) tipovi.add(CHAR);
                    node.setProperty("tipovi", tipovi);
                } else {
                    node.setProperty("tip", izraz_pridruzivanja.getProperty("tip"));
                }

                if(izraz_pridruzivanja.hasProperty("niz")) {
                    node.setProperty("niz", true);
                }

                node.setProperty("kod", code);
            });

            addRule("<inicijalizator>", List.of(
                    "L_VIT_ZAGRADA",
                    "<lista_izraza_pridruzivanja>",
                    "D_VIT_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_izraza_pridruzivanja = (Node) node.getChild(1);
                checker.run(lista_izraza_pridruzivanja);

                List<String> codes = (List<String>) lista_izraza_pridruzivanja.getProperty("kodovi");

                Variable variable = (Variable) node.getProperty("variable");
                String code = stack.generateLOADVariableAddress(variable.getName(), R3);

                for(int i = 0; i < codes.size(); i++){
                    code = code + codes.get(i);
                    code = code + generateSTORE(R6, R3.name());
                    code = code + generateADD(R3, 4, R3);
                }

                node.setProperty("br-elem", lista_izraza_pridruzivanja.getProperty("br-elem"));
                node.setProperty("tipovi", lista_izraza_pridruzivanja.getProperty("tipovi"));
                node.setProperty("kod", code);
            });

        }

        // <lista_izraza_pridruzivanja>
        {
            addRule("<lista_izraza_pridruzivanja>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);

                stack.defineTmpScope();

                checker.run(izraz_pridruzivanja);

                int localVariableOffset = stack.getVariableScopeOffset();
                String code = izraz_pridruzivanja.getProperty("kod") +
                        generateADD(R7, localVariableOffset, R7);
                stack.deleteLastTmpScope();

                List<DataType> tipovi = new ArrayList<>();
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));
                node.setProperty("tipovi", tipovi);
                node.setProperty("br-elem", 1);

                List<String> codes = new ArrayList<>(List.of(code));

                node.setProperty("kodovi", codes);
            });

            addRule("<lista_izraza_pridruzivanja>", List.of(
                    "<lista_izraza_pridruzivanja>",
                    "ZAREZ",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_izraza_pridruzivanja = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                checker.run(lista_izraza_pridruzivanja);

                stack.defineTmpScope();

                checker.run(izraz_pridruzivanja);

                int localVariableOffset = stack.getVariableScopeOffset();
                String code = izraz_pridruzivanja.getProperty("kod") +
                        generateADD(R7, localVariableOffset, R7);
                stack.deleteLastTmpScope();

                List<DataType> tipovi = (List<DataType>) lista_izraza_pridruzivanja.getProperty("tipovi");
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));
                int br = (Integer) lista_izraza_pridruzivanja.getProperty("br-elem");

                List<String> codes = (List<String>) lista_izraza_pridruzivanja.getProperty("kodovi");
                codes.add(code);

                node.setProperty("tipovi", tipovi);
                node.setProperty("br-elem", br + 1);
                node.setProperty("kodovi", codes);
            });
        }

    }



}
