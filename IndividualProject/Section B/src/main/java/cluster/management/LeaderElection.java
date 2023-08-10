package cluster.management;

import org.apache.zookeeper.*;

import java.util.List;



public class LeaderElection implements Watcher {
    private static final String ELECTION_ZNODE_NAME = "/leader_election";
    private static final String REGISTRY_ZNODE = "/service_registry";
    private static final String ZNODE_PREFIX = "/guide-n_";
    private String currentZnodeName;
    private final ZookeeperClient zooKeeperClient;
    private final ServiceRegistry serviceRegistry;
    private final int currentServerPort;

    public LeaderElection(ZookeeperClient zooKeeperClient, ServiceRegistry serviceRegistry, int port) {
        this.zooKeeperClient = zooKeeperClient;
        this.serviceRegistry = serviceRegistry;
        this.currentServerPort = port;

        createElectionRegistryPZnode();

    }

    // -------- TODO -------
    public void registerCandidacyForLeaderElection() throws KeeperException, InterruptedException {
        participateInLeaderElection();
    }
    // --------END TODO ------

    // -------- TODO -------
    private void participateInLeaderElection() throws KeeperException, InterruptedException {


        List<String> children = zooKeeperClient.getSortedChildren(ELECTION_ZNODE_NAME);

        // If its the first node --> Leader
        if (children.isEmpty()){
            onElectedToBeLeader();
        } else {
            updateServiceRegistry(false);
        }
    }
    // --------END TODO ------

    private void updateServiceRegistry(boolean isLeader) throws InterruptedException, KeeperException {
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
        }
    }
    // --------END TODO ------

    // -------- TODO -------
    public void onElectedToBeLeader() throws InterruptedException, KeeperException {
        String znodePath = zooKeeperClient.createEphemeralSequentialNode(ELECTION_ZNODE_NAME + ZNODE_PREFIX, null);
        currentZnodeName = znodePath.replace(ELECTION_ZNODE_NAME + "/", "");

        System.out.println("I am the leader");
        System.out.println("Updated point of contact: http://host.docker.internal:" + currentServerPort);
        serviceRegistry.unregisterFromCluster();
    }
    // --------END TODO ------

    // -------- TODO -------
    public void onWorker() {

        // ------ FAULT TOLERANCE ------
        // Watch for any changes to the predecessor node, in which case rerun the leader
        // election
        System.out.println("I am a worker");
        try {
            serviceRegistry.registerToCluster(currentServerPort);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed to register worker to cluster.");
        }

    }
    // --------END TODO ------

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted) {
            // The leader znode has been deleted, a new leader is elected

        } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
            // The worker nodes have changed, so call the method to register for updates

        }
    }
}