
import org.apache.zookeeper.KeeperException;
import java.io.IOException;
import java.util.Hashtable;

public class Application {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

        try {
            int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

            NodeHashTable nodeHashTable = new NodeHashTable();

            ZookeeperClient zooKeeperClient = new ZookeeperClient(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT);

            NodeRegistry nodeRegistry = new NodeRegistry(zooKeeperClient, currentServerPort, nodeHashTable);

            nodeRegistry.registerToHashTable(currentServerPort);

            zooKeeperClient.run();
            zooKeeperClient.close();
            System.out.println("Disconnected from Zookeeper, exiting application");
        } catch (Exception ex) {
            System.out.println(ex.toString());

        }
    }
}
