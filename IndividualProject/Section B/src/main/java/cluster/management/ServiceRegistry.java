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
        this.createServiceRegistryPZnode();

        // Set a watcher on the service registry znode
        try {
            zooKeeperClient.getZookeeper().getChildren(REGISTRY_ZNODE, this);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // -------- TODO -------
    public void registerToCluster(int port) throws KeeperException, InterruptedException {
        // Register as a worker in /service_registry znode by adding IP address and Port number
        String ipAndPort = getIpAddress() + ":" + port;

        String znodePath = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", ipAndPort.getBytes());
        currentZnode = znodePath.replace(REGISTRY_ZNODE + "/", "");
        System.out.println("Registered as worker: " + currentZnode);

        // Set a watcher on the created worker znode
        zooKeeperClient.getZookeeper().exists(znodePath, this);

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


                String[] parts = workerData.split(":");
                if (parts.length == 2) {
                    String port = parts[1];
                    System.out.println("Worker: " + worker + ", Address:  http://host.docker.internal:" + port);
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
        // Create a persistant znode /service_registry in zookeeper if it doesn't exist
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

    }
}
