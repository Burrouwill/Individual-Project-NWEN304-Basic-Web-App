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
import java.beans.PropertyEditorSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class PeerRegistry {
    private static final String DHT = "/DHT";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    // Fields for the Node
    private String nodeId;
    private String ipAddress;
    private int port;
    private Map<String,String> map;

    public PeerRegistry(ZookeeperClient zooKeeperClient, int port) throws InterruptedException, KeeperException {
        this.zooKeeperClient = zooKeeperClient;
        this.port = port;
        this.map = new HashMap<>();

        // Create Permanent DHT node if required & Create Ephemeral Node
        createDHTPZnode();
        registerWithDHT(port);

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
                    try {
                        handlePutRequest(request, response);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (KeeperException e) {
                        throw new RuntimeException(e);
                    }
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

    public void handlePutRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException, InterruptedException, KeeperException {
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

            // Process the Put Request
            // If we are at the correct node --> Save the key value pair & return success
            if (isCorrectNode(key, 4)){
                String result = processPutRequest(key,value);
                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(result);
                // If we are at the wrong node --> Forward request to the correct one
            } else {
                String result = forwardPutRequest(key,value);
                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(result);
            }
            System.out.println(map);
        }
    }

    private String processPutRequest(String key, String value) {
        map.put(key,value);
        return "Data saved to node" + currentZnode + "at port:" + port + " / " + port+10;
    }

    private String forwardPutRequest(String key, String value) {
        /**
         * Calculate the hash of the key.
         * Iterate through all the znodes/nodes in the DHT.
         * For each node, calculate the hash modulus of its id using the same hash function.
         * Compare the calculated hash modulus with the calculated hash from the key.
         * If the calculated hash modulus matches the calculated hash from the key, you've found the responsible node.
         * Forward the request to the responsible node for further processing (storing or retrieving data).
         */
        // Calc moduloHash of key
        


        System.out.println("* Forwarding Put request to node: " + "*");
        return "";
    }


    public int moduloHash(String input, int modulus) {
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            byte[] inputBytes = input.getBytes();
            byte[] hashBytes = sha1Digest.digest(inputBytes);

            // Convert the first 4 bytes of the hash to an integer
            int hashInt = ByteBuffer.wrap(hashBytes, 0, 4).getInt();

            // Take modulus
            int modulusResult = Math.abs(hashInt) % modulus;

            return modulusResult;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return -1; // Return an error value
        }
    }

    public boolean isCorrectNode(String key, int modulus) throws InterruptedException, KeeperException {
        String currentZnodePath = DHT + "/" + currentZnode;
        byte[] zNodeId = zooKeeperClient.getZookeeper().getData(currentZnodePath, false, null);
        String nodePortandIp = zNodeId[0] + "" + zNodeId[1];
        return (moduloHash(key,modulus) == moduloHash(nodePortandIp, modulus));
    }




    public void registerWithDHT(int port) throws InterruptedException, KeeperException {
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(DHT + "/", generateZnodeData(NetworkUtils.getIpAddress(),port));
        currentZnode = znodePath.replace(DHT + "/", "");

        System.out.println("Registered to DHT with ID: " + currentZnode);
        System.out.println("Get & Put Requests accessed at: " + (port+10)); // DO I hash this instead?

    }

    /**
     * Converts Data object --> Byte array to be passed as arg to Znode
     * @param ipAddress
     * @param port
     * @return
     */
    public byte[] generateZnodeData(String ipAddress, int port) {
        Data data = new Data(ipAddress,port);

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
