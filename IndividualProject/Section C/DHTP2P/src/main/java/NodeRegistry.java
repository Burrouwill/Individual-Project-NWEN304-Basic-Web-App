

import org.apache.zookeeper.*;

import java.util.Hashtable;
import java.util.List;

public class NodeRegistry implements Watcher {
    private static final String REGISTRY_ZNODE = "/node_registry";
    private final ZookeeperClient zooKeeperClient;
    private final NodeHashTable nodeHashTable;
    private static Node currentNode;


    public NodeRegistry(ZookeeperClient zooKeeperClient, int port, NodeHashTable hashtable) {
        this.zooKeeperClient = zooKeeperClient;
        this.nodeHashTable = hashtable;
        this.createPeerRegistryZnode();
    }

    public void registerToHashTable(int port) throws KeeperException, InterruptedException {
        // Register the Node in Zookeeper
        String ipAndPort = NetworkUtils.getIpAddress() + ":" + port;
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", ipAndPort.getBytes());

        // Register the Node in NodeHashTable
        // String nodeID, String prevNodeID, String ipAddress, int port)
        String nodeID = znodePath.replace(REGISTRY_ZNODE + "/", "");

        String prevNodeID = "none";
        List<String> znodes = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);
        if (!znodes.isEmpty()){
            prevNodeID = znodes.get(znodes.size()-1); // Because we have to add the node befpre we get the id --> If NodeID == prevID then this was the first noded added
        }
            // Turn it to an it and -1? if id != to previd
            /*
            Us this method of getting a single id --> Compare it to the current id (somehow store than we we register the worker node)
            Do I neeed to implement a node class like this in the other one? I think the Znode is the only way to access stuff between instances though
             */
        String ipAddress = NetworkUtils.getIpAddress();

        Node node = new Node(nodeID,prevNodeID,ipAddress,port);

        nodeHashTable.addNode(nodeID,node);


        // Set a watcher on the created peer znode
        zooKeeperClient.getZookeeper().exists(znodePath, this);
        // Anouce Registration
        System.out.println("Registered as node: " + nodeID);
        System.out.println(node);
    }

    public List<String> getNodes() throws KeeperException, InterruptedException {
        String registryZnode = "/node_registry";

        // Get the list of children (node names) under the registry znode
        List<String> nodes = zooKeeperClient.getZookeeper().getChildren(registryZnode, false);

        return nodes;
    }

    public void unregisterFromCluster() {
        /*
        try {
            if (currentZnode != null && zooKeeperClient.getZookeeper().exists(REGISTRY_ZNODE + "/" + currentZnode, false) != null) {
                zooKeeperClient.getZookeeper().delete(REGISTRY_ZNODE + "/" + currentZnode, -1);
                System.out.println("Unregistered from cluster: " + currentZnode);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

         */
    }

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeChildrenChanged) {
            registerForUpdates();
        }
    }

    private void createPeerRegistryZnode() {
        try {
            zooKeeperClient.createPersistantNode(REGISTRY_ZNODE, null);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void registerForUpdates() {
        try {
            List<String> peers = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);

            System.out.println("The peer addresses are:");

            for (String peer : peers) {
                String znodePath = REGISTRY_ZNODE + "/" + peer;
                byte[] data = zooKeeperClient.getZookeeper().getData(znodePath, false, null);
                String peerData = new String(data);

                String[] parts = peerData.split(":");
                if (parts.length == 2) {
                    String port = parts[1];
                    System.out.println("Peer: " + peer + ", Address: http://host.docker.internal:" + port);
                } else {
                    System.out.println("Peer: " + peer + ", Invalid address format: " + peerData);
                }
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

