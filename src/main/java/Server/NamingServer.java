package Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@RestController
public class NamingServer {
    Logger logger = LoggerFactory.getLogger(NamingServer.class);
    private static final TreeMap<Integer, String> ipMapping = new TreeMap<>();
    static ReadWriteLock ipMapLock = new ReentrantReadWriteLock(); //lock to avoid reading when someone else is writing and vice versa
    DiscoveryHandler discoveryHandler;

    public NamingServer() {
        this.discoveryHandler = new DiscoveryHandler(this);
        discoveryHandler.start();
    }

    public static TreeMap<Integer, String> getIpMapping() {
        return ipMapping;
    }

    public static ReadWriteLock getIpMapLock() {
        return ipMapLock;
    }


    public int hash(String string) {
        this.logger.info("Calculating hash of: " + string);
        long max = 2147483647;
        long min = -2147483648;
        return (int) (((long) string.hashCode() + max) * (32768.0 / (max + Math.abs(min))));
    }
    @PostMapping("/NamingServer/addNode")
    public int addNode(String name, String IP){
        int hash = hash(name);
        this.logger.info("Adding node: " + name + " with hash: " + hash);
        ipMapLock.writeLock().lock();
        if (ipMapping.containsKey(hash)){
            ipMapLock.writeLock().unlock();
            return -1;
        }
        ipMapping.put(hash, IP);
        JSON_Handler.writeFile();
        ipMapLock.writeLock().unlock();
        return hash;
    }
    @DeleteMapping("/NamingServer/removeNode")
    public int removeNode(@RequestParam String name){
        int hash = hash(name);
        this.logger.info("Removing node: " + name + "with hash: " + hash);
        ipMapLock.writeLock().lock();
        if (ipMapping.containsKey(hash)) {
            ipMapping.remove(hash);
            JSON_Handler.writeFile();
        }
        ipMapLock.writeLock().unlock();
        return hash;
    }
    @GetMapping("/NamingServer/getFile")
    public String getFile(@RequestParam String fileName) {
        this.logger.info("Where is file?: " + fileName);
        int hash = hash(fileName);
        Map.Entry<Integer,String> entry; //get an entry from the map
        ipMapLock.readLock().lock();
        if(ipMapping.floorEntry(hash-1) == null){
            entry = ipMapping.lastEntry();
        }else{
            entry = ipMapping.floorEntry(hash-1); //returns closest value lower than or equal to key (so where the file is located)
        }
        ipMapLock.readLock().unlock();
        return entry.getValue();
    }
    private static class DiscoveryHandler extends Thread{
        NamingServer nameServer;
        boolean running = false;
        DatagramSocket socket;
        public DiscoveryHandler(NamingServer nameServer) {
            this.nameServer = nameServer;
            try {
                this.socket = new DatagramSocket(8001);
                this.socket.setBroadcast(true);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            File deleteFile = new File("src/main/resources/nodeMapping.json");
            deleteFile.delete();
            this.running = true;
            byte[] buf = new byte[512];
            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
            while (this.running) {
                try {
                    this.socket.receive(receivePacket);
                    System.out.println("Discovered node" + receivePacket.getAddress() + ":" + receivePacket.getPort());
                    String buffer = new String(receivePacket.getData()).trim();
                    String IP = receivePacket.getAddress().getHostAddress();
                    int hash = this.nameServer.addNode(buffer,IP);
                    String send;
                    if (hash != -1){
                        int lowerNeighbour;
                        int higherNeighbour;
                        ipMapLock.readLock().lock();
                        if(getIpMapping().lowerKey(hash-1) != null){ // If there is a lower node
                            lowerNeighbour = getIpMapping().lowerKey(hash-1); // get this node
                        }else{
                            lowerNeighbour = getIpMapping().lastKey();  // if there is no lower node, the node himself is this node
                        }
                        if(getIpMapping().higherKey(hash+1) != null){
                            higherNeighbour = getIpMapping().higherKey(hash+1);
                        }else{
                            higherNeighbour = getIpMapping().firstKey();
                        }
                        send = "{\"node\":\"Added successfully\"," +
                                "\"node hash\":" + hash + "," +
                                "\"nodes\":" + getIpMapping().size() + "," +
                                "\"lowerNeighbour hash\":" + lowerNeighbour + "," +
                                "\"higherNeighbour hash\":" + higherNeighbour + "}";
                        ipMapLock.readLock().unlock();
                    }else{
                        //adding unsuccessful
                        this.nameServer.logger.info("Error: node was not added");
                        send = "{\"node\":\"Error: Node was not added\"}";
                    }
                    DatagramPacket sendPacket = new DatagramPacket(send.getBytes(StandardCharsets.UTF_8), send.length(), receivePacket.getAddress(), receivePacket.getPort());
                    this.socket.send(sendPacket);

                } catch (IOException ignore) {}
            }
        }
    }
}
