public class Node {
    private String nodeID;
    private String ipAddress;
    private int port;
    private String prevID;

    public Node(String nodeID, String parentID, String ipAddress, int port) {
        this.nodeID = nodeID;
        this.ipAddress = ipAddress;
        this.port = port;
        this.prevID = parentID;
    }

    public String getNodeID() {
        return nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeID='" + nodeID + '\'' +
                ", prevID='" + prevID + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                '}';
    }
}

