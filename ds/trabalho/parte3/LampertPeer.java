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
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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
                
                System.out.println("Foreign Peer host address: "+InetAddress.getByName(peer.hostName).toString());
                System.out.println("This Peer host address: "+InetAddress.getByName(this.hostName).toString());

                System.out.println("Foreign Peer host port: "+peer.hostPort);
                System.out.println("This Peer host port: "+this.hostPort);
                
                
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


class QueueComparator implements Comparator<List<String>>{

    public int compare(List<String> list1, List<String> list2){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        Date parsedTimeList1 = null;
        Date parsedTimeList2 = null;

        try{
            parsedTimeList1 = dateFormat.parse(list1.get(1));
            parsedTimeList2 = dateFormat.parse(list2.get(1));
        }catch(Exception e){
            e.printStackTrace();
        }finally{

        }

        Timestamp timeList1 = new Timestamp(parsedTimeList1.getTime());
        Timestamp timeList2 = new Timestamp(parsedTimeList2.getTime());

        if(timeList1.before(timeList2)){
            return -1;
        }else if(timeList1.after(timeList2)){
            return 1;
        }

        return 0;
    }

}

public class LampertPeer {
    String host;
    Logger logger;
    static String port;
    List<PeerHost> peerList;
    PriorityQueue<List<String>> messageQueue;

    public LampertPeer(String hostname) {
        host   = hostname;
        logger = Logger.getLogger("logfile");
        peerList = new ArrayList<PeerHost>();
        messageQueue = new PriorityQueue<List<String>>(new QueueComparator());

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
        new Thread(new Server(args[0], Integer.parseInt(args[1]), lampertPeer.logger, lampertPeer.peerList, lampertPeer.messageQueue)).start();
        new Thread(new Client(args[0], Integer.parseInt(args[1]), lampertPeer.logger, lampertPeer.peerList, lampertPeer.messageQueue)).start();
    }

}


class Server implements Runnable{
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    List<PeerHost> peerList;
    final int secondsRandomWord = 15;
    PriorityQueue<List<String>> messageQueue;


