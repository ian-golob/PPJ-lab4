package generator.model.type;

import generator.semantic.SemanticException;

public class ArrayType extends DataType {

    private static final ArrayType ARRAY_CHAR = new ArrayType(NumericType.CHAR);

    private static final ArrayType ARRAY_CONST_CHAR = new ArrayType(NumericType.CONST_CHAR);

    private static final ArrayType ARRAY_INT = new ArrayType(NumericType.INT);

    private static final ArrayType ARRAY_CONST_INT = new ArrayType(NumericType.CONST_INT);

    public static ArrayType of(DataType dataType) throws SemanticException {
        if(!(dataType instanceof NumericType)){
            throw new SemanticException();
        }

        NumericType numericType = (NumericType) dataType;

        if(numericType == NumericType.CHAR){
            return ARRAY_CHAR;
        } else if(numericType == NumericType.CONST_CHAR){
            return ARRAY_CONST_CHAR;
        } else if(numericType == NumericType.INT){
            return ARRAY_INT;
        } else if(numericType == NumericType.CONST_INT){
            return ARRAY_CONST_INT;
        } else {
            throw new SemanticException();
        }
    }

    private final NumericType numericType;

    private ArrayType(NumericType numericType){
        this.numericType = numericType;
    }

    public NumericType getNumericType() {
        return numericType;
    }


    @Override
    public boolean implicitlyCastableTo(DataType dataType) {
        if(!(dataType instanceof ArrayType)){
            return false;
        }

        ArrayType other = (ArrayType) dataType;

        if(this.numericType == other.numericType){
            return true;
        }

        if(!this.numericType.isConst() &&
            other.numericType.isConst() &&
            this.numericType == other.numericType.getBaseNumericType()){
            return true;
        }

        return false;
    }

    @Override
    public boolean explicitlyCastableTo(DataType dataType) {
        return implicitlyCastableTo(dataType);
    }
}
