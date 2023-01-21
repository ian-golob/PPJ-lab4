public class Leaf extends TreeElement{
    private int lineNumber;
    private String sourceText;

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public Leaf(Node parent, String name, int lineNumber, String sourceText) {
        super(parent, name);
        this.lineNumber = lineNumber;
        this.sourceText = sourceText;
        isLeaf = true;
    }

    @Override
    public String toString() {
        return getName() + "(" + lineNumber + "," + sourceText +")";
    }
}
