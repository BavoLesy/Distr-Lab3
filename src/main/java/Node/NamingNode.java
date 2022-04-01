package Node;

import com.mashape.unirest.http.Unirest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.*;

public class NamingNode{
    private final String name;
    private String node_IP;
    private String namingServer_IP;
    private int hash;
    private int nodes;
    private int lowerNeighbour;
    private int higherNeighbour;

    public NamingNode(String name) { //constructor
        //turn off most of the logging
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.apache.http");
        root.setLevel(ch.qos.logback.classic.Level.OFF);
        this.name = name;
    }
    //Network discovery of the Naming Server
    public void discoveryNamingServer() throws IOException {
        boolean received = false;
        InetAddress broadcast = InetAddress.getByName("255.255.255.255"); //Broadcast
        DatagramSocket socket = new DatagramSocket(8000); // receiving port
        socket.setSoTimeout(1000); // wait for 1 s when we try to receive()
        byte[] receive = new byte[512];
        DatagramPacket sendPacket = new DatagramPacket(name.getBytes(), name.length(), broadcast, 8001); //broadcast on port 8001
        DatagramPacket receivePacket = new DatagramPacket(receive, receive.length);  // receivePacket
        while (!received) { // send a datagram packet until the NamingServer answers

            socket.send(sendPacket);
            System.out.println("sent packet to: " + sendPacket.getSocketAddress());

            try { // If we receive
                socket.receive(receivePacket); // So now timeout for 1s
                System.out.println("received packet from: " + receivePacket.getSocketAddress());
                String data = new String(receivePacket.getData()).trim();
                System.out.println("received data: " + data);
                this.node_IP = InetAddress.getLocalHost().getHostAddress();
                this.namingServer_IP = String.valueOf(receivePacket.getAddress().getHostAddress());

                JSONParser parser = new JSONParser();
                Object obj = parser.parse(data);
                String status = ((JSONObject)obj).get("node").toString();

                if (status.equals("Added successfully")){
                    this.hash =   (int) (long)(((JSONObject)obj).get("node hash"));
                    this.nodes  = (int) (long)(((JSONObject)obj).get("nodes"));
                    this.lowerNeighbour = (int) (long)(((JSONObject)obj).get("lowerNeighbour hash"));
                    this.higherNeighbour = (int) (long) (((JSONObject)obj).get("higherNeighbour hash"));
                }else if (status.equals("Error: Node was not added")){
                    System.out.println("Error: Node was not added");
                }
                received = true;
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
    public void getFile(String filename) {
        try {
            String url = "http://" + this.namingServer_IP+ ":8081/NamingServer/getFile?fileName="+filename; //REST command
            System.out.println(filename + "is stored at: " + Unirest.get(url).asString().getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void delete(){
        try {
            String url = "http://" + this.namingServer_IP + ":8081/NamingServer/removeNode?name=" + this.name;
            System.out.println("Deleting Node with hash: " + Unirest.delete(url).asString().getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void printOut() throws UnknownHostException {
        System.out.println("Node IP:\t\t" + this.node_IP);
        System.out.println("NamingServer IP:\t" + this.namingServer_IP);
        System.out.println("Node hash:\t\t" + this.hash);
        System.out.println("Nodes:\t\t\t" + this.nodes);
        System.out.println("lowerNeighbour hash:\t" + this.lowerNeighbour);
        System.out.println("higherNeighbour hash:\t" + this.higherNeighbour);
        System.out.println("node hostname + IP : " + InetAddress.getLocalHost());
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting Node...");
        String name;
        if (args.length > 0) {
            name = args[0];
        } else {
            System.out.println("Please give a name to your node!");
            return;
        }

        NamingNode node = new NamingNode(name);
        node.discoveryNamingServer();
        node.printOut();
        //test some files
        node.getFile("testFile.txt");
        node.getFile("testFile2.pdf");
        node.getFile("testFile3.jpg");
        //node.delete();
    }
}