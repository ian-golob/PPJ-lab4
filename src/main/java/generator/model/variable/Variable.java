package generator.model.variable;

import generator.semantic.SemanticException;
import generator.model.type.DataType;
import generator.model.type.NumericType;
import generator.scope.ScopeElement;

import java.util.Objects;

public class Variable implements ScopeElement {

    private final String name;

    private final DataType type;

    private final boolean isConst;

    private final boolean isArray;

    private int arraySize = -1;


    public Variable(String name, DataType type, boolean isConst, boolean isArray) throws SemanticException {
        this.name = Objects.requireNonNull(name);

        if(type == DataType.VOID){
            throw new SemanticException();
        }
        this.type = Objects.requireNonNull(type);

        this.isConst = isConst;
        this.isArray = isArray;
    }

    public String getName() {
        return name;
    }

    @Override
    public DataType getType() {
        return type;
    }

    public boolean isConst() {
        return isConst;
    }

    public boolean isArray() {
        return isArray;
    }

    public Boolean isLValue(){
        return type == NumericType.CHAR || type == NumericType.INT;
    }

    public int getArraySize() {
        return arraySize;
    }

    public void setArraySize(int arraySize) {
        this.arraySize = arraySize;
    }
}
