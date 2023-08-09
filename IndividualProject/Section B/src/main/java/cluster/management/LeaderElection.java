package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;

import static cluster.management.NetworkUtils.getIpAddress;


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

        try {
            zooKeeperClient.getZookeeper().getChildren(ELECTION_ZNODE_NAME, this);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    // -------- TODO -------
    public void registerCandidacyForLeaderElection() throws KeeperException, InterruptedException {
        participateInLeaderElection();
    }
    // --------END TODO ------

    // -------- TODO -------
    private void participateInLeaderElection() throws KeeperException, InterruptedException {


        List<String> children = zooKeeperClient.getSortedChildren(REGISTRY_ZNODE);

        // Check if a leader is already present
        if (!children.isEmpty() && children.get(0).equals(currentZnodeName)) {
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

        // Watch for leader znode deletion
        zooKeeperClient.getZookeeper().exists(znodePath, this);

        // Unregister current leader from service registry
        serviceRegistry.unregisterFromCluster();
    }
    // --------END TODO ------

    // -------- TODO -------
    public void onWorker() {
        System.out.println("I am a worker");
        try {
            // Register as a worker in the service registry and get the znode path
            String znodePath = serviceRegistry.registerToCluster(currentServerPort);
            // Get the current node's name from the znode path
            String currentNodeName = znodePath.substring(znodePath.lastIndexOf('/') + 1);
            // Get the parent znode name
            String parentZnodeName = REGISTRY_ZNODE;
            // Get the predecessor node
            String predecessorNode = zooKeeperClient.getPredecessorNode(parentZnodeName, currentNodeName);

            if (predecessorNode != null) {
                // Declare and initialize predecessorStat outside of the synchronized block
                Stat predecessorStat = zooKeeperClient.getZookeeper().exists(predecessorNode, new Watcher() {
                    @Override
                    public void process(WatchedEvent event) {
                        if (event.getType() == Event.EventType.NodeDataChanged) {
                            // Rerun leader election when predecessor node's data changes
                            try {
                                participateInLeaderElection();
                            } catch (KeeperException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });

                // Synchronize on predecessorStat
                synchronized (predecessorStat) {
                    if (predecessorStat != null) {
                        predecessorStat.wait();
                    }
                }
            }

            // Check if leader is still active before attempting to become leader
            List<String> leaderNodes = zooKeeperClient.getSortedChildren(ELECTION_ZNODE_NAME);
            String currentLeader = leaderNodes.get(0);
            if (!currentLeader.equals(currentNodeName)) {
                System.out.println("Current leader is " + currentLeader + ", not attempting leader election.");
                return;
            }

            // Proceed with leader election attempt
            participateInLeaderElection();
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
            System.out.println("Failed to register worker to cluster or watch predecessor node.");
        }
    }





    // --------END TODO ------

    public String getLeader() throws InterruptedException, KeeperException {
        List<String> leader = zooKeeperClient.getSortedChildren(ELECTION_ZNODE_NAME);

        if (leader.size() == 1 && leader != null){
            return leader.get(0);
        }
        return "none";
    }




    /**
     * Need to somehow filter for just one of the Workers when leader node is deleted --> Compare next leader with the name of the next one
     * Somehow expose the name from one of the other classes?
     *
     * @param event
     */

    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted) {
            // Handle znode deletion
            try {
                String predecessorNode = zooKeeperClient.getPredecessorNode(ELECTION_ZNODE_NAME, currentZnodeName);
                if (predecessorNode != null && event.getPath().equals(predecessorNode)) {
                    // Predecessor node deleted, rerun leader election
                    participateInLeaderElection();
                }
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (event.getType() == Event.EventType.NodeChildrenChanged && getLeader().equals("none")) {
                    try {
                        participateInLeaderElection();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (KeeperException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (KeeperException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