    public Server(String host, int port, Logger logger, List<PeerHost> peerList, PriorityQueue<List<String>> messageQueue) throws Exception {
        this.host   = host;
        this.port   = port;
        this.logger = logger;
        this.peerList = peerList;
        this.messageQueue = messageQueue;
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
                    new Thread(new Connection(clientAddress, port, client, logger,this.host, this.peerList, this.messageQueue)).start();
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
    PriorityQueue<List<String>> messageQueue;

    public Connection(String clientAddress, int port, Socket clientSocket, Logger logger, String host, List<PeerHost> peerList, PriorityQueue<List<String>> messageQueue) {
        this.clientAddress = clientAddress;
        this.clientSocket  = clientSocket;
        this.logger        = logger;
        this.host          = host;
        this.peerList = peerList;
        this.port = port;
        this.messageQueue = messageQueue;
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
                int regist = register(listOfMessages.get(1), clientPort, logger);

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

                //Send Result List
                objectOutputStream.writeObject(resultMessages);
                clientSocket.close();

            }else{

                /**
                 * Lamper Clock
                 */
                Timestamp bleatTime = null;
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                Timestamp originTime = stringToTimestamp(listOfMessages.get(1));
                
                if(timestamp.after(originTime)){
                    bleatTime=timestamp;
                    bleatTime.setTime(bleatTime.getTime()+1000);
                }else{
                    bleatTime=originTime;
                    bleatTime.setTime(bleatTime.getTime()+1000);
                }

                /**
                 * Send Bleats
                 */
                if(!listOfMessages.get(0).equals("bleat")){
                    // System.out.println("Got message: "+listOfMessages.toString());
                    sendBleat(bleatTime.toString());
                    
                }else{
                    System.out.println("Got bleat from: "+listOfMessages.get(3));
                }
                
                //Add time of arrival
                listOfMessages.add(timestamp.toString());

                //Add message to priority queue
                List<String> messageListToQueue = new ArrayList<String>();
                messageListToQueue.addAll(listOfMessages);
                
                messageQueue.add(messageListToQueue);

                // System.out.println("\n---------server PRIORITY QUEUE----------");
                // printPriorityQueue(messageQueue);
                // System.out.println("\n---------PRIORITY QUEUE----------\n");

                //Handle message order
                displayMessage(messageQueue);
                
            }

            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Timestamp stringToTimestamp(String time){
        Timestamp goodTime = Timestamp.valueOf(time);
        return goodTime;
    }

    public void sendBleat(String bleatTime){
        List<String> bleat = new ArrayList<String>();
        bleat.add("bleat");
        bleat.add(bleatTime);
        bleat.add(this.host);
        bleat.add(String.valueOf(this.port));
        //Add own bleat
        messageQueue.add(bleat);

        for(PeerHost peer : this.peerList){ 
            System.out.println(this.port+"sent bleat to: "+peer.hostPort);
            try{
                /**
                 * create bleat
                 */
                List<String> bleatToSend = new ArrayList<String>();
                bleatToSend.addAll(bleat);
                
                        /* 
                * make connection
                */
                Socket socket  = new Socket(InetAddress.getByName(peer.hostName), peer.hostPort);
                // get the output stream from the socket.
                OutputStream outputStream = socket.getOutputStream();
                // create an object output stream from the output stream so we can send an object through it
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                //Send Bleat
                objectOutputStream.writeObject(bleatToSend);
                socket.close();
            }catch(IOException e){
                e.printStackTrace();
            }finally{}
        }
    }

    public boolean allPeerExistInMessageQueue(PriorityQueue<List<String>> messageQueue){
        int nPeersCounter=0;
        int totalPeers = peerList.size()+1;
        PriorityQueue auxQueue = new PriorityQueue<List<String>>(new QueueComparator());
        auxQueue.addAll(messageQueue);
        List<PeerHost> peersVisited = new ArrayList<PeerHost>();

        for(PeerHost peer : peerList){
            // System.out.println("FROM PEER LIST: "+peer.hostPort+ " "+peer.hostName);
            while(!auxQueue.isEmpty()){
                List<String> auxList = (List<String>)auxQueue.poll();
                PeerHost auxPeer = new PeerHost(auxList.get(2), Integer.parseInt(auxList.get(3)));

                // System.out.println("SPAM from list: "+auxPeer.hostPort+ " "+peer.hostName);

                if(peer.equals(auxPeer) && !peer.inList(peersVisited)){
                    // System.out.println("EY FOUND THIS GUY: "+auxPeer.hostPort+ " "+peer.hostName);
                    peersVisited.add(auxPeer);
                    nPeersCounter++;
                    break;
                }
            }
        }


        /**
         * Search for current peer bleat/message
         */
        auxQueue.clear();
        auxQueue.addAll(messageQueue);

        while(!auxQueue.isEmpty()){
            List<String> auxList = (List<String>)auxQueue.poll();
            PeerHost auxPeer = new PeerHost(auxList.get(2), Integer.parseInt(auxList.get(3)));
            if(auxPeer.hostName.equals(this.host) && auxPeer.hostPort == this.port && !auxPeer.inList(peersVisited)){
                nPeersCounter++;
                peersVisited.add(auxPeer);
            }
            
        }

        if(nPeersCounter == totalPeers){
            return true;
        }

        return false;
    }


    public void displayMessage(PriorityQueue<List<String>> messageQueue){
        //System.out.println("List added to queue "+ messages.toString());
        //timestamp.setTime(timestamp.getTime()+1000);

        // System.out.println("\n---------server before PRIORITY QUEUE----------");
        // printPriorityQueue(messageQueue);
        // System.out.println("\n---------PRIORITY QUEUE----------\n");

        while(allPeerExistInMessageQueue(messageQueue)){
            List<String> auxList = (List<String>)messageQueue.poll();
            String message = auxList.get(0);

            if(!message.equals("bleat")){
                String time = auxList.get(1);
                String peerHost = auxList.get(2);
                String peerPort = auxList.get(3);
                String timeOfArrival = auxList.get(4);
    
                System.out.println("\nTime of arrival: "+ timeOfArrival);
                System.out.println("Time from origin: "+time);
                System.out.println("Message From("+peerHost+":"+peerPort+"): "+message);
            }

        }

        // System.out.println("\n---------server after PRIORITY QUEUE----------");
        // printPriorityQueue(messageQueue);
        // System.out.println("\n---------PRIORITY QUEUE----------\n");
    }

    /**
     * Add Client Host to table
     * @param targetPeerHost
     */
    public int register(String targetPeerHost, int port, Logger logger){
        PeerHost newPeer = new PeerHost(targetPeerHost, port);

        PeerHost thisPeer = new PeerHost(this.host, this.port);


        System.out.println("Server: Peer to add: "+targetPeerHost+" port: "+port);
        System.out.println("Server: Host: "+this.host+" Port: "+this.port);
        
        System.out.println("Peer List size: "+peerList.size());

        if(!newPeer.inList(peerList) && newPeer.equals(thisPeer) && newPeer.hostPort != this.port){
            peerList.add(newPeer);
            //logger.info("Server: added new peer --> Host: "+targetPeerHost+" Port: "+String.valueOf(port));
            //logger.info("Server: Current Peer Ip Table: ");
            System.out.println("Server: Current Peer Ip Table: ");
            printPeerHostList(logger);
            return 1;
        }else if(!newPeer.inList(peerList) && !newPeer.equals(thisPeer) && newPeer.hostPort == this.port){
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

    public void printPriorityQueue(PriorityQueue messageQueue){
        PriorityQueue auxQueue = new PriorityQueue<List<String>>(new QueueComparator());
        
        if(!messageQueue.isEmpty()){
            auxQueue.addAll(messageQueue);

            while(!auxQueue.isEmpty()){
                List<String> auxList = (List<String>)auxQueue.poll();
                System.out.println(auxList.toString());
            }
        }
    }

}

class Client implements Runnable{
    String  host;
    Logger  logger;
    Scanner scanner;
    int port;
    List<PeerHost> peerList;
    PriorityQueue<List<String>> messageQueue;

    public Client(String host, int port, Logger logger, List<PeerHost> peerList, PriorityQueue<List<String>> messageQueue) throws Exception {
        this.host    = host;
        this.logger  = logger; 
        this.scanner = new Scanner(System.in);
        this.peerList = peerList;
        this.port = port;
        this.messageQueue = messageQueue;
    }

    @Override 
    public void run() {
        try {
            while (true) {
                try {
                    //System.out.print("$ ");
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
                        messageAll(messages, this.host, String.valueOf(this.port));

                    }

                    
                } catch(Exception e) {
                    e.printStackTrace();
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

        /**
         * Lamport Clock
         */

        timestamp.setTime(timestamp.getTime()+1000);

        //Build Add time and peer info to message list
        messages.add(timestamp.toString());
        messages.add(server);
        messages.add(port);
        //messages.add(timestamp.toString());
        //Add To priority Queue
        messageQueue.add(messages);

        for(PeerHost peer : peerList){ 
            try{

                /**
                 * New message object for each peer
                 */

                List<String> messagesToSend = new ArrayList<String>();
                messagesToSend.addAll(messages);

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

                //Send Data
                objectOutputStream.writeObject(messagesToSend);

                socket.close();

            }catch(IOException e){
                e.printStackTrace();
            }finally{

            }
            
        }

        //displayMessage(messageQueue);
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

    public void printPriorityQueue(PriorityQueue messageQueue){
        PriorityQueue auxQueue = new PriorityQueue<List<String>>(new QueueComparator());
        if(!messageQueue.isEmpty()){
            auxQueue.addAll(messageQueue);

            while(!auxQueue.isEmpty()){
                List<String> auxList = (List<String>)auxQueue.poll();
                System.out.println(auxList.toString());
            }
        }
    }

    public void printPeerHostList(Logger logger){
        peerList.forEach((host) -> {
            host.printPeerHost(logger);
        });
    }

}
