public abstract class DataType {

    public static final DataType VOID = new DataType() {
        @Override
        public boolean implicitlyCastableTo(DataType dataType) {
            return dataType == VOID;
        }

        @Override
        public boolean explicitlyCastableTo(DataType dataType) {
            return dataType == VOID;
        }
    };

    public abstract boolean implicitlyCastableTo(DataType dataType);

    public abstract boolean explicitlyCastableTo(DataType dataType);

}
