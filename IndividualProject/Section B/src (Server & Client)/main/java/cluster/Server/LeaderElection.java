package cluster.Server;

import org.apache.zookeeper.*;

import java.util.List;


public class LeaderElection implements Watcher {
    private static final String ELECTION_ZNODE_NAME = "/leader_election";
    private static final String LEADER_ZNODE = "/leader_znode";
    private static final String REGISTRY_ZNODE = "/service_registry";
    private static final String ZNODE_PREFIX = "/guide-n_";
    private final ZookeeperClient zooKeeperClient;
    private final ServiceRegistry serviceRegistry;
    private final int currentServerPort;

    public LeaderElection(ZookeeperClient zooKeeperClient, ServiceRegistry serviceRegistry, int port) {
        this.zooKeeperClient = zooKeeperClient;
        this.serviceRegistry = serviceRegistry;
        this.currentServerPort = port;
        createElectionRegistryPZnode();
        createLeaderPZnode();
    }

    public void registerCandidacyForLeaderElection() throws KeeperException, InterruptedException {
        participateInLeaderElection();
    }

    private void participateInLeaderElection() throws KeeperException, InterruptedException {
        List<String> leadersElectionNodes = zooKeeperClient.getSortedChildren(ELECTION_ZNODE_NAME);
        List<String> leaderNode = zooKeeperClient.getSortedChildren(LEADER_ZNODE);
        // If no leader --> Elect one
        if (leaderNode.size() == 0) {
            onElectedToBeLeader();
        } else {
            // If there is a leader --> We need a worker & for that worker to follow the previous node
            String previousWorkerZnodeName = leadersElectionNodes.get(leadersElectionNodes.size() - 1);
            String previousWorkerPath = ELECTION_ZNODE_NAME + "/" + previousWorkerZnodeName;
            zooKeeperClient.getZookeeper().exists(previousWorkerPath, this);
            String[] splitPath = previousWorkerPath.split("_");
            String previousWorkderID = splitPath[1];
            System.out.println("Watching previous node: " + previousWorkerZnodeName);
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

    /**
     * This permanent Z Node holds the instance of the leader ephemeral node (If there is one).
     */
    private void createLeaderPZnode() {
        // Create a persistant znode /leader_node in zookeeper if it doesn't exist
        try {
            zooKeeperClient.createPersistantNode(LEADER_ZNODE, null);
        } catch (KeeperException | InterruptedException e) {
    }

    }

    public void onElectedToBeLeader() throws InterruptedException, KeeperException {
        // Create the Leader node & register it with /leader_election & /leader_znode
        zooKeeperClient.createEphemeralSequentialNode(ELECTION_ZNODE_NAME + ZNODE_PREFIX, null);
        zooKeeperClient.createEphemeralSequentialNode(LEADER_ZNODE + ZNODE_PREFIX, null);
        serviceRegistry.unregisterFromCluster();
        serviceRegistry.registerForUpdates();

        // Leader watches all the workers
        zooKeeperClient.getZookeeper().getChildren(REGISTRY_ZNODE,this);

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

            if (event.getType() == Event.EventType.NodeDeleted) {
                // Only undergo leader election if we don't have a
                // leader (Prevents ghost nodes when worker leaves
                // & registers because the worker node it was watching left)
                if (zooKeeperClient.getSortedChildren(LEADER_ZNODE).size() == 0) {
                    participateInLeaderElection();
                }
            }

            if (event.getType() == Event.EventType.NodeChildrenChanged) {
                serviceRegistry.registerForUpdates();
                // Reset the watcher
                zooKeeperClient.getZookeeper().getChildren(REGISTRY_ZNODE,this);

            }

        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        } catch (KeeperException ex) {
            throw new RuntimeException(ex);
        }
    }

    }