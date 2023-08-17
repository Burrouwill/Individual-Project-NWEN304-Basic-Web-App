package cluster.Server;

import org.apache.zookeeper.*;

import java.util.List;


public class ServiceRegistry implements Watcher {
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    public ServiceRegistry(ZookeeperClient zooKeeperClient) {
        this.zooKeeperClient = zooKeeperClient;
        createServiceRegistryPZnode();

        try {
            zooKeeperClient.getZookeeper().getChildren(REGISTRY_ZNODE, false);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void registerToCluster(int port) throws KeeperException, InterruptedException {
        // Get ip address & Process net into new Data object
        String ip = NetworkUtils.getIpAddress().replace(".","");
        Data data = new Data(ip,port);
        // Register as a worker in /service_registry znode
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", Data.convertDataToBytes(data));
        currentZnode = znodePath.replace(REGISTRY_ZNODE + "/", "");
        System.out.println("Registered as worker: " + currentZnode);
    }

    public void registerForUpdates() {
        try {
            List<String> workers = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);

            System.out.print("The cluster addresses are: [ ");
            for (int i = 0; i < workers.size(); i++) {
                if (i == 0){
                    System.out.println("");
                }
                String zNodePathName = REGISTRY_ZNODE + "/" + workers.get(i);

                // Get the Port Number from the znode / data object
                byte[] zNodeData = zooKeeperClient.getZookeeper().getData(zNodePathName, false, null);
                Data data = Data.convertBytesToData(zNodeData);
                int port = data.getPort();
                System.out.println("http://host.docker.internal:" + port);
            }
            System.out.println(" ]");

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        }
    }


    private void createServiceRegistryPZnode() {
        // Create a persistant znode /service_registry in zookeeper if it doesn't exist
        try {
            zooKeeperClient.createPersistantNode(REGISTRY_ZNODE, null);
        } catch (KeeperException | InterruptedException e) {
        }
    }


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
    public void process(WatchedEvent event) {}
}
