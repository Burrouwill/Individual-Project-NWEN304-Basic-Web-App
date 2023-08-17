public class ApplicationC {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args){

        try {
            int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;

            ZookeeperClientC zooKeeperClientC = new ZookeeperClientC(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT);

            DistributedHashTable distributedHashTable = new DistributedHashTable(zooKeeperClientC, currentServerPort);

            zooKeeperClientC.run();
            zooKeeperClientC.close();
            System.out.println("Disconnected from Zookeeper, exiting application");
        } catch (Exception ex) {
            System.out.println(ex.toString());

        }
    }
}
