import java.util.HashMap;
import java.util.Map;

public class NodeHashTable {
    private static Map<String, String> data;
    private static Map<String, Node> nodes;

    public NodeHashTable() {
        this.data = new HashMap<>();
        this.nodes = new HashMap<>();
    }

    public synchronized void putData(String key, String value) {
        data.put(key, value);
    }

    public synchronized String getData(String key) {
        return data.get(key);
    }

    public synchronized void addNode(String nodeID, Node node){
        nodes.put(nodeID,node);
    }
}
