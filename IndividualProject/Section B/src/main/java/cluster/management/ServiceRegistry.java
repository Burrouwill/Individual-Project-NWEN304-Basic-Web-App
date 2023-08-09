package cluster.management;

import org.apache.zookeeper.*;

import java.util.List;

import static cluster.management.NetworkUtils.getIpAddress;

public class ServiceRegistry implements Watcher {
    private static final String REGISTRY_ZNODE = "/service_registry";
    private static final String ELECTION_ZNODE_NAME = "/leader_election";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;
    private String nodeName = "";


    public ServiceRegistry(ZookeeperClient zooKeeperClient) {
        this.zooKeeperClient = zooKeeperClient;
        this.createServiceRegistryPZnode();

        // Set a watcher on the service registry znode
        try {
            zooKeeperClient.getZookeeper().getChildren(REGISTRY_ZNODE, this);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // -------- TODO -------
    public String registerToCluster(int port) throws KeeperException, InterruptedException {
        // Register as a worker in /service_registry znode by adding IP address and Port number
        String ipAndPort = getIpAddress() + ":" + port;

        String znodePath = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", ipAndPort.getBytes());

        currentZnode = znodePath.replace(REGISTRY_ZNODE + "/", "");
        System.out.println("Registered as worker: " + currentZnode);

        // Set a watcher on the created worker znode
        zooKeeperClient.getZookeeper().exists(znodePath, this);

        return znodePath;
    }
    // --------END TODO ------

    // -------- TODO -------
    public void registerForUpdates() {
        try {
            List<String> workers = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);

            System.out.println("The cluster addresses are:");

            for (String worker : workers) {
                String znodePath = REGISTRY_ZNODE + "/" + worker;
                byte[] data = zooKeeperClient.getZookeeper().getData(znodePath, false, null);
                String workerData = new String(data);
                nodeName = REGISTRY_ZNODE;
                // Assuming workerData is in the format "IP:Port", split and print port
                String[] parts = workerData.split(":");
                if (parts.length == 2) {
                    String port = parts[1];
                    System.out.println("Worker: " + worker + ", Port: " + port);
                } else {
                    System.out.println("Worker: " + worker + ", Invalid address format: " + workerData);
                }
            }

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // --------END TODO ------

    // -------- TODO -------
    private void createServiceRegistryPZnode() {
        try {
            zooKeeperClient.createPersistantNode(REGISTRY_ZNODE, null);
        } catch (KeeperException | InterruptedException e) {
        }
    }
    // --------END TODO ------

    // -------- TODO -------
    public void unregisterFromCluster() {
        try {
            if (currentZnode != null && zooKeeperClient.getZookeeper().exists(REGISTRY_ZNODE + "/" + currentZnode, false) != null) {
                zooKeeperClient.getZookeeper().delete(REGISTRY_ZNODE + "/" + currentZnode, -1);
                System.out.println("Unregistered from cluster: " + currentZnode);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }




    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeChildrenChanged && !nodeName.equals(REGISTRY_ZNODE)){

            registerForUpdates();
        }
    }
}

