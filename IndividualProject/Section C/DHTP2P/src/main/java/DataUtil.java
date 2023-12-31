/**
 * DataUtil objects store the data that each ZNode needs to have:
 *  - IpAddress
 *  - Port number
 *  (The id of the znode is generated from the byte array of this data during znode creation)
 */
public class DataUtil {
    private String ipAddress;
    private int port;

    // Default Constructor for Jackson
    public DataUtil(){

    }

    public DataUtil(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
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
        return "DataUtil{" +
                "ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                '}';
    }
}
