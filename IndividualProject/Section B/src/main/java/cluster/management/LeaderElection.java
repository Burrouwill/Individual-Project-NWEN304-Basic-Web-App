package cluster.management;

import org.apache.zookeeper.*;

import java.util.List;


public class LeaderElection implements Watcher {
    private static final String ELECTION_ZNODE_NAME = "/leader_election";
    private static final String ZNODE_PREFIX = "/guide-n_";
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZookeeperClient zooKeeperClient;
    private final ServiceRegistry serviceRegistry;
    private final int currentServerPort;

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
        List<String> leadersElectionNodes = zooKeeperClient.getSortedChildren(ELECTION_ZNODE_NAME);
        List<String> serviceNodes = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);
        // If no leader --> Elect one
        if (leadersElectionNodes.size() == serviceNodes.size()) {
            onElectedToBeLeader();
        } else {
            // If there is a leader --> We need a worker & for that worker to follow the previous node
            String previousWorkerZnodeName = leadersElectionNodes.get(leadersElectionNodes.size() - 1);
            String previousWorkerPath = ELECTION_ZNODE_NAME + "/" + previousWorkerZnodeName;
            zooKeeperClient.getZookeeper().exists(previousWorkerPath, this);
            System.out.println("Watching previous worker node: " + previousWorkerZnodeName);
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
            if (event.getType() == Event.EventType.NodeDeleted) {
                participateInLeaderElection();
            }
        } catch (KeeperException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}