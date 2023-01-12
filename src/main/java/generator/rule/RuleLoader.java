package generator.rule;

import generator.generator.CodeGenerator;
import generator.generator.Register;
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

                String variableAddress = stack.getVariableAddress(variableName);

                String code = generateLOAD(variableAddress, R6);

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

            /*
            addRule("<primarni_izraz>", List.of(
                    "ZNAK"
            ), (node, checker, scope, writer) -> {
                Leaf ZNAK = (Leaf) node.getChild(0);

                requireValidChar(ZNAK.getSourceText());

                node.setProperty("tip", CHAR);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<primarni_izraz>", List.of(
                    "NIZ_ZNAKOVA"
            ), (node, checker, scope, writer) -> {
                Leaf NIZ_ZNAKOVA = (Leaf) node.getChild(0);

                requireValidString(NIZ_ZNAKOVA.getSourceText());

                node.setProperty("tip", ArrayType.of(CHAR));
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<primarni_izraz>", List.of(
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA"
            ), (node, checker, scope, writer) -> {
                Node izraz = (Node) node.getChild(1);

                checker.run(izraz);

                node.setProperty("tip", izraz.getProperty("tip"));
                node.setProperty("l-izraz", izraz.getProperty("l-izraz"));
            });

             */
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
            });
            /*
            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "L_UGL_ZAGRADA",
                    "<izraz>",
                    "D_UGL_ZAGRADA"
            ), (node, checker, scope) -> {
                Node postfiks_izraz = (Node) node.getChild(0);
                Node izraz = (Node) node.getChild(2);

                checker.run(postfiks_izraz);
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
            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                     "L_ZAGRADA",
                    "D_ZAGRADA"
            ), (node, checker, scope) -> {
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
            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "L_ZAGRADA",
                    "<lista_argumenata>",
                    "D_ZAGRADA"
            ), (node, checker, scope) -> {

                Node postfiks_izraz = (Node) node.getChild(0);
                Node lista_argumenata = (Node) node.getChild(2);

                checker.run(postfiks_izraz);
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

                node.setProperty("tip", functionType.getReturnType());
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "OP_INC"
            ), (node, checker, scope) -> {

                Node postfiks_izraz = (Node) node.getChild(0);
                checker.run(postfiks_izraz);

                if (!(Boolean) postfiks_izraz.getProperty("l-izraz").equals(Boolean.TRUE)) throw new SemanticException();
                if (postfiks_izraz.getProperty("tip") != INT) throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<postfiks_izraz>", List.of(
                    "<postfiks_izraz>",
                    "OP_DEC"
            ), (node, checker, scope) -> {

                Node postfiks_izraz = (Node) node.getChild(0);
                checker.run(postfiks_izraz);

                if (!(Boolean) postfiks_izraz.getProperty("l-izraz").equals(Boolean.TRUE)) throw new SemanticException();
                if (postfiks_izraz.getProperty("tip") != INT) throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
        }

        // <lista_argumenata>
        {
            /*
            addRule("<lista_argumenata>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scoper) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);
                checker.run(izraz_pridruzivanja);

                List<DataType> tipovi = new ArrayList<>();
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));

                node.setProperty("tipovi", tipovi);

            });

            addRule("<lista_argumenata>", List.of(
                    "<lista_argumenata>",
                    "ZAREZ",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scoper) -> {

                Node lista_argumenata = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                checker.run(izraz_pridruzivanja);
                checker.run(lista_argumenata);

                List<DataType> tipovi = (List<DataType>) lista_argumenata.getProperty("tipovi");
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));

                node.setProperty("tipovi", tipovi);
            });
             */
        }

        // <unarni_izraz>
        {
            addRule("<unarni_izraz>", List.of(
                    "<postfiks_izraz>"
            ), (node, checker, scope, writer, stack) -> {
                Node postfiks_izraz = (Node) node.getChild(0);

                checker.run(postfiks_izraz);

                node.setProperty("tip", postfiks_izraz.getProperty("tip"));
                node.setProperty("l-izraz", postfiks_izraz.getProperty("l-izraz"));
                node.setProperty("kod", postfiks_izraz.getProperty("kod"));
            });

            /*

            addRule("<unarni_izraz>", List.of(
                    "OP_INC",
                    "<unarni_izraz>"
            ), (node, checker, scope) -> {
                Node unarni_izraz = (Node) node.getChild(1);

                checker.run(unarni_izraz);

                if (unarni_izraz.getProperty("l-izraz").equals(Boolean.FALSE)
                        || !((DataType) unarni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<unarni_izraz>", List.of(
                    "OP_DEC",
                    "<unarni_izraz>"
            ), (node, checker, scope) -> {
                Node unarni_izraz = (Node) node.getChild(1);

                checker.run(unarni_izraz);

                if (unarni_izraz.getProperty("l-izraz").equals(Boolean.FALSE)
                        || !((DataType) unarni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<unarni_izraz>", List.of(
                    "<unarni_operator>",
                    "<cast_izraz>"
            ), (node, checker, scope) -> {
                Node cast_izraz = (Node) node.getChild(1);

                checker.run(cast_izraz);

                if (!((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });

            /*
            addRule("<cast_izraz>", List.of(
                    "L_ZAGRADA",
                    "<ime_tipa>",
                    "D_ZAGRADA",
                    "<cast_izraz>"
            ), (node, checker, scope) -> {
                Node ime_tipa = (Node) node.getChild(1);
                Node cast_izraz = (Node) node.getChild(3);

                checker.run(ime_tipa);
                checker.run(cast_izraz);

                if (!((DataType) cast_izraz.getProperty("tip")).explicitlyCastableTo((DataType) ime_tipa.getProperty("tip")))
                    throw new SemanticException();

                node.setProperty("tip", ime_tipa.getProperty("tip"));
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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

            /*
            addRule("<ime_tipa>", List.of(
                    "KR_CONST",
                    "<specifikator_tipa>"
            ), (node, checker, scope) -> {
                Node specifikator_tipa = (Node) node.getChild(1);

                checker.run(specifikator_tipa);

                if (specifikator_tipa.getProperty("tip") == VOID) throw new SemanticException();

                node.setProperty("tip", constOf((DataType) specifikator_tipa.getProperty("tip")));
            });
            */
        }

        // <specifikator_tipa>
        {
            /*
            addRule("<specifikator_tipa>", List.of(
                    "KR_VOID"
            ), (node, checker, scope) -> {
                node.setProperty("tip", VOID);
            });

            addRule("<specifikator_tipa>", List.of(
                    "KR_CHAR"
            ), (node, checker, scope) -> {
                node.setProperty("tip", CHAR);
            });
             */

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
            });

            /*
            addRule("<multiplikativni_izraz>", List.of(
                    "<multiplikativni_izraz>",
                    "OP_PUTA",
                    "<cast_izraz>"
            ), (node, checker, scope) -> {
                Node multiplikativni_izraz = (Node) node.getChild(0);
                Node cast_izraz = (Node) node.getChild(2);

                checker.run(multiplikativni_izraz);
                checker.run(cast_izraz);

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<multiplikativni_izraz>", List.of(
                    "<multiplikativni_izraz>",
                    "OP_DIJELI",
                    "<cast_izraz>"
            ), (node, checker, scope) -> {
                Node multiplikativni_izraz = (Node) node.getChild(0);
                Node cast_izraz = (Node) node.getChild(2);

                checker.run(multiplikativni_izraz);
                checker.run(cast_izraz);

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<multiplikativni_izraz>", List.of(
                    "<multiplikativni_izraz>",
                    "OP_MOD",
                    "<cast_izraz>"
            ), (node, checker, scope) -> {
                Node multiplikativni_izraz = (Node) node.getChild(0);
                Node cast_izraz = (Node) node.getChild(2);

                checker.run(multiplikativni_izraz);
                checker.run(cast_izraz);

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) cast_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });

            /*
            addRule("<aditivni_izraz>", List.of(
                    "<aditivni_izraz>",
                    "PLUS",
                    "<multiplikativni_izraz>"
            ), (node, checker, scope) -> {
                Node aditivni_izraz = (Node) node.getChild(0);
                Node multiplikativni_izraz = (Node) node.getChild(2);

                checker.run(aditivni_izraz);
                checker.run(multiplikativni_izraz);

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<aditivni_izraz>", List.of(
                    "<aditivni_izraz>",
                    "MINUS",
                    "<multiplikativni_izraz>"
            ), (node, checker, scope) -> {
                Node aditivni_izraz = (Node) node.getChild(0);
                Node multiplikativni_izraz = (Node) node.getChild(2);

                checker.run(aditivni_izraz);
                checker.run(multiplikativni_izraz);

                if (!((DataType) multiplikativni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });
            /*
            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_LT",
                    "<aditivni_izraz>"
            ), (node, checker, scope) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);
                checker.run(aditivni_izraz);

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_GT",
                    "<aditivni_izraz>"
            ), (node, checker, scope) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);
                checker.run(aditivni_izraz);

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_LTE",
                    "<aditivni_izraz>"
            ), (node, checker, scope) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);
                checker.run(aditivni_izraz);

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<odnosni_izraz>", List.of(
                    "<odnosni_izraz>",
                    "OP_GTE",
                    "<aditivni_izraz>"
            ), (node, checker, scope) -> {
                Node odnosni_izraz = (Node) node.getChild(0);
                Node aditivni_izraz = (Node) node.getChild(2);

                checker.run(odnosni_izraz);
                checker.run(aditivni_izraz);

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) aditivni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });
            /*

            addRule("<jednakosni_izraz>", List.of(
                    "<jednakosni_izraz>",
                    "OP_EQ",
                    "<odnosni_izraz>"
            ), (node, checker, scope) -> {
                Node jednakosni_izraz = (Node) node.getChild(0);
                Node odnosni_izraz = (Node) node.getChild(2);

                checker.run(jednakosni_izraz);
                checker.run(odnosni_izraz);

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) jednakosni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });

            addRule("<jednakosni_izraz>", List.of(
                    "<jednakosni_izraz>",
                    "OP_NEQ",
                    "<odnosni_izraz>"
            ), (node, checker, scope) -> {
                Node jednakosni_izraz = (Node) node.getChild(0);
                Node odnosni_izraz = (Node) node.getChild(2);

                checker.run(jednakosni_izraz);
                checker.run(odnosni_izraz);

                if (!((DataType) odnosni_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) jednakosni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });

            /*
            addRule("<bin_i_izraz>", List.of(
                    "<bin_i_izraz>",
                    "OP_BIN_I",
                    "<jednakosni_izraz>"
            ), (node, checker, scope) -> {
                Node bin_i_izraz = (Node) node.getChild(0);
                Node jednakosni_izraz = (Node) node.getChild(2);

                checker.run(bin_i_izraz);
                checker.run(jednakosni_izraz);

                if (!((DataType) bin_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) jednakosni_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });
            /*
            addRule("<bin_xili_izraz>", List.of(
                    "<bin_xili_izraz>",
                    "OP_BIN_XILI",
                    "<bin_i_izraz>"
            ), (node, checker, scope) -> {
                Node bin_xili_izraz = (Node) node.getChild(0);
                Node bin_i_izraz = (Node) node.getChild(2);

                checker.run(bin_i_izraz);
                checker.run(bin_xili_izraz);

                if (!((DataType)bin_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType)bin_xili_izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });

            /*
            addRule("<bin_ili_izraz>", List.of(
                    "<bin_ili_izraz>",
                    "OP_BIN_ILI",
                    "<bin_xili_izraz>"
            ), (node, checker, scope) -> {
                Node bin_ili_izraz = (Node) node.getChild(0);
                Node bin_xili_izraz = (Node) node.getChild(2);

                checker.run(bin_ili_izraz);
                checker.run(bin_xili_izraz);

                if (!((DataType) bin_ili_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) bin_xili_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });

            /*
            addRule("<log_i_izraz>", List.of(
                    "<log_i_izraz>",
                    "OP_I",
                    "<bin_ili_izraz>"
            ), (node, checker, scope) -> {
                Node log_i_izraz = (Node) node.getChild(0);
                Node bin_ili_izraz = (Node) node.getChild(2);

                checker.run(log_i_izraz);
                checker.run(bin_ili_izraz);

                if (!((DataType) log_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) bin_ili_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });

            /*
            addRule("<log_ili_izraz>", List.of(
                    "<log_ili_izraz>",
                    "OP_ILI",
                    "<log_i_izraz>"
            ), (node, checker, scope) -> {
                Node log_ili_izraz = (Node) node.getChild(0);
                Node log_i_izraz = (Node) node.getChild(2);

                checker.run(log_ili_izraz);
                checker.run(log_i_izraz);

                if (!((DataType) log_i_izraz.getProperty("tip")).implicitlyCastableTo(INT) ||
                        !((DataType) log_ili_izraz.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();

                node.setProperty("tip", INT);
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
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
            });

            /*
            addRule("<izraz_pridruzivanja>", List.of(
                    "<postfiks_izraz>",
                    "OP_PRIDRUZI",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope) -> {
                Node postfiks_izraz = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                checker.run(postfiks_izraz);
                checker.run(izraz_pridruzivanja);

                if (!postfiks_izraz.getProperty("l-izraz").equals(Boolean.TRUE) ||
                        !((DataType) izraz_pridruzivanja.getProperty("tip"))
                                .implicitlyCastableTo((DataType) postfiks_izraz.getProperty("tip")))
                    throw new SemanticException();

                node.setProperty("tip", postfiks_izraz.getProperty("tip"));
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
        }

        // <izraz>
        {
            addRule("<izraz>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);

                checker.run(izraz_pridruzivanja);

                node.setProperty("tip", izraz_pridruzivanja.getProperty("tip"));
                node.setProperty("l-izraz", izraz_pridruzivanja.getProperty("l-izraz"));
                node.setProperty("kod", izraz_pridruzivanja.getProperty("kod"));
            });

            /*
            addRule("<izraz>", List.of(
                    "<izraz>",
                    "ZAREZ",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope) -> {
                Node izraz = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                checker.run(izraz);
                checker.run(izraz_pridruzivanja);

                node.setProperty("tip", izraz_pridruzivanja.getProperty("tip"));
                node.setProperty("l-izraz", Boolean.FALSE);
            });
            */
        }

        // <slozena_naredba>
        {
            addRule("<slozena_naredba>", List.of(
                    "L_VIT_ZAGRADA",
                    "<lista_naredbi>",
                    "D_VIT_ZAGRADA"
            ), (node, checker, scope, writer, stack) -> {
                Node lista_naredbi = (Node) node.getChild(1);

                scope.defineNewScope();

                checker.run(lista_naredbi);

                scope.exitLastScope();

                node.setProperty("kod", lista_naredbi.getProperty("kod"));
            });

            /*
            addRule("<slozena_naredba>", List.of(
                    "L_VIT_ZAGRADA",
                    "<lista_deklaracija>",
                    "<lista_naredbi>",
                    "D_VIT_ZAGRADA"
            ), (node, checker, scope) -> {
                Node lista_deklaracija = (Node) node.getChild(1);
                Node lista_naredbi = (Node) node.getChild(2);

                scope.defineNewScope();

                checker.run(lista_deklaracija);
                checker.run(lista_naredbi);

                scope.exitLastScope();
            });
            */
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
            /*
            addRule("<naredba>", List.of(
                    "<slozena_naredba>"
            ), (node, checker, scope) -> {
                Node slozena_naredba = (Node) node.getChild(0);

                checker.run(slozena_naredba);
            });

            addRule("<naredba>", List.of(
                    "<izraz_naredba>"
            ), (node, checker, scope) -> {
                Node izraz_naredba = (Node) node.getChild(0);

                checker.run(izraz_naredba);
            });

            addRule("<naredba>", List.of(
                    "<naredba_grananja>"
            ), (node, checker, scope) -> {
                Node naredba_grananja = (Node) node.getChild(0);

                checker.run(naredba_grananja);
            });

            addRule("<naredba>", List.of(
                    "<naredba_petlje>"
            ), (node, checker, scope) -> {
                Node naredba_petlje = (Node) node.getChild(0);

                checker.run(naredba_petlje);
            });
            */

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
            /*
            addRule("<izraz_naredba>", List.of(
                    "TOCKAZAREZ"
            ), (node, checker, scope) -> {
                node.setProperty("tip", INT);
            });

            addRule("<izraz_naredba>", List.of(
                    "<izraz>",
                    "TOCKAZAREZ"
            ), (node, checker, scope) -> {
                Node izraz = (Node) node.getChild(0);

                checker.run(izraz);

                node.setProperty("tip", izraz.getProperty("tip"));
            });
            */
        }

        // <naredba_grananja>
        {
            /*
            addRule("<naredba_grananja>", List.of(
                    "KR_IF",
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA",
                    "<naredba>"
            ), (node, checker, scope) -> {
                Node izraz = (Node) node.getChild(2);
                Node naredba = (Node) node.getChild(4);

                checker.run(izraz);
                checker.run(naredba);

                if (!((DataType) izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();
            });

            addRule("<naredba_grananja>", List.of(
                    "KR_IF",
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA",
                    "<naredba>",
                    "KR_ELSE",
                    "<naredba>"
            ), (node, checker, scope) -> {
                Node izraz = (Node) node.getChild(2);
                Node naredba1 = (Node) node.getChild(4);
                Node naredba2 = (Node) node.getChild(6);

                checker.run(izraz);
                checker.run(naredba1);
                checker.run(naredba2);

                if (!((DataType) izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();
            });
            */
        }

        // <naredba_petlje>
        {
            /*
            addRule("<naredba_petlje>", List.of(
                    "KR_WHILE",
                    "L_ZAGRADA",
                    "<izraz>",
                    "D_ZAGRADA",
                    "<naredba>"
            ), (node, checker, scope) -> {
                Node izraz = (Node) node.getChild(2);
                Node naredba = (Node) node.getChild(4);

                checker.run(izraz);

                scope.enterLoop();
                checker.run(naredba);
                scope.exitLoop();

                if (!((DataType) izraz.getProperty("tip")).implicitlyCastableTo(INT)) throw new SemanticException();
            });

            addRule("<naredba_petlje>", List.of(
                    "KR_FOR",
                    "L_ZAGRADA",
                    "<izraz_naredba>",
                    "<izraz_naredba>",
                    "D_ZAGRADA",
                    "<naredba>"
            ), (node, checker, scope) -> {
                Node izraz_naredba1 = (Node) node.getChild(2);
                Node izraz_naredba2 = (Node) node.getChild(3);
                Node naredba = (Node) node.getChild(5);

                checker.run(izraz_naredba1);
                checker.run(izraz_naredba2);

                scope.enterLoop();
                checker.run(naredba);
                scope.exitLoop();

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
            ), (node, checker, scope) -> {
                Node izraz_naredba1 = (Node) node.getChild(2);
                Node izraz_naredba2 = (Node) node.getChild(3);
                Node izraz = (Node) node.getChild(4);
                Node naredba = (Node) node.getChild(6);

                checker.run(izraz_naredba1);
                checker.run(izraz_naredba2);
                checker.run(izraz);

                scope.enterLoop();
                checker.run(naredba);
                scope.exitLoop();

                if (!((DataType) izraz_naredba2.getProperty("tip")).implicitlyCastableTo(INT))
                    throw new SemanticException();
            });
            */
        }

        // <naredba_skoka>
        {
            /*
            addRule("<naredba_skoka>", List.of(
                    "KR_CONTINUE",
                    "TOCKAZAREZ"
            ), (node, checker, scope) -> {
                if (!scope.inLoop()) throw new SemanticException();
            });

            addRule("<naredba_skoka>", List.of(
                    "KR_BREAK",
                    "TOCKAZAREZ"
            ), (node, checker, scope) -> {
                if (!scope.inLoop()) throw new SemanticException();
            });

            addRule("<naredba_skoka>", List.of(
                    "KR_RETURN",
                    "TOCKAZAREZ"
            ), (node, checker, scope) -> {
                Function currentFunction = scope.getCurrentFunction();
                if (currentFunction.getReturnType() != VOID) throw new SemanticException();
            });
            */

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

                String code = izraz.getProperty("kod") + generateRET();

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

                checker.run(slozena_naredba);

                scope.endFunctionDefinition();

                writer.defineFunction(IDN.getSourceText(), (String) slozena_naredba.getProperty("kod"));

                node.setProperty("kod", slozena_naredba.getProperty("kod"));
            });
            /*
            addRule("<definicija_funkcije>", List.of(
                    "<ime_tipa>",
                    "IDN",
                    "L_ZAGRADA",
                    "<lista_parametara>",
                    "D_ZAGRADA",
                    "<slozena_naredba>"
            ), (node, checker, scope) -> {
                Node ime_tipa = (Node) node.getChild(0);
                Leaf IDN = (Leaf) node.getChild(1);
                Node lista_parametara = (Node) node.getChild(3);
                Node slozena_naredba = (Node) node.getChild(5);

                checker.run(ime_tipa);

                if (NumericType.isConst((DataType) ime_tipa.getProperty("tip"))) {
                    throw new SemanticException();
                }

                checker.run(lista_parametara);

                if (scope.isDeclaredGlobally(IDN.getSourceText())) {
                    Function declaredFunction = scope.getGloballyDeclaredFunction(IDN.getSourceText());

                    if (!declaredFunction.getReturnType().equals(
                            new FunctionType((DataType) ime_tipa.getProperty("tip"),
                                    (DataType) lista_parametara.getProperty("tipovi")))) {
                        throw new SemanticException();
                    }

                }


                List<DataType> lista_tipova = (List<DataType>) lista_parametara.getProperty("tipovi");
                List<String> lista_imena_tipova = (List<String>) lista_parametara.getProperty("imena");
                FunctionType functionType = new FunctionType((DataType) ime_tipa.getProperty("tip"), lista_tipova.toArray(new DataType[0]));

                scope.startFunctionDefinition(new Function(IDN.getSourceText(), functionType));

                for (int i = 0; i < lista_tipova.size(); i++) {
                    scope.declareVariable(new Variable(lista_imena_tipova.get(i),
                            lista_tipova.get(i),
                            false,
                            false
                    ));
                }

                checker.run(slozena_naredba);

                scope.endFunctionDefinition();
            });
            */
        }

        // <lista_parametara>
        {
            /*
            addRule("<lista_parametara>", List.of(
                    "<deklaracija_parametra>"
            ), (node, checker, scope) -> {
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
            ), (node, checker, scope) -> {
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
            */
        }

        // <deklaracija_parametra>
        {
            /*
            addRule("<deklaracija_parametra>", List.of(
                    "<ime_tipa>",
                    "IDN"
            ), (node, checker, scope) -> {
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
            ), (node, checker, scope) -> {
                Node ime_tipa = (Node) node.getChild(0);
                Leaf idn = (Leaf) node.getChild(1);

                checker.run(ime_tipa);

                if (ime_tipa.getProperty("tip") == VOID) throw new SemanticException();

                node.setProperty("tip", ArrayType.of((DataType) ime_tipa.getProperty("tip")));
                node.setProperty("ime", idn.getSourceText());
            });
            */
        }

        // <lista_deklaracija>
        {
            /*
            addRule("<lista_deklaracija>", List.of(
                    "<deklaracija>"
            ), (node, checker, scope) -> {
                Node deklaracija = (Node) node.getChild(0);

                checker.run(deklaracija);
            });

            addRule("<lista_deklaracija>", List.of(
                    "<lista_deklaracija>",
                    "<deklaracija>"
            ), (node, checker, scope) -> {
                Node lista_deklaracija = (Node) node.getChild(0);
                Node deklaracija = (Node) node.getChild(1);

                checker.run(lista_deklaracija);
                checker.run(deklaracija);
            });
            */
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
                checker.run(inicijalizator);

                DataType tip_dekl = (DataType) izravni_deklarator.getProperty("tip");
                DataType tip_inic = (DataType) inicijalizator.getProperty("tip");

                if (tip_dekl instanceof NumericType) {

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

                String code = (String) izravni_deklarator.getProperty("kod") +
                        (String) inicijalizator.getProperty("kod") +
                        generateSTORE(R6, stack.getVariableAddress(variable.getName()));

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

                if (tip == VOID) throw new SemanticException();
                if (scope.variableIsDeclared(idn.getSourceText())) throw new SemanticException();

                Variable variable = new Variable(
                        idn.getSourceText(),
                        tip,
                        tip instanceof NumericType && ((NumericType) tip).isConst(),
                        tip instanceof ArrayType //uvijek false valjda idk
                );

                scope.declareVariable(variable);

                if(!scope.variableIsGlobal(variable.getName())){
                    throw new UnsupportedOperationException();
                } else {
                    node.setProperty("kod", "");
                }
                node.setProperty("variable", variable);
            });
            /*
            addRule("<izravni_deklarator>", List.of(
                    "IDN",
                    "L_UGL_ZAGRADA",
                    "BROJ",
                    "D_UGL_ZAGRADA"
            ), (node, checker, scope) -> {
                Leaf idn = (Leaf) node.getChild(0);
                Leaf broj = (Leaf) node.getChild(2);

                DataType ntip = (DataType) node.getProperty("ntip");
                ArrayType tip = ArrayType.of(ntip);

                node.setProperty("tip", tip);

                if (ntip == VOID) throw new SemanticException();
                if (scope.variableIsDeclared(idn.getSourceText())) throw new SemanticException();

                requireArraySize(broj.getSourceText());

                scope.declareVariable(new Variable(
                        idn.getSourceText(),
                        tip,
                        tip.getNumericType().isConst(),
                        true
                ));

                node.setProperty("br-elem", Integer.valueOf(broj.getSourceText()));
            });

            addRule("<izravni_deklarator>", List.of(
                    "IDN",
                    "L_ZAGRADA",
                    "KR_VOID",
                    "D_ZAGRADA"
            ), (node, checker, scope) -> {
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
            });

            addRule("<izravni_deklarator>", List.of(
                    "IDN",
                    "L_ZAGRADA",
                    "<lista_parametara>",
                    "D_ZAGRADA"
            ), (node, checker, scope) -> {
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
            });
            */
        }

        // <inicijalizator>
        {
            addRule("<inicijalizator>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope, writer, stack) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);
                checker.run(izraz_pridruzivanja);

                int charArraySize = TreeUtil.charArraySize(node);

                if (charArraySize > 0) {
                    node.setProperty("br-elem", charArraySize + 1);
                    List<DataType> tipovi = new ArrayList<>();
                    for (int i = 0; i < charArraySize + 1; i++) tipovi.add(CHAR);
                    node.setProperty("tipovi", tipovi);
                } else {
                    node.setProperty("tip", izraz_pridruzivanja.getProperty("tip"));
                }

                node.setProperty("kod", izraz_pridruzivanja.getProperty("kod"));
            });
            /*
            addRule("<inicijalizator>", List.of(
                    "L_VIT_ZAGRADA",
                    "<lista_izraza_pridruzivanja>",
                    "D_VIT_ZAGRADA"
            ), (node, checker, scope) -> {
                Node lista_izraza_pridruzivanja = (Node) node.getChild(1);
                checker.run(lista_izraza_pridruzivanja);

                node.setProperty("br-elem", lista_izraza_pridruzivanja.getProperty("br-elem"));
                node.setProperty("tipovi", lista_izraza_pridruzivanja.getProperty("tipovi"));
            });
            */
        }

        // <lista_izraza_pridruzivanja>
        {
            /*
            addRule("<lista_izraza_pridruzivanja>", List.of(
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope) -> {
                Node izraz_pridruzivanja = (Node) node.getChild(0);
                checker.run(izraz_pridruzivanja);

                List<DataType> tipovi = new ArrayList<>();
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));
                node.setProperty("tipovi", tipovi);
                node.setProperty("br-elem", 1);
            });

            addRule("<lista_izraza_pridruzivanja>", List.of(
                    "<lista_izraza_pridruzivanja>",
                    "ZAREZ",
                    "<izraz_pridruzivanja>"
            ), (node, checker, scope) -> {
                Node lista_izraza_pridruzivanja = (Node) node.getChild(0);
                Node izraz_pridruzivanja = (Node) node.getChild(2);

                checker.run(lista_izraza_pridruzivanja);
                checker.run(izraz_pridruzivanja);

                List<DataType> tipovi = (List<DataType>) lista_izraza_pridruzivanja.getProperty("tipovi");
                tipovi.add((DataType) izraz_pridruzivanja.getProperty("tip"));
                int br = (Integer) lista_izraza_pridruzivanja.getProperty("br-elem");

                node.setProperty("tipovi", tipovi);
                node.setProperty("br-elem", br + 1);
            });
            */
        }

    }



}
