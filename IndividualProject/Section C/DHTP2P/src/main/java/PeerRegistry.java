import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.zookeeper.KeeperException;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private Map<String,String> map;

    public PeerRegistry(ZookeeperClient zooKeeperClient, int port) {
        this.zooKeeperClient = zooKeeperClient;
        this.port = port;
        this.map = new HashMap<>();
        // Create Znode
        createDHTPZnode();
        // Start the server
        try {
            startWebServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startWebServer() throws Exception {
        Server server = new Server(port+10);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
                // Handle requests here based on the 'target' URL
                if ("/get".equals(target)) {
                    // Handle GET request
                    handleGetRequest(response);
                } else if ("/put".equals(target)) {
                    // Handle PUT request
                    handlePutRequest(request, response);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                request.setHandled(true);
            }
        });

        server.start();
        server.join();
    }

    private void handleGetRequest(HttpServletResponse response) throws IOException {
        // Simulate node-specific data
        Map<String, String> nodeData = new HashMap<>();
        nodeData.put("nodeId", nodeId);
        nodeData.put("ipAddress", ipAddress);
        nodeData.put("port", Integer.toString(port));

        // Convert data to JSON format
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonResponse = objectMapper.writeValueAsString(nodeData);

        // Set response content type and write JSON data
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(jsonResponse);
    }

    public void handlePutRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if ("PUT".equals(request.getMethod())) {
            StringBuilder requestBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    requestBody.append(line);
                }
            }

            String jsonString = requestBody.toString();

            // Parse JSON data
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            String key = jsonNode.get("key").asText();
            String value = jsonNode.get("value").asText();

            // Do something with the key and value
            String result = processKeyAndValue(key, value);
                System.out.println("Key: " + key); // TEST
                System.out.println("Value: " + value); // TEST

            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println(result);
        }
    }

    private String processKeyAndValue(String key, String value) {
        // Customize this method to process the key and value as needed
        return "Processed: " + key + " - " + value;
    }




    public void registerWithDHT(int port) throws InterruptedException, KeeperException {
        // The "map" --> What goes in this?
        HashMap<String,String> genericMap = new HashMap<>();


        String znodePath = zooKeeperClient.createEphemeralSequentialNode(DHT + "/", generateZnodeData(NetworkUtils.getIpAddress(),port,genericMap));
        currentZnode = znodePath.replace(DHT + "/", "");

        System.out.println("Registered to DHT with ID: " + currentZnode);
        System.out.println("Get & Put Requests accessed at: " + (port+10)); // DO I hash this instead?

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
