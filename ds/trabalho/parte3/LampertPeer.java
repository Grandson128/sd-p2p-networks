//package ds.trabalho.parte2;


import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

import java.io.File;  // Import the File class
import java.io.FileNotFoundException;  // Import this class to handle errors
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;


public class PeerHost{
    String hostName;
    int hostPort;

    public PeerHost(String hostName, int hostPort){
        this.hostName = hostName;
        this.hostPort = hostPort;
    }
}

public class LampertPeer {
    String host;
    Logger logger;
    static String port;
    List<PeerHost> peerList;

    public Lampert(String hostname) {
        host   = hostname;
        logger = Logger.getLogger("logfile");
        peerList = new ArrayList<PeerHost>();

        try {
            FileHandler handler = new FileHandler("./" + hostname + "_peer.log", true);
            logger.addHandler(handler);
            SimpleFormatter formatter = new SimpleFormatter();	
            handler.setFormatter(formatter);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws Exception {
        port = Integer.parseInt(args[1]);

        LampertPeer lampertPeer = new PampertPeer(args[0]);
        System.out.printf("new peer @ host=%s\n", args[0]);
        new Thread(new Server(args[0], port, lampertPeer.logger, lampertPeer.peerList)).start();
        new Thread(new Client(args[0], lampertPeer.logger, lampertPeer.peerList)).start();
    }


}


class Server implements Runnable{
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    List<PeerHost> peerList;
    final int secondsRandomWord = 15;

    public Server(String host, int port, Logger logger, List<PeerHost> peerList) throws Exception {
        this.host   = host;
        this.port   = port;
        this.logger = logger;
        this.peerList = peerList;
        server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

    @Override
    public void run() {
        try {
            logger.info("server: endpoint running at port " + port + " ...");

            while(true) {
                try {
                    Socket client = server.accept();
                    String clientAddress = client.getInetAddress().getHostAddress();
                    logger.info("server: new connection from " + clientAddress);
                    new Thread(new Connection(clientAddress, client, logger,this.host, this.peerList)).start();
                }catch(Exception e) {
                    e.printStackTrace();
                }    
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

class Connection implements Runnable{

    String clientAddress;
    Socket clientSocket;
    Logger logger;
    String host;
    List<PeerHost> peerList;

    public Connection(String clientAddress, Socket clientSocket, Logger logger, String host, List<PeerHost> peerList) {
        this.clientAddress = clientAddress;
        this.clientSocket  = clientSocket;
        this.logger        = logger;
        this.host          = host;
        this.peerList = peerList;
    }

    @Override
    public void run() {
        try {
            
            // get the input stream from the connected socket
            InputStream inputStream = clientSocket.getInputStream();
            // create a DataInputStream so we can read data from it.
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            // get the output stream from the socket.
            OutputStream outputStream = clientSocket.getOutputStream();
            // create an object output stream from the output stream so we can send an object through it
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);


            //Messages from CLIENT
            List<String> listOfMessages = new ArrayList<String>();
            listOfMessages = (List<String>)objectInputStream.readObject();

            //Client COMMAND
            String op = listOfMessages.get(0);
            List<String> resultMessages = new ArrayList<String>();

            


            //Send Result List
            objectOutputStream.writeObject(resultMessages);
            clientSocket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}

class Client implements Runnable{
    String  host;
    Logger  logger;
    Scanner scanner;
    List<PeerHost> peerList;

    public Client(String host, Logger logger, List<PeerHost> peerList) throws Exception {
        this.host    = host;
        this.logger  = logger; 
        this.scanner = new Scanner(System.in);
        this.peerList = peerList;
    }


    @Override 
    public void run() {
        try {
            logger.info("client: endpoint running ...\n");	
            /*
            * send messages such as:
            *   - register ip port
            *   - push ip port
            *   - pull ip port
            *   - pushpull ip port
            * ip is the address of the server, port is the port where server is listening
            */
            while (true) {
                try {
                    System.out.print("$ ");
                    String command = scanner.next();
                    String server  = scanner.next();
                    String port    = scanner.next();

                    /* 
                    * make connection
                    */
                    Socket socket  = new Socket(InetAddress.getByName(server), Integer.parseInt(port));
                    logger.info("client: connected to server " + socket.getInetAddress() + "[port = " + socket.getPort() + "]");
                    
                    // get the output stream from the socket.
                    OutputStream outputStream = socket.getOutputStream();
                    // create an object output stream from the output stream so we can send an object through it
                    ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

                    // get the input stream from the connected socket
                    InputStream inputStream = socket.getInputStream();
                    // create a DataInputStream so we can read data from it.
                    ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);




                    socket.close();
                } catch(Exception e) {
                    //e.printStackTrace();
                    System.out.println("Wrong Host/Port");
                } 
            }
        } catch(Exception e) {
            e.printStackTrace();
            //System.out.println("Wrong Host");
        }   	    
    }

}
