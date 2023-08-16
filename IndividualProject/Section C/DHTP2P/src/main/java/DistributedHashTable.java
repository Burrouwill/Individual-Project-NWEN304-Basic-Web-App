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
import java.io.*;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;

public class DistributedHashTable {
    private static final String DHT = "/DHT";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    // Fields for the Node
    private String nodeId;
    private String ipAddress;
    private int port;
    private Map<String, String> map;

    public DistributedHashTable(ZookeeperClient zooKeeperClient, int port) throws InterruptedException, KeeperException, IOException {
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
        Server server = new Server(port + 10);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
                // Handle requests here based on the 'target' URL
                if ("/get".equals(target)) {
                    // Handle GET request
                    try {
                        handleGetRequest(request, response);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (KeeperException e) {
                        throw new RuntimeException(e);
                    }
                } else if ("/put".equals(target)) {
                    // Handle PUT request
                    try {
                        handlePutRequest(request, response);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (KeeperException e) {
                        throw new RuntimeException(e);
                    }
                } else if ("/getMap".equals(target)) {
                    // Handle MAP request
                    handleMapRequest(request, response);
                } else if ("/updateMap".equals(target)) {
                    handleUpdateMapRequest(request, response);
                }else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                request.setHandled(true);
            }
        });

        server.start();
        server.join();
    }

    // ===============================================
    //              GET REQUEST HANDLING
    // ===============================================
    public void handleGetRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException, KeeperException {
        if ("GET".equals(request.getMethod())) {
            String key = request.getParameter("key");

            if (key == null || key.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            // Process the Get Request
            // If we are at the correct node --> Retrieve the value for the key
            if (currentZnode.equals(findCorrectNode(key))) {
                String value = map.get(key);
                if (value != null) {
                    response.setContentType("text/plain;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println(value);
                    System.out.println("Returning Value: " + value + " to client");
                } else {
                    response.setContentType("text/plain;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println("Key not found");
                }
            } else {
                // If we are at the wrong node --> Forward request to the correct one
                String correctNodeId = findCorrectNode(key);
                String correctNodePath = DHT + "/" + correctNodeId;
                String forwardedValue = forwardGetRequest(key, correctNodePath);
                System.out.println("* Forwarding Get request to Znode: " + correctNodeId + " (Hashed Node: " + hash(correctNodeId) + ")" + " *");

                // Forward the response from the correct node to the client
                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(forwardedValue);
            }
        }
    }

    private String forwardGetRequest(String key, String correctNodePath) throws InterruptedException, KeeperException {
        // Get the dest port + Add 10 to access the Jetty server
        int targetPort = getPortFromZnode(correctNodePath) + 10;

        try {
            URL url = new URL("http://localhost:" + targetPort + "/get?key=" + key);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method to GET
            connection.setRequestMethod("GET");

            // Get response from the forwarded request
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return response.toString();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    // ===============================================
    //              PUT REQUEST HANDLING
    // ===============================================
    public void handlePutRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException, KeeperException {
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
            if (currentZnode.equals(findCorrectNode(key))) {
                String result = processPutRequest(key, value);
                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(result);
                // If we are at the wrong node --> Forward request to the correct one
            } else {
                String result = forwardPutRequest(key, value);
                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(result);
            }
            System.out.println(map); // For testing - Shows the contents of the map
        }
    }

    private String processPutRequest(String key, String value) {
        map.put(key, value);
        System.out.println(hash(key));
        System.out.println("Data saved to Znode ID: " + currentZnode + " (Hash:" + hash(currentZnode) + ")");
        return "Data saved to node" + currentZnode + "at port:" + port + " / " + port + 10;
    }

    private String forwardPutRequest(String key, String value) throws InterruptedException, KeeperException {
        // Get the correct node
        String correctNodeId = findCorrectNode(key);

        // Forward the put request
        String correctNodePath = DHT + "/" + correctNodeId;
        String response = sendPutRequest(key, value, correctNodePath);

        // Display forwarding message on current node
        System.out.println("Hashed Key: " + hash(key));
        System.out.println("* Forwarding Put request to node: " + correctNodeId + " (Hashed Node: " + hash(correctNodeId) + ")" + " *");

        return "* Forwarding Put request to node: " + correctNodeId + "*";
    }

    public String sendPutRequest(String key, String value, String destZNodePath) throws InterruptedException, KeeperException {
        // Get the dest port + Add 10 to access the Jetty server
        int targetPort = getPortFromZnode(destZNodePath) + 10;

        try {
            URL url = new URL("http://localhost:" + targetPort + "/put");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method to PUT
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Prepare JSON data
            String jsonData = "{\"key\": \"" + key + "\", \"value\": \"" + value + "\"}";
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get response from the forwarded request
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            connection.disconnect();

            return "Forwarded to " + destZNodePath + " (Response Code: " + responseCode + ", Response Message: " + responseMessage + ")";
        } catch (IOException e) {
            e.printStackTrace();
            return "Forwarding request failed.";
        }
    }

    // ===============================================
    //              MAP REQUEST HANDLING
    // ===============================================
    public void handleMapRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("GET".equals(request.getMethod())) {
            try {
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);

                ObjectMapper objectMapper = new ObjectMapper();
                PrintWriter out = response.getWriter();
                objectMapper.writeValue(out, map);

            } catch (IOException e) {
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    public Map<String, String> sendMapRequest(int port) throws IOException {
        Map<String, String> resultMap = new HashMap<>();

        try {
            URL url = new URL("http://localhost:" + port + "/getMap"); // Assuming the endpoint is "/getMap"
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder responseContent = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseContent.append(inputLine);
                }

                ObjectMapper objectMapper = new ObjectMapper();
                resultMap = objectMapper.readValue(responseContent.toString(), new TypeReference<Map<String, String>>() {});
            }

        } catch (IOException e) {
            e.printStackTrace();
            resultMap.put("error", "Error: " + e.getMessage());
        }

        return resultMap;
    }

    // ===============================================
    //              UPDATE MAP REQUEST HANDLING
    // ===============================================
    private void handleUpdateMapRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if ("PUT".equals(request.getMethod())) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> newMap = objectMapper.readValue(request.getInputStream(), new TypeReference<Map<String, String>>() {});

                map = newMap;

                // Announce Keys Updated at Successor Node
                System.out.println("Keys Updated");
                System.out.println("Updated Map: "+ map);

                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println("Map updated successfully");
            } catch (IOException e) {
                e.printStackTrace();
                response.setContentType("text/plain;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println("Error updating map");
            }
        } else {
            response.setContentType("text/plain;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.getWriter().println("Method not allowed");
        }
    }

    public void sendMapPutRequest(int targetPort, Map<String, String> mapToSend) throws IOException {
        try {
            URL url = new URL("http://localhost:" + targetPort + "/updateMap");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method to PUT
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Convert the map to JSON format
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonData = objectMapper.writeValueAsString(mapToSend);

            // Write JSON data to the request body
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get response from the PUT request
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            connection.disconnect();

            System.out.println("Response Code: " + responseCode);
            System.out.println("Response Message: " + responseMessage);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error sending PUT request: " + e.getMessage());
        }
    }

    /**
     * Gets the port number out of a given zNode
     *
     * @param znodePath
     * @return
     * @throws InterruptedException
     * @throws KeeperException
     */
    public int getPortFromZnode(String znodePath) throws InterruptedException, KeeperException {
        byte[] dataBytes = zooKeeperClient.getZookeeper().getData(znodePath, false, null);
        Data data = convertBytesToData(dataBytes);
        return data.getPort();
    }

    /**
     * Hashes a string using SHA-1 hashing algo. Returns the hash as an int.
     *
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
            int result = Math.abs(hashInt);

            return result;
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

        for (int i = 0; i < nodes.size(); i++) {
            int nodeHash = hash(nodes.get((i)));
            if (keyHash < nodeHash) { // If node is to the right of the key / Further around on the hashring --> We will consider it a potential correct node (Everything to the left / less than is not considered)
                if (nodeHash - keyHash < bestHashDiff) { // We look for the node closest to the key but still to the right of the key on the hashring
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
        if (correctNode == null) {
            throw new IllegalArgumentException("Couldnt find a node, something went wrong.");
        }
        return correctNode;
    }

    public String findRightAdjacentNode(String newNodeId) throws InterruptedException, KeeperException {
        // Calc Hash of the newly added node
        int newNodeHash = hash(newNodeId);

        // Get all nodes in DHT
        List<String> nodes = zooKeeperClient.getZookeeper().getChildren(DHT, false);
        // Remove new nodeId to prevent finding its self / the newly created node
        nodes.remove(newNodeId);

        String adjacentNode = null;
        int smallestHashDiff = Integer.MAX_VALUE;

        for (String node : nodes) {
            int currentNodeHash = hash(node);

            if (currentNodeHash > newNodeHash) { // If we are not looking at the same new node & node hash > newNode hash (Further clockwise than new node)
                int hashDiff = currentNodeHash - newNodeHash;
                if (hashDiff < smallestHashDiff) {  // Find the node that is closest to the new node & update vars
                    adjacentNode = node;
                    smallestHashDiff = hashDiff;
                }
            }
        }

        // Handle wrap-around case
        if (adjacentNode == null && !nodes.isEmpty()) {
            int firstNodeHash = hash(nodes.get(0));
            int wrapAroundDiff = newNodeHash - firstNodeHash;
            if (wrapAroundDiff < smallestHashDiff) {
                adjacentNode = nodes.get(0);
            }
        }
        // Handle the case where the newNodeId itself might be the only node in the DHT
        if (adjacentNode == null) {
            return newNodeId;
        }

        return adjacentNode;
    }

    /**
     * When a new node n joins, it finds out its successor and claims the applicable key value pairs.
     */
    public void claimSuccessorKeys(String newZnode) throws InterruptedException, KeeperException, IOException {
        // Get the node next to the new node (Clockwise) on DHT
        String successorNode = findRightAdjacentNode(newZnode);

        // Find the port no of the adjacent node
        String successorNodePath = DHT + "/" + successorNode;
        int port = getPortFromZnode(successorNodePath)+10;
        System.out.println(port);
        // Retive the map from said node via the /getMap end point
        Map<String,String> successorMap = sendMapRequest(port);

        // Transfer the appropriate keys to the new node & remove them from successor
        for (String key : successorMap.keySet()){
            if (hash(key) < hash(newZnode)){ // If hashKey to the to the left of new node then shift it to the new node (Helps to draw a picture)
                String value = successorMap.get(key);
                map.put(key,value); // Add the map entry to this nodes map
                // Delete the map entry from the successor node map via /deleteMapEntry end point
            }
        }

        // Update Keys of Successor Node
        Map<String,String> testMap = Map.of("I","am","a","test");
        sendMapPutRequest(port,testMap);

    }

    public void registerWithDHT(int port) throws InterruptedException, KeeperException, IOException {
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(DHT + "/", generateZnodeData(NetworkUtils.getIpAddress(), port));
        currentZnode = znodePath.replace(DHT + "/", "");

        System.out.println("Registered to DHT with Hash: " + hash(currentZnode) + " (Znode ID: " + currentZnode + ")");
        System.out.println("Get & Put Requests accessed at: " + (port + 10));


        if (zooKeeperClient.getSortedChildren(DHT).size() > 1){
            claimSuccessorKeys(currentZnode);
        }



    }

    /**
     * Converts Data object --> Byte array to be passed as arg to Znode
     *
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
     *
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
     *
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
