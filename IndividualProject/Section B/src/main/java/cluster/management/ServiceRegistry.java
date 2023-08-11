package cluster.management;

import org.apache.zookeeper.*;

import java.nio.ByteBuffer;
import java.util.List;


public class ServiceRegistry implements Watcher {
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZookeeperClient zooKeeperClient;
    private String currentZnode = null;

    public ServiceRegistry(ZookeeperClient zooKeeperClient) {
        this.zooKeeperClient = zooKeeperClient;
        createServiceRegistryPZnode();

        // Set a watcher on the service registry znode
        try {
            zooKeeperClient.getZookeeper().getChildren(REGISTRY_ZNODE, false);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void registerToCluster(int port) throws KeeperException, InterruptedException {
        // Register as a worker in /service_registry znode
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(REGISTRY_ZNODE + "/", portToByteArray(port));
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
                byte[] data = zooKeeperClient.getZookeeper().getData(zNodePathName, false, null);
                Integer dataAsInt = byteArrayToInt(data);
                System.out.println("http://host.docker.internal:" + dataAsInt);
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

    public static byte[] portToByteArray(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int byteArrayToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return buffer.getInt();
    }


    @Override
    public void process(WatchedEvent event) {}
}
