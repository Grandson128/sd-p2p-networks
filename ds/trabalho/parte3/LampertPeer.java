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

import java.sql.Timestamp;

class PeerHost{
    String hostName;
    int hostPort;

    public PeerHost(String hostName, int hostPort){
        this.hostName = hostName;
        this.hostPort = hostPort;
    }

    public void printPeerHost(Logger logger){
        //logger.info("server: hostname: " +hostName + " port: " + hostPort);
        System.out.println("server: hostname -> " +hostName + " ; port -> " + hostPort);

    }

    public boolean equals(PeerHost peer){
        try{
            if(InetAddress.getByName(peer.hostName).equals(InetAddress.getByName(this.hostName)) && peer.hostPort == this.hostPort){
                return true;
            }
            //return false;
        }catch(UnknownHostException e){
            e.printStackTrace();
        }finally{
            
        }

        return false;
    }

    public boolean inList(List<PeerHost> peers){
        for(PeerHost peer : peers){ 
            if(peer.equals(this)){
                return true;
            }
        }
        return false;
    }

}

public class LampertPeer {
    String host;
    Logger logger;
    static String port;
    List<PeerHost> peerList;

    public LampertPeer(String hostname) {
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
        // port = Integer.parseInt(args[1]);

        LampertPeer lampertPeer = new LampertPeer(args[0]);
        System.out.printf("new peer @ host=%s\n", args[0]);
        new Thread(new Server(args[0], Integer.parseInt(args[1]), lampertPeer.logger, lampertPeer.peerList)).start();
        new Thread(new Client(args[0], Integer.parseInt(args[1]), lampertPeer.logger, lampertPeer.peerList)).start();
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
                    //logger.info("server: new connection from " + clientAddress);
                    new Thread(new Connection(clientAddress, port, client, logger,this.host, this.peerList)).start();
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
    int port;
    List<PeerHost> peerList;

    public Connection(String clientAddress, int port, Socket clientSocket, Logger logger, String host, List<PeerHost> peerList) {
        this.clientAddress = clientAddress;
        this.clientSocket  = clientSocket;
        this.logger        = logger;
        this.host          = host;
        this.peerList = peerList;
        this.port = port;
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

            if(op.equals("register")){
                int clientPort = Integer.parseInt(listOfMessages.get(2));
                //Register client peer
                int regist = register(clientAddress, clientPort, logger);

                if(regist == 1){
                    //List with register comman in head
                    resultMessages.add("register");
                    //List with server host name and port in body
                    resultMessages.add(this.host);
                    resultMessages.add(String.valueOf(this.port));
                }else{
                    //List with register comman in head
                    resultMessages.add("ERROR");
                }

            }else{
                String message = op;
                String time = listOfMessages.get(1);
                String peerHost = listOfMessages.get(2);
                String peerPort = listOfMessages.get(3);

                Timestamp timestamp = new Timestamp(System.currentTimeMillis());

                System.out.println("\nTime of arrival: "+ timestamp.toString());
                System.out.println("Time from origin: "+time);
                System.out.println("Message From("+peerHost+":"+peerPort+"): "+message);
            }

            //Send Result List
            objectOutputStream.writeObject(resultMessages);
            clientSocket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add Client Host to table
     * @param targetPeerHost
     */
    public int register(String targetPeerHost, int port, Logger logger){
        PeerHost newPeer = new PeerHost(targetPeerHost, port);

        if(!newPeer.inList(peerList)){
            peerList.add(newPeer);
            //logger.info("Server: added new peer --> Host: "+targetPeerHost+" Port: "+String.valueOf(port));
            //logger.info("Server: Current Peer Ip Table: ");
            System.out.println("Server: Current Peer Ip Table: ");
            printPeerHostList(logger);
            return 1;
        }else{
            //logger.info("Server: ERROR - Didn't register given peer, already exists");
            System.out.println("Server: ERROR - Didn't register given peer, already exists");
            return 0;
        }
    }

    public void printPeerHostList(Logger logger){
        peerList.forEach((host) -> {
            host.printPeerHost(logger);
        });
    }


}

class Client implements Runnable{
    String  host;
    Logger  logger;
    Scanner scanner;
    int port;
    List<PeerHost> peerList;

    public Client(String host, int port, Logger logger, List<PeerHost> peerList) throws Exception {
        this.host    = host;
        this.logger  = logger; 
        this.scanner = new Scanner(System.in);
        this.peerList = peerList;
        this.port = port;
    }

    @Override 
    public void run() {
        try {
            //logger.info("client: endpoint running ...\n");	
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
                    
                    List<String> messages = new ArrayList<String>();
                    messages.add(command);

                    //List to hold server reply
                    List<String> resultMessages = new ArrayList<String>();

    
                    /**
                     * Prepare List of messages
                     */
                    if(command.equals("register")){
                        String server  = scanner.next();
                        String port = scanner.next(); //Ask new peer port
                        messages.add(this.host);
                        messages.add(String.valueOf(this.port));

                        resultMessages = handleRegisterCommand(messages, server, port);
                        register(resultMessages, server, port, logger);

                    }else{
                        String chatMessage = command;
                        chatMessage = chatMessage + scanner.nextLine();
                        messages.clear();
                        messages.add(chatMessage);
                        //System.out.println("GOT :" + message);
                        messageAll(messages, this.host, String.valueOf(this.port));

                    }

                    
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


    public List<String> handleRegisterCommand(List<String> messages, String server, String port){

        try{
            /* 
            * make connection
            */
            Socket socket  = new Socket(InetAddress.getByName(server), Integer.parseInt(port));
            //logger.info("client: connected to server " + socket.getInetAddress() + "[port = " + socket.getPort() + "]");
            
            // get the output stream from the socket.
            OutputStream outputStream = socket.getOutputStream();
            // create an object output stream from the output stream so we can send an object through it
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);

            // get the input stream from the connected socket
            InputStream inputStream = socket.getInputStream();
            // create a DataInputStream so we can read data from it.
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

            //Send Data
            objectOutputStream.writeObject(messages);

            List<String> resultMessages = new ArrayList<String>();

            try{            
                resultMessages = (List<String>)objectInputStream.readObject();
            }catch(ClassNotFoundException e){ e.printStackTrace();}

            socket.close();

            return resultMessages;
        }catch(IOException e){
            e.printStackTrace();
        }   

        return null;
    }


    public void messageAll(List<String> messages, String server, String port){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        String holdTimeOfCreation = "";

        for(PeerHost peer : peerList){ 
            try{
                /* 
                * make connection
                */
                Socket socket  = new Socket(InetAddress.getByName(peer.hostName), peer.hostPort);
                //logger.info("client: connected to server " + socket.getInetAddress() + "[port = " + socket.getPort() + "]");
                
                // get the output stream from the socket.
                OutputStream outputStream = socket.getOutputStream();
                // create an object output stream from the output stream so we can send an object through it
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
    
                // get the input stream from the connected socket
                InputStream inputStream = socket.getInputStream();
                // create a DataInputStream so we can read data from it.
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                //Add timestamp to message
                
                holdTimeOfCreation = timestamp.toString();
                messages.add(timestamp.toString());
                messages.add(server);
                messages.add(port);
    
                //Send Data
                objectOutputStream.writeObject(messages);
                
                
                List<String> resultMessages = new ArrayList<String>();
    
                try{            
                    resultMessages = (List<String>)objectInputStream.readObject();
                }catch(ClassNotFoundException e){ e.printStackTrace();}
                

                socket.close();

            }catch(IOException e){
                e.printStackTrace();
            }finally{

            }
            
        }
        
        System.out.println("\nTime of arrival: "+ timestamp.toString());
        System.out.println("Time from origin: "+holdTimeOfCreation);
        System.out.println("Message From("+server+":"+port+"): "+messages.get(0));
    }

    /**
     * Add server host to the peer table
     * @param result
     * @param server
     */
    public void register(List<String> result, String server, String port, Logger logger){
        String resultCommand = result.get(0);

        if(!resultCommand.equals("register")){
            logger.info("Client: ERROR - Didn't register peer");
        }else{
            String resultServerHost = result.get(1);
            String resultServerPort = result.get(2);

            if (resultServerHost.equals(server) && resultServerPort.equals(port)){
                PeerHost newPeer = new PeerHost(resultServerHost, Integer.parseInt(resultServerPort));

                if(!newPeer.inList(peerList)){
                    peerList.add(newPeer);
                    //logger.info("Client: added new peer --> Host: "+resultServerHost+" Port: "+resultServerPort);
                    //logger.info("Client: Current Peer Ip Table: ");
                    System.out.println("Client: Current Peer Ip Table: ");
                    printPeerHostList(logger);
                }else{
                    // logger.info("Client: ERROR - Didn't register given peer, already exists");
                    System.out.println("Client: ERROR - Didn't register given peer, already exists");
                }
            }else{
                //logger.info("Client: ERROR - Didn't register peer");
            }
        }        
    }

    public void printPeerHostList(Logger logger){
        peerList.forEach((host) -> {
            host.printPeerHost(logger);
        });
    }

}
