package generator.scope;

import generator.model.type.DataType;


public interface ScopeElement {

    DataType getType();

    Boolean isLValue();
}
