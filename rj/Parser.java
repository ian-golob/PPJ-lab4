import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {

    private static int calcDepth(String s){
        int i;
        for (i = 0; s.charAt(i) == ' '; i++);
        return i;
    }

    private static Leaf parseLeaf(String s){
        int firstSpace = s.indexOf(' ');
        int secondSpace = s.substring(firstSpace + 1).indexOf(' ') + firstSpace + 1;
        return new Leaf(null, s.substring(0, firstSpace), Integer.parseInt(s.substring(firstSpace + 1, secondSpace)), s.substring(secondSpace + 1));
    }

    public static Node parseInput(InputStream in){
        List<String> inputLines = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
        Node root = new Node(null, inputLines.get(0));
        Node currentNode = root;
        int oldDepth = 0;
        int depth;
        String currentLine;
        for (int i = 1; i < inputLines.size(); i++){
            currentLine = inputLines.get(i);
            depth = calcDepth(currentLine);
            currentLine = currentLine.substring(depth);
            boolean isNode = currentLine.charAt(0) == '<';
            TreeElement newTreeElement;

            if (!isNode){
                newTreeElement = parseLeaf(currentLine);
            } else{
                newTreeElement = new Node(null, currentLine);
            }

            for (int j = 0; j < oldDepth - depth; j++) currentNode = currentNode.getParent();

            newTreeElement.setParent(currentNode);
            currentNode.getChildren().add(newTreeElement);
            if (isNode) currentNode = (Node) newTreeElement;
            oldDepth = depth;
        }

        return root;
    }

}
