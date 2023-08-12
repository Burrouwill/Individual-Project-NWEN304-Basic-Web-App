import java.util.HashMap;
import java.util.Map;

/**
 * Data objects store the data that each ZNode needs to have:
 *  - IpAddress
 *  - Port number
 *  - In memory hash map that stores key-value pairs in the format of (string, string)
 *  (The id of the znode is generated from the byte array of this data during znode creation)
 */
public class Data {
    private String ipAddress;
    private int port;
    private Map<String, String> map;

    // Default Constructor for Jackson
    public Data(){

    }

    public Data(String ipAddress, int port, HashMap<String, String> dataMap) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.map = dataMap;
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


    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(HashMap<String, String> dataMap) {
        this.map = dataMap;
    }

    @Override
    public String toString() {
        return "Data{" +
                "ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                ", map=" + map +
                '}';
    }
}
