import Server.NamingServer;

import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class UnitTest {
    protected static NamingServer ns;

    @BeforeAll
    static void beforeAll() {
        System.out.println("Before All");
        ns = new NamingServer();
    }

    @AfterEach
    void terminate() {
        System.out.println("terminate previous");
        NamingServer.getIpMapping().clear();
    }

    @Test
    public void addNodeUnique() {
        ns.addNode("Bavo", "192.168.80.3");
        ns.addNode("Jeoffrey", "192.168.80.5");
        System.out.println("nodes: " + NamingServer.getIpMapping().toString());
        Assertions.assertEquals("192.168.80.3", NamingServer.getIpMapping().get(ns.hash("Bavo")));
        Assertions.assertEquals(2, NamingServer.getIpMapping().size());
    }

    @Test
    public void addNodeExisting() {
        String name = "Bavo";
        int hash = ns.hash(name);
        Assumptions.assumeTrue(ns.addNode("Bavo", "192.168.80.3").equals("Added Node " + name + " with hash: " + hash + "\n"));
        Assumptions.assumeTrue(ns.addNode("Bavo", "192.168.80.3").equals("Node " + name + " with hash: " + hash + " already exists or has the same hash as another node\n")); //should return -1 because already exists
        Assertions.assertEquals(1, NamingServer.getIpMapping().size());
    }


    @Test
    public void sendFileName() {
        ns.addNode("Bavo", "192.168.80.3");
        ns.addNode("Jeoffrey", "192.168.80.5");
        int hash = ns.hash("Jeoffrey");
        ns.addNode("Max", "192.168.80.2");
        ns.addNode("Oliver", "192.168.80.4");
        ns.addNode("King", "192.168.80.6");
        System.out.println("nodes: " + NamingServer.getIpMapping().toString());
        String myStr = "test.txt";
        System.out.println("testFile.txt = " + ns.hash(myStr));
        Assertions.assertEquals("The file " + myStr + " is located at: 192.168.80.5" + "\n", ns.getFile(myStr));
    }


    @Test
    public void sendFileNameSmaller(){
        ns.addNode("Bavo", "192.168.80.3");
        ns.addNode("Jeoffrey", "192.168.80.5");
        ns.addNode("Max", "192.168.80.2");
        ns.addNode("Oliver", "192.168.80.4");
        ns.addNode("King", "192.168.80.6");
        int hash = 32768; // highest possible of hash
        String randomString = ""; //try random names until the hash is lower than >1000
        while (hash >= 1000){
            byte[] array = new byte[10];
            new Random().nextBytes(array);
            randomString = new String(array, StandardCharsets.UTF_8);
            hash = ns.hash(randomString);
        }
        Assertions.assertEquals("The file " + randomString + " is located at: 192.168.80.6" + "\n", ns.getFile(randomString));

    }
}







