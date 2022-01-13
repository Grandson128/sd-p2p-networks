package ds.trabalho.parte2;


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
import java.util.Random;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;

class PeerHost{
    String hostName;
    int hostPort;

    public PeerHost(String hostName, int hostPort){
        this.hostName = hostName;
        this.hostPort = hostPort;
    }

    public void printPeerHost(Logger logger){
        logger.info("server: hostname: " +hostName + " port: " + hostPort);
    }
    
    public boolean equals(PeerHost peer){
        if(peer.hostName.equals(this.hostName) && peer.hostPort == this.hostPort){
            return true;
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
public class Pushpull {
    String host;
    Logger logger;
    static String port;
    List<String> peerWordsList;
    List<PeerHost> peerList;

    public Pushpull(String hostname) {
        host   = hostname;
        peerWordsList = new ArrayList<String>();
        peerList = new ArrayList<PeerHost>();
        logger = Logger.getLogger("logfile");

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
        Pushpull pushpull = new Pushpull(args[0]);
        System.out.printf("new peer @ host=%s\n", args[0]);
        new Thread(new Server(args[0], Integer.parseInt(args[1]), pushpull.logger, pushpull.peerList, pushpull.peerWordsList)).start();
        new Thread(new Client(args[0], Integer.parseInt(args[1]), pushpull.logger, pushpull.peerList, pushpull.peerWordsList)).start();
    }

}

class Server implements Runnable{
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    List<String> peerWordsList;
    List<PeerHost> peerList;

    final int secondsRandomWord = 15;

    public Server(String host, int port, Logger logger, List<PeerHost> peerList, List<String> peerWordsList) throws Exception {
        this.host   = host;
        this.port   = port;
        this.logger = logger;
        this.peerList = peerList;
        this.peerWordsList = peerWordsList;
        server = new ServerSocket(port, 1, InetAddress.getByName(host));
    }

    @Override
    public void run() {
        try {
            logger.info("server: endpoint running at port " + port + " ...");

            //Scheduled service to generate random words every x (secondsRandomWord) seconds
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(randomWords, 0, secondsRandomWord, TimeUnit.SECONDS);

            while(true) {
                try {
                    Socket client = server.accept();
                    String clientAddress = client.getInetAddress().getHostAddress();
                    logger.info("server: new connection from " + clientAddress);
                    new Thread(new Connection(clientAddress, client, logger, this.peerList, this.host, this.port, this.peerWordsList)).start();
                }catch(Exception e) {
                    e.printStackTrace();
                }    
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to be executed by the Scheduled service
     * to generate random words
     */
    Runnable randomWords = new Runnable() {
        public void run(){
            String word = getRandomWordFromFile();
            if(!peerWordsList.contains(word)){
                peerWordsList.add(word);
            }
            logger.info("server: New Random Word --> " + word);
            logger.info("server: Current List --> " + peerWordsList.toString());
        }    
    };
    
    /**
     * Opens a word dictionary and returns a random word
     * @return - random word from file
     */
    public String getRandomWordFromFile(){
        //29858 palavras
        Random rand = new Random();
        int randInt = rand.nextInt(29858);
        int count = 0;
        String chosenWord = "";
        try {
            File myObj = new File("todasPalavras.txt");
            Scanner myReader = new Scanner(myObj);
            while (count != randInt) {
                chosenWord = myReader.nextLine();
                count++;
            }
        myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return chosenWord;
    }

}

class Connection implements Runnable{

    String clientAddress;
    Socket clientSocket;
    Logger logger;
    String host;
    List<String> peerWordsList;
    List<PeerHost> peerList;
    int port;

    public Connection(String clientAddress, Socket clientSocket, Logger logger, List<PeerHost> peerList, String host, int port, List<String> peerWordsList) {
        this.clientAddress = clientAddress;
        this.clientSocket  = clientSocket;
        this.logger        = logger;
        this.peerList    = peerList;
        this.host          = host;
        this.peerWordsList = peerWordsList;
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

            /**
             * Handle Commands received by Client
             */

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

            }else if(op.equals("push")){
                push(listOfMessages);
                //List with "YES" at head to indicate successfull push
                resultMessages.add("YES");

            }else if(op.equals("pull")){
                //Add the current Peer word list to the result list to be deliver to the client
                resultMessages.addAll(peerWordsList);

            }else if(op.equals("pushpull")){
                push(listOfMessages);
                //Add the current Peer word list to the result list to be deliver to the client
                resultMessages.addAll(peerWordsList);

            }else{
                logger.info("server: Wrong Command --> " + op);
                clientSocket.close();
            }

            //Send Result List
            objectOutputStream.writeObject(resultMessages);
            clientSocket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Add words from client list to the peer list
     * @param result
     */
    public void push(List<String> result){
        //Remove command string
        result.remove(0);
        //Handle words
        result.forEach((word) -> {
            if(!peerWordsList.contains(word)){
                peerWordsList.add(word);
                logger.info("server: Added Word --> " + word);
            }
        });
        logger.info("server: Current Word List --> " + peerWordsList.toString());
    }

    /**
     * Add Client Host to table
     * @param targetPeerHost
     */
    public int register(String targetPeerHost, int port, Logger logger){
        PeerHost newPeer = new PeerHost(targetPeerHost, port);

        if(!newPeer.inList(peerList)){
            peerList.add(newPeer);
            logger.info("Server: added new peer --> Host: "+targetPeerHost+" Port: "+String.valueOf(port));
            logger.info("Server: Current Peer Ip Table: ");
            printPeerHostList(logger);
            return 1;
        }else{
            logger.info("Server: ERROR - Didn't register given peer, already exists");
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
    List<String> peerWordsList;
    List<PeerHost> peerList;
    int port;

    public Client(String host, int port, Logger logger, List<PeerHost> peerList, List<String> peerWordsList) throws Exception {
        this.host    = host;
        this.logger  = logger; 
        this.scanner = new Scanner(System.in);
        this.peerList = peerList;
        this.peerWordsList = peerWordsList;
        this.port = port;
    }

    public List<String> handleRegisterCommand(List<String> messages, String server, String port){

        try{
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

    public void handleCommands(List<String> messages, String server, String command){
        int flag = 0;
        for(PeerHost peer : peerList){ 
            if(peer.hostName.equals(server)){
                flag = 1;
                try{
                    /* 
                    * make connection
                    */
                    Socket socket  = new Socket(InetAddress.getByName(peer.hostName), peer.hostPort);
                    logger.info("client: connected to server " + socket.getInetAddress() + "[port = " + socket.getPort() + "]");
                    
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
                    

                    if(command.equals("push")){
                        if(resultMessages.get(0).equals("YES")){
                            System.out.println("Word list pushed with success");
                        }else{
                            System.out.println("Word list push FAILED");
                        }
                        
                    } else if(command.equals("pull")){
                        pull(resultMessages);

                    }else if(command.equals("pushpull")){
                        pull(resultMessages);
                    }


                    socket.close();

                }catch(IOException e){
                    e.printStackTrace();
                }   
            }else{
                //System.out.println("Error: Host not in table");
            }
        }

        if(flag == 0){
            System.out.println("Error: Host not in table");
        }

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
                    
                    List<String> messages = new ArrayList<String>();
                    messages.add(command);

                    //List to hold server reply
                    List<String> resultMessages = new ArrayList<String>();

    
                    /**
                     * Prepare List of messages
                     */
                    if(command.equals("register")){
                        String port = scanner.next(); //Ask new peer port
                        messages.add(this.host);
                        messages.add(String.valueOf(this.port));

                        resultMessages = handleRegisterCommand(messages, server, port);
                        register(resultMessages, server, port, logger);

                    }else if(command.equals("push")){
                        messages.addAll(peerWordsList);
                        handleCommands(messages, server, "push");

                    } else if(command.equals("pull")){
                        handleCommands(messages, server, "pull");

                    }else if(command.equals("pushpull")){
                        handleCommands(messages, server, "pushpull");

                    }else{
                        System.out.println("Wrong command");
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

    /**
     * Add words from server list to the peer list
     * @param result
     */
    public void pull(List<String> result){
        result.forEach((word) -> {
            if(!peerWordsList.contains(word)){
                peerWordsList.add(word);
                logger.info("client: Added Word --> " + word);	
            }
        });
        logger.info("client: Current Word List --> " + peerWordsList.toString());
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
                    logger.info("Client: added new peer --> Host: "+resultServerHost+" Port: "+resultServerPort);
                    logger.info("Client: Current Peer Ip Table: ");
                    printPeerHostList(logger);
                }else{
                    logger.info("Client: ERROR - Didn't register given peer, already exists");
                }
            }else{
                logger.info("Client: ERROR - Didn't register peer");
            }
        }        
    }

    public void printPeerHostList(Logger logger){
        peerList.forEach((host) -> {
            host.printPeerHost(logger);
        });
    }

}
