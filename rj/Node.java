import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Node extends TreeElement{
    private final List<TreeElement> children = new ArrayList<>();

    public List<TreeElement> getChildren() {
        return children;
    }

    public TreeElement getChild(int num){
        return children.get(num);
    }

    public Node(Node parent, String name) {
        super(parent, name);
        isLeaf = false;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public String getProductionErrorString() {
        return this.getName() + " ::= " +
                children.stream().map(TreeElement::toString).collect(Collectors.joining(" "));
    }
}
