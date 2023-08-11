package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;


import java.util.List;


public class LeaderElection implements Watcher {
    private static final String ELECTION_ZNODE_NAME = "/leader_election";
    private static final String REGISTRY_ZNODE = "/service_registry";
    private static final String ZNODE_PREFIX = "/guide-n_";
    private String currentZnodeName;
    private final ZookeeperClient zooKeeperClient;
    private final ServiceRegistry serviceRegistry;
    private final int currentServerPort;
    private RuntimeException runtimeException;

    public LeaderElection(ZookeeperClient zooKeeperClient, ServiceRegistry serviceRegistry, int port) {
        this.zooKeeperClient = zooKeeperClient;
        this.serviceRegistry = serviceRegistry;
        this.currentServerPort = port;

        createElectionRegistryPZnode();

    }

    public void registerCandidacyForLeaderElection() throws KeeperException, InterruptedException {
        participateInLeaderElection();
    }

    private void participateInLeaderElection() throws KeeperException, InterruptedException {
        List<String> leader = zooKeeperClient.getSortedChildren(ELECTION_ZNODE_NAME);
        List<String> children = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);

        if (leader.isEmpty()) { // If no leader --> Elect one
            onElectedToBeLeader();

        } else if (children.size() == 0) {  // If there is no previous worker, watch the leader node instead
            String leaderNodePath = ELECTION_ZNODE_NAME + "/" + leader.get(0);
            zooKeeperClient.getZookeeper().exists(leaderNodePath, this);
            System.out.println("Watching leader node: " + leaderNodePath);
            onWorker();

        } else {
            // If there is a leader & at least one worker --> We need a worker & for that worker to follow the previous node
            String previousWorkerZnodeName = children.get(children.size() - 1);
            String previousWorkerPath = REGISTRY_ZNODE + "/" + previousWorkerZnodeName;
            Stat stat = zooKeeperClient.getZookeeper().exists(previousWorkerPath, this);
            System.out.println("Watching previous worker node: " + previousWorkerZnodeName);
            onWorker();
        }
    }



    private void updateServiceRegistry(boolean isLeader) throws InterruptedException, KeeperException {
        if (isLeader) {
            onElectedToBeLeader();
        } else {
            onWorker();
        }
    }

    private void createElectionRegistryPZnode() {
        // Create a persistant znode /leader_election in zookeeper if it doesn't exist
        try {
            zooKeeperClient.createPersistantNode(ELECTION_ZNODE_NAME, null);
        } catch (KeeperException | InterruptedException e) {
        }
    }

    public void onElectedToBeLeader() throws InterruptedException, KeeperException {
        zooKeeperClient.createEphemeralSequentialNode(ELECTION_ZNODE_NAME + ZNODE_PREFIX, null);
        serviceRegistry.unregisterFromCluster();
        System.out.println("I am the leader");
        System.out.println("Updated point of contact: http://host.docker.internal:" + currentServerPort);
    }

    public void onWorker() throws InterruptedException, KeeperException {
        // Register Worker with the Leader_Election
        zooKeeperClient.createEphemeralSequentialNode(ELECTION_ZNODE_NAME + ZNODE_PREFIX, null);
        // Register Worker with the Service_registry
        serviceRegistry.registerToCluster(currentServerPort);
        // Announce success if worker registered
        System.out.println("I am a worker");
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            List<String> children = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);
            System.out.println(event.getType());
            if (event.getType() == Event.EventType.NodeDeleted) {
                // A node has been deleted, check if it's the leader or a previous worker
                String deletedNodePath = event.getPath();
                System.out.println(deletedNodePath+" *********");
                if (deletedNodePath.startsWith(REGISTRY_ZNODE + ZNODE_PREFIX)) { // This isnt triggering -- > Need to re order nodes to have all in
                    // Previous worker node deleted, trigger leader election for the corresponding worker node
                    String deletedNodeName = deletedNodePath.replace(REGISTRY_ZNODE + "/", "");
                    String currentNodeName = currentZnodeName.replace(ELECTION_ZNODE_NAME + "/", "");

                    if (deletedNodeName.equals("n_" + currentNodeName)) {
                        // Only trigger leader election for the worker that was pointing to the deleted node
                        try {
                            participateInLeaderElection();
                        } catch (KeeperException | InterruptedException e) {
                            e.printStackTrace();
                            System.out.println("Failed to participate in leader election.");
                        }
                    }
                } else if (deletedNodePath.equals(ELECTION_ZNODE_NAME + "/" + children.get(0))) {
                    // Leader node deleted, register for leader election
                }
            } else if (event.getType() == Event.EventType.NodeChildrenChanged) {
                serviceRegistry.registerForUpdates();
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw runtimeException;
        }
    }

}