package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;

public class LeaderElection implements Watcher {
    private static final String ELECTION_ZNODE_NAME = "/leader_election";
    private static final String ZNODE_PREFIX = "/guide-n_";
    private String currentZnodeName;
    private final ZookeeperClient zooKeeperClient;
    private final ServiceRegistry serviceRegistry;
    private final int currentServerPort;

    public LeaderElection(ZookeeperClient zooKeeperClient, ServiceRegistry serviceRegistry, int port) {
        this.zooKeeperClient = zooKeeperClient;
        this.serviceRegistry = serviceRegistry;
        this.currentServerPort = port;

        // Invoke this method to create a persistant znode /leader_election in zookeeper
        // if it doesn't exist
        if (zooKeeperClient.getZookeeper() == null){
            createElectionRegistryPZnode();
        }
    }




    // -------- TODO -------
    public void registerCandidacyForLeaderElection() throws KeeperException, InterruptedException {
        // Create a ephermeral sequential node under election znode and assign it to the
        // string currentZnodeName
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(ELECTION_ZNODE_NAME + ZNODE_PREFIX, null);
        currentZnodeName = znodePath.replace(ELECTION_ZNODE_NAME + "/", "");
        participateInLeaderElection();

    }
    // --------END TODO ------

    // -------- TODO -------
    private void participateInLeaderElection() throws KeeperException, InterruptedException {
        List<String> children = zooKeeperClient.getSortedChildren(ELECTION_ZNODE_NAME);
        String smallestChild = children.get(0);

        int currentSequenceNumber = Integer.parseInt(currentZnodeName.substring(currentZnodeName.lastIndexOf('_') + 1));
        int smallestSequenceNumber = Integer.parseInt(smallestChild.substring(smallestChild.lastIndexOf('_') + 1));

        if (currentSequenceNumber == smallestSequenceNumber) {
            System.out.println("I am the leader");
            updateServiceRegistry(true);
        } else {
            System.out.println("I am a worker");
            updateServiceRegistry(false);
        }
    }
    // --------END TODO ------

    private void updateServiceRegistry(boolean isLeader) {
        if (isLeader) {
            onElectedToBeLeader();
        } else {
            onWorker();
        }
    }

    // -------- TODO -------
    private void createElectionRegistryPZnode() {
        // Create a persistant znode /leader_election in zookeeper if it doesn't exist
        try {
            zooKeeperClient.createPersistantNode(ELECTION_ZNODE_NAME, null);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed To Create Election Registry");
        }
    }
    // --------END TODO ------

    // -------- TODO -------
    public void onElectedToBeLeader() {
        System.out.println("I am the leader");
        serviceRegistry.registerForUpdates();
        serviceRegistry.unregisterFromCluster(); // Handle fault tolerance
    }
    // --------END TODO ------

    // -------- TODO -------
    public void onWorker() {

        // Print an appropriate message on console - "I am a worker".
        // Register as a worker in /service_registry znode
        // (ServiceRegistry.registerToCluster(int port) method)

        // ------ FAULT TOLERANCE ------
        // Watch for any changes to the predecessor node, in which case rerun the leader
        // election
        System.out.println("I am a worker");
        try {
            serviceRegistry.registerToCluster(currentServerPort);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

    }
    // --------END TODO ------

    @Override
    public void process(WatchedEvent event) {
        // -------- TODO ------
        switch (event.getType()) {
            case NodeDeleted:
                if (event.getPath().equals(ELECTION_ZNODE_NAME + "/" + currentZnodeName)) {
                    System.out.println("Leader node " + currentZnodeName + " has failed. Re-running leader election.");
                    try {
                        participateInLeaderElection();
                    } catch (KeeperException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
        // ------- END TODO ------
    }
}
