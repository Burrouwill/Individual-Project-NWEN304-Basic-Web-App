package cluster.management;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributedLock {

    private static final String LOCK_NODE = "/locknode";
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;

    private final ZooKeeper zooKeeper;
    private final String lockPath;
    private String currentLockNode;

    private CountDownLatch lockAcquiredSignal = new CountDownLatch(1);

    public DistributedLock(String lockPath) throws IOException, InterruptedException, KeeperException {
        this.lockPath = lockPath;
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, null);
    }

    public void lock() throws KeeperException, InterruptedException {
        createLockNode();
        attemptLockAcquisition();
        waitForLockAcquiredSignal();
    }

    public void unlock() throws KeeperException, InterruptedException {
        zooKeeper.delete(currentLockNode, -1);
    }

    private void createLockNode() throws KeeperException, InterruptedException {
        String sequentialPath = zooKeeper.create(LOCK_NODE + lockPath + "/", new byte[0],
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        currentLockNode = sequentialPath;
    }

    private void attemptLockAcquisition() throws KeeperException, InterruptedException {
        while (true) {
            List<String> children = zooKeeper.getChildren(LOCK_NODE + lockPath, false);
            Collections.sort(children);

            String smallestChild = children.get(0);
            String myNode = currentLockNode.substring(currentLockNode.lastIndexOf("/") + 1);

            if (smallestChild.equals(myNode)) {
                return; // Lock acquired
            }

            String previousChild = children.get(children.indexOf(myNode) - 1);
            Stat stat = zooKeeper.exists(LOCK_NODE + lockPath + "/" + previousChild, true);

            if (stat != null) {
                lockAcquiredSignal.await();
            }
        }
    }

    private void waitForLockAcquiredSignal() throws InterruptedException {
        lockAcquiredSignal.await();
    }

}
