package generator.model.type;

import generator.semantic.SemanticException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FunctionType extends DataType{

    private final DataType returnType;

    private final List<DataType> parameters;

    private final boolean voidParameters;

    public FunctionType(DataType returnType, DataType... parameters) throws SemanticException {
        this.returnType = returnType;
        this.parameters = new ArrayList<>();

        if(parameters.length == 0 ||
                parameters.length == 1 && parameters[0] == DataType.VOID){
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

    public DataType getReturnType() {
        return returnType;
    }

    public List<DataType> getParameters() {
        return parameters;
    }

    public boolean isVoidParameters() {
        return voidParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionType that = (FunctionType) o;
        return voidParameters == that.voidParameters && returnType.equals(that.returnType) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, parameters, voidParameters);
    }

    @Override
    public boolean implicitlyCastableTo(DataType dataType) {
        return equals(dataType);
    }

    @Override
    public boolean explicitlyCastableTo(DataType dataType) {
        return equals(dataType);
    }
}
