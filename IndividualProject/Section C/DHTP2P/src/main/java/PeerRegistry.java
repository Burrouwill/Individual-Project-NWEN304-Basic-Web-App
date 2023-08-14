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
import java.util.List;
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
            if (currentZnode.equals(findCorrectNode(key))){
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

    private String forwardPutRequest(String key, String value) throws InterruptedException, KeeperException {
        // Get the correct node
        String correctNode = findCorrectNode(key);

        // Forward the put request

        System.out.println(hash(correctNode));



        // Display forwarding message on current node
        System.out.println("Hashed Key: "+ hash(key));
        System.out.println("* Forwarding Put request to node: " + correctNode + "(Hashed Node: "+ hash(correctNode)+ " *");

        return "* Forwarding Put request to node: " + correctNode + "*";
    }

    /**
     * Hashes a string using SHA-1 hashing algo. Returnns the hash as an int.
     * @param input
     * @return
     */
    public int hash(String input) {
        try {
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            byte[] inputBytes = input.getBytes();
            byte[] hashBytes = sha1Digest.digest(inputBytes);

            // Convert the first 4 bytes of the hash to an integer
            int hashInt = ByteBuffer.wrap(hashBytes, 0, 20).getInt();
            int modulusResult = Math.abs(hashInt);

            return modulusResult;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return -1; // Return an error value
        }
    }

    /**
     * Returns the closed node to the key, in a clockwise direction on the hashring.
     * @param key
     * @return
     * @throws InterruptedException
     * @throws KeeperException
     */
    public String findCorrectNode(String key) throws InterruptedException, KeeperException {
        // Calc Hash of key
        int keyHash = hash(key);

        // Find the correct node
        List<String> nodes = zooKeeperClient.getSortedChildren(DHT);

        String correctNode = null;
        int bestHashDiff = Integer.MAX_VALUE;

        for (int i = 0 ; i < nodes.size() ; i++){
            int nodeHash = hash(nodes.get((i)));
            if (keyHash < nodeHash){ // If node is to the right of the key / Further around on the hashring --> We will consider it a potential correct node (Everything to the left / less than is not considered)
                if (nodeHash - keyHash < bestHashDiff){ // We look for the node closest to the key but still to the right of the key on the hashring
                    correctNode = nodes.get(i);
                    bestHashDiff = nodeHash - keyHash;
                }
            }
        }

        // If no node found, take the first node as the correct node
        if (correctNode == null && !nodes.isEmpty()) {
            correctNode = nodes.get(0);
        }

        // Error handling
        if (correctNode == null){
            throw new IllegalArgumentException("Couldnt find a node, something went wrong.");
        }
        return correctNode;
    }



    public void registerWithDHT(int port) throws InterruptedException, KeeperException {
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(DHT + "/", generateZnodeData(NetworkUtils.getIpAddress(),port));
        currentZnode = znodePath.replace(DHT + "/", "");

        System.out.println("Registered to DHT with ID: " + hash(currentZnode) + " (Znode ID: " + currentZnode + ")");
        System.out.println("Get & Put Requests accessed at: " + (port+10));

    }

    /**
     * Converts Data object --> Byte array to be passed as arg to Znode
     * @param ipAddress
     * @param port
     * @return
     */
    public byte[] generateZnodeData(String ipAddress, int port) {
        // Create a Data object with IP address and port
        Data data = new Data(ipAddress, port);

        // Convert Data object to bytes
        byte[] dataBytes = convertDataToBytes(data);

        return dataBytes;
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

    /**
     * Converts a Data obj to byte array
     * @param data
     * @return
     */
    public byte[] convertDataToBytes(Data data) {
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

    /**
     * Create a persistant znode /DHT in zookeeper if it doesn't exist
     */
    private void createDHTPZnode() {
        try {
            zooKeeperClient.createPersistantNode(DHT, null);
        } catch (KeeperException | InterruptedException e) {
        }
    }
}
