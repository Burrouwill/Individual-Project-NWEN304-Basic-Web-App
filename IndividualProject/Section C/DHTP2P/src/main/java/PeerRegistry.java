import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class PeerRegistry {
    private static final String DHT = "/DHT";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    // Fields for the Node
    private String nodeId; // Unique ID for this peer
    private String ipAddress;
    private int port;

    public PeerRegistry(ZookeeperClient zooKeeperClient, int port) {
        this.zooKeeperClient = zooKeeperClient;
        this.port = port;
        createDHTPZnode();
    }

    public void registerWithDHT(int port) throws InterruptedException, KeeperException {
        // The "map" --> What goes in this?
        HashMap<String,String> genericMap = new HashMap<>();


        String znodePath = zooKeeperClient.createEphemeralSequentialNode(DHT + "/", generateZnodeData(NetworkUtils.getIpAddress(),port,genericMap));
        currentZnode = znodePath.replace(DHT + "/", "");

        System.out.println("Registered to DHT with ID: " + currentZnode);


        // Test deserialisation:
        byte[] znodeData = zooKeeperClient.getZookeeper().getData(DHT + "/" + currentZnode,false,null);
        System.out.println(convertBytesToData(znodeData));

    }

    /**
     * Converts Data object --> Byte array to be passed as arg to Znode
     * @param ipAddress
     * @param port
     * @param map
     * @return
     */
    public byte[] generateZnodeData(String ipAddress, int port, HashMap<String, String> map) throws InterruptedException, KeeperException { // What goes in the map? Where and when do I set this?
        // Needs to return Data obj --> Byte array **TEST MAP**
        Data data = new Data(ipAddress,port,map);
        data.getMap().put("Test","test");

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            byte[] dataBytes = jsonData.getBytes();
            return dataBytes;
        } catch (Exception e) {
            e.printStackTrace();
        }

        throw new IllegalArgumentException("Something went very wrong");
    }

    /**
     * Converts byte data from Znode --> Data object
     * @param dataBytes
     * @return
     */
    public Data convertBytesToData(byte[] dataBytes) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String jsonData = new String(dataBytes);
            Data data = objectMapper.readValue(jsonData, Data.class);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void createDHTPZnode() {
        // Create a persistant znode /DHT in zookeeper if it doesn't exist
        try {
            zooKeeperClient.createPersistantNode(DHT, null);
        } catch (KeeperException | InterruptedException e) {
        }
    }
}
