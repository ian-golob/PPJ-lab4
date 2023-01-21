import java.util.ArrayList;
import java.util.List;

public class Function implements ScopeElement {

    private final String name;

    private final DataType returnType;

    private final List<DataType> parameters;

    private final FunctionType functionType;

    private boolean isDefined = false;

    private final boolean voidParameters;

    public Function(String name, FunctionType functionType) throws SemanticException {

        this.functionType = functionType;

        DataType returnType = functionType.getReturnType();
        List<DataType> parameters = functionType.getParameters();

        this.name = name;
        this.returnType = returnType;
        this.parameters = new ArrayList<>();

        if(parameters.size() == 0 ||
                parameters.size() == 1 && parameters.get(0) == DataType.VOID){
            voidParameters = true;

        } else {
            voidParameters = false;

            for(DataType parameter: parameters){
                if(parameter == DataType.VOID){
                    throw new SemanticException();
                }

                this.parameters.add(parameter);
            }
        }
    }

    public String getName() {
        return name;
    }

    public DataType getReturnType() {
        return returnType;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public List<DataType> getParameters() {
        return parameters;
    }

    public boolean isVoidParameters() {
        return voidParameters;
    }

    public void define(){
        isDefined = true;
    }

    public boolean isDefined(){
        return isDefined;
    }

    @Override
    public DataType getType() {
        return functionType;
    }

    @Override
    public Boolean isLValue() {
        return false;
    }
}
