import java.util.HashMap;
import java.util.Map;

public abstract class TreeElement {
    protected Node parent;
    protected String name;
    protected boolean isLeaf;

    private final Map<String, Object> properties = new HashMap<>();

    public void setProperty(String property, Object value){
        properties.put(property, value);
    }

    public Object getProperty(String property){
        if(properties.get(property) == null){
            throw new NullPointerException("property is null");
        }

        return properties.get(property);
    }

    public boolean hasProperty(String property){
        return properties.get(property) != null;
    }


    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public TreeElement(Node parent, String name) {
        this.parent = parent;
        this.name = name;
    }
}
