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

public class Pushpull {
    String host;
    Logger logger;
    String[] peersTable;
    static String port;
    List<String> peerWordsList;

    public Pushpull(String hostname) {
        peersTable  = new String[255];
        host   = hostname;
        peerWordsList = new ArrayList<String>();
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
        port = Integer.parseInt(args[1]);

        Pushpull pushpull = new Pushpull(args[0]);
        System.out.printf("new peer @ host=%s\n", args[0]);
        new Thread(new Server(args[0], port, pushpull.logger, pushpull.peersTable, pushpull.peerWordsList)).start();
        new Thread(new Client(args[0], pushpull.logger, pushpull.peersTable, pushpull.peerWordsList)).start();
    }


}


class Server implements Runnable{
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    String[]     peersTable;
    List<String> peerWordsList;
    final int secondsRandomWord = 15;

    public Server(String host, int port, Logger logger, String[] peersTable, List<String> peerWordsList) throws Exception {
        this.host   = host;
        this.port   = port;
        this.logger = logger;
        this.peersTable = peersTable;
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
                    new Thread(new Connection(clientAddress, client, logger, this.peersTable, this.host, this.peerWordsList)).start();
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
    String[] peersTable;
    String host;
    List<String> peerWordsList;

    public Connection(String clientAddress, Socket clientSocket, Logger logger, String[] peersTable, String host, List<String> peerWordsList) {
        this.clientAddress = clientAddress;
        this.clientSocket  = clientSocket;
        this.logger        = logger;
        this.peersTable    = peersTable;
        this.host          = host;
        this.peerWordsList = peerWordsList;
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
                register(clientAddress);
                //List with register comman in head
                resultMessages.add("register");
                //List with server host name in body
                resultMessages.add(this.host);

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
    public void register(String targetPeerHost){
        /**
         * Uncoment for live demonstration
         */
        int counter = 0;
        while(peersTable[counter] != null){
            // if(peersTable[counter].equals(targetPeerHost)){
            //     counter = -1;
            //     break;
            // }
            counter++;
        }
        // if(counter < 0){
        //     System.out.println("No peer Added, host and server are the same\n");
        // }else{
            peersTable[counter] = targetPeerHost;
            logger.info("server: Added new peer --> " + peersTable[counter]);
        // }
    }
}

class Client implements Runnable{
    String  host;
    Logger  logger;
    Scanner scanner;
    String[] peersTable;
    List<String> peerWordsList;

    public Client(String host, Logger logger, String[] peersTable, List<String> peerWordsList) throws Exception {
        this.host    = host;
        this.logger  = logger; 
        this.scanner = new Scanner(System.in);
        this.peersTable = peersTable;
        this.peerWordsList = peerWordsList;
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


                    List<String> messages = new ArrayList<String>();
                    messages.add(command);
                    
                    if(command.equals("push") || command.equals("pushpull")){
                        messages.addAll(peerWordsList);
                    }

                    //Send Data
                    objectOutputStream.writeObject(messages);

                    //Result from server
                    List<String> resultMessages = new ArrayList<String>();
                    resultMessages = (List<String>)objectInputStream.readObject();
                    
                    if(command.equals("register")){
                        register(resultMessages, server);

                    }else if(command.equals("push")){
                        logger.info("client: Push done\n");	

                    }else if(command.equals("pull")){
                        pull(resultMessages);

                    }else if(command.equals("pushpull")){
                        pull(resultMessages);

                    }else{
                        System.out.println("Wrong command");
                        socket.close();
                    }


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
    public void register(List<String> result, String server){
        String resultCommand = result.get(0);
        String resultServer = result.get(1);

        if (resultCommand.equals("register") && resultServer.equals(server)){
            int counter = 0;
            while(peersTable[counter] != null){
                counter++;
            }
            peersTable[counter] = server;
            logger.info("Added peer ->" + peersTable[counter]);
        } 
    }

}
