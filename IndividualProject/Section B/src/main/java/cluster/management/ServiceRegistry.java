package cluster.management;

import org.apache.zookeeper.*;

import java.util.List;

import static cluster.management.NetworkUtils.getIpAddress;

public class ServiceRegistry implements Watcher {
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    public ServiceRegistry(ZookeeperClient zooKeeperClient) {
        this.zooKeeperClient = zooKeeperClient;

        if (zooKeeperClient.getZookeeper() == null){
            createServiceRegistryPZnode();
        }

    }

    // -------- TODO -------
    public void registerToCluster(int port) throws KeeperException, InterruptedException {

        // Register as a worker in /service_registry znode by adding IP address and Port
        // number

        String ipAndPort = getIpAddress() + ":" + port;
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", ipAndPort.getBytes());
        currentZnode = znodePath.replace(REGISTRY_ZNODE + "/", "");
        System.out.println("Registered as worker: " + currentZnode);


        return;
    }
    // --------END TODO ------

    // -------- TODO -------
    public void registerForUpdates() {

        // Get and print the list of all the workers
        try {
            List<String> workers = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);
            System.out.println("List of worker nodes: " + workers);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }
    // --------END TODO ------

    // -------- TODO -------
    private void createServiceRegistryPZnode() {

        // Create a persistant znode /service_registry in zookeeper if it doesn't exist
        try {
            zooKeeperClient.createPersistantNode(REGISTRY_ZNODE, null);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
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
    // --------END TODO ------

    // -------- TODO -------
    @Override
    public void process(WatchedEvent event) {

        // In the case of node addition or deletion retrieve the updated list of workers
        switch (event.getType()) {
            case NodeChildrenChanged:
                if (event.getPath().equals(REGISTRY_ZNODE)) {
                    registerForUpdates();
                }
                break;
            default:
                // Handle other event types if needed
                break;
        }
    }
    // --------END TODO ------
}
