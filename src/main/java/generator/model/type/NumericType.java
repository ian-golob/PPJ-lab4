package generator.model.type;

import generator.semantic.SemanticException;

public abstract class NumericType extends DataType{

    public static final NumericType CHAR = new NumericType(false) {
        @Override
        public boolean implicitlyCastableTo(DataType dataType) {
            return dataType == CHAR ||
                    dataType == CONST_CHAR ||
                    dataType == INT ||
                    dataType == CONST_INT;
        }

        @Override
        public boolean explicitlyCastableTo(DataType dataType) {
            return implicitlyCastableTo(dataType);
        }
    };

    public static final NumericType CONST_CHAR = new NumericType(true, CHAR) {
        @Override
        public boolean implicitlyCastableTo(DataType dataType) {
            return dataType == CHAR ||
                    dataType == CONST_CHAR ||
                    dataType == INT ||
                    dataType == CONST_INT;
        }

        @Override
        public boolean explicitlyCastableTo(DataType dataType) {
            return implicitlyCastableTo(dataType);
        }
    };

    public static final NumericType INT = new NumericType(false) {
        @Override
        public boolean implicitlyCastableTo(DataType dataType) {
            return dataType == INT ||
                    dataType == CONST_INT;
        }

        @Override
        public boolean explicitlyCastableTo(DataType dataType) {
            return implicitlyCastableTo(dataType) ||
                    dataType == CHAR ||
                    dataType == CONST_CHAR;
        }
    };

    public static final NumericType CONST_INT = new NumericType(true, INT) {
        @Override
        public boolean implicitlyCastableTo(DataType dataType) {
            return dataType == INT ||
                    dataType == CONST_INT;
        }

        @Override
        public boolean explicitlyCastableTo(DataType dataType) {
            return implicitlyCastableTo(dataType) ||
                    dataType == CHAR ||
                    dataType == CONST_CHAR;
        }
    };

    private final boolean isConst;

    private final NumericType baseNumericType;

    private NumericType(boolean isConst, NumericType baseNumericType){
        this.isConst = isConst;
        this.baseNumericType = baseNumericType;
    }

    public NumericType(boolean isConst) {
        this.isConst = isConst;
        this.baseNumericType = this;
    }

    public static NumericType constOf(DataType dataType) throws SemanticException {
        if(!(dataType instanceof NumericType)){
            throw new SemanticException();
        }

        NumericType numericType = (NumericType) dataType;

        if(numericType == CHAR){
            return CONST_CHAR;
        } else if(numericType == INT){
            return CONST_INT;
        } else {
            throw new SemanticException();
        }
    }

    public boolean isConst(){
        return isConst;
    }

    /**
     * Returns the unwrapped NumericType, for example const(INT) and INT would return INT.
     * @return CHAR or INT
     */
    public NumericType getBaseNumericType(){
        return baseNumericType;
    }


    public static boolean isConst(DataType type){
        if(!(type instanceof NumericType)){
            return false;
        }

        return ((NumericType) type).isConst();
    }

}
