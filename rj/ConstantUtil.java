import java.util.Set;

public class ConstantUtil {

    public static void requireIntValue(String value) throws SemanticException {
        try {
            Integer.valueOf(value);
        } catch (IllegalArgumentException ex){
            throw new SemanticException();
        }
    }

    public static void requireValidChar(String value) throws SemanticException
    {
        if (value.length() < 3 || value.length() > 4) throw new SemanticException();
        if (value.charAt(0) != '\'') throw new SemanticException();
        if (value.length() == 4 && value.charAt(1) != '\\') throw new SemanticException();
        if (value.length() == 4 && !Set.of('t','n','0','\'','\"','\\').contains(value.charAt(2))) throw new SemanticException();
    }

    public static void requireValidString(String value) throws SemanticException
    {
        if(value.length() < 2 ||
                value.charAt(0) != '\"' ||
                value.charAt(value.length()-1) != '\"'){
            throw new SemanticException();
        }

        for (int i = 1; i < value.length()-1; i++){
            if (value.charAt(i) == '\\' && !Set.of('t','n','0','\'','\"','\\').contains(value.charAt(i+1))){
                throw new SemanticException();
            } else {
                i++;
            }
        }
    }

    public static void requireArraySize(String value) throws SemanticException {
        try {
            if (Integer.parseInt(value) < 1 || Integer.parseInt(value) > 1024) throw new SemanticException();
        } catch (IllegalArgumentException ex){
            throw new SemanticException();
        }
    }

    public static int getCharArraySize(String value) {
        int size = 0;
        for (int i = 1; i < value.length() - 1; i++){
            size++;
            if (value.charAt(i) == '\\') i++;
        }
        return size;
    }

}
