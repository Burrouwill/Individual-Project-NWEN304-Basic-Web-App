package cluster.management;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Data objects store the data that each ZNode needs to have:
 *  - IpAddress
 *  - Port number
 *  (The id of the znode is generated from the byte array of this data during znode creation)
 */
public class Data {
    private String ipAddress;
    private int port;

    // Default Constructor for Jackson
    public Data(){
    }

    public Data(String ipAddress, int port) {
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
        return "Data{" +
                "ipAddress='" + ipAddress + '\'' +
                ", port=" + port +
                '}';
    }

    public static Data convertBytesToData(byte[] dataBytes) {
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

    /**
     * Converts a Data obj to byte array
     *
     * @param data
     * @return
     */
    public static byte[] convertDataToBytes(Data data) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonData = objectMapper.writeValueAsString(data);
            byte[] dataBytes = jsonData.getBytes();
            return dataBytes;
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0]; // Return an empty array in case of error
        }
    }

}
