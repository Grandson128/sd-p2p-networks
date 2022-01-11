//package ds.trabalho.parte2;


import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;


public class Pushpull {
    String host;
    Logger logger;
    String[] peersTable;

    public Pushpull(String hostname) {
        peersTable  = new String[255];
        host   = hostname;
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
        new Thread(new Server(args[0], Integer.parseInt(args[1]), pushpull.logger, pushpull.peersTable)).start();
        //new Thread(new Server(args[0], 2222, pushpull.logger)).start();
        new Thread(new Client(args[0], pushpull.logger, pushpull.peersTable)).start();
    }


}


class Server implements Runnable{
    String       host;
    int          port;
    ServerSocket server;
    Logger       logger;
    String[]     peersTable;

    public Server(String host, int port, Logger logger, String[] peersTable) throws Exception {
        this.host   = host;
        this.port   = port;
        this.logger = logger;
        this.peersTable = peersTable;
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
                    new Thread(new Connection(clientAddress, client, logger, this.peersTable, this.host)).start();
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
    String[] peersTable;
    String host;

    public Connection(String clientAddress, Socket clientSocket, Logger logger, String[] peersTable, String host) {
        this.clientAddress = clientAddress;
        this.clientSocket  = clientSocket;
        this.logger        = logger;
        this.peersTable    = peersTable;
        this.host          = host;
    }

    @Override
    public void run() {
        /*
        * prepare socket I/O channels
        */
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));    
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            String result="";
            String command;
            command = in.readLine();
            logger.info("server: message from host " + clientAddress + "[command = " + command + "]");
            /*
            * parse command
            */
            Scanner sc = new Scanner(command);
            String op = sc.next();
            String targetPeerHost = clientAddress;
            //String targetPeerPort = sc.next();	    
            /*
            * execute op
            */
            
            if(op.equals("register")){
                register(targetPeerHost);
                result = "register "+this.host;
            }else if(op.equals("push")){
                command = "got push";
            }else if(op.equals("pull")){
            }else if(op.equals("pushpull")){
            }else{
                System.out.println("Wrong command");
                clientSocket.close();
            }
            

            /*
            * send result
            */
            out.println(result);
            out.flush();
            /*
            * close connection
            */
            clientSocket.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }


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
            System.out.println("Added peer ->" + peersTable[counter] + "\n");
        // }
    }
}



class Client implements Runnable{
    String  host;
    Logger  logger;
    Scanner scanner;
    String[] peersTable;
    public Client(String host, Logger logger, String[] peersTable) throws Exception {
        this.host    = host;
        this.logger  = logger; 
        this.scanner = new Scanner(System.in);
        this.peersTable = peersTable;
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
                    /*
                    * prepare socket I/O channels
                    */
                    PrintWriter   out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));    
                    /*
                    * send command
                    */
                    out.println(command);
                    out.flush();	    
                    /*
                    * receive result
                    */
                    String result = in.readLine();
                    System.out.printf("\nRecebido --->" + result+ "\n");


                    if(command.equals("register")){
                        register(result, server);
                    }else if(command.equals("push")){
                    }else if(command.equals("pull")){
                    }else if(command.equals("pushpull")){
                    }else{
                        System.out.println("Wrong command");
                        socket.close();
                    }


                    /*
                    * close connection
                    */
                    socket.close();
                } catch(Exception e) {
                    e.printStackTrace();
                }   
            }
        } catch(Exception e) {
            e.printStackTrace();
        }   	    
    }


    public void register(String result, String server){
        Scanner sc = new Scanner(result);
        String resultCommand = sc.next();
        String resultServer = sc.next();

        if (resultCommand.equals("register") && resultServer.equals(server)){
            int counter = 0;
            while(peersTable[counter] != null){
                counter++;
            }
            peersTable[counter] = server;

            System.out.println("Added peer ->" + peersTable[counter] + "\n");
        } 


    }

}

