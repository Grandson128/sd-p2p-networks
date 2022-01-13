//package ds.examples.sockets.peer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Peer {
	String host;
	Logger logger;

	public Peer(String hostname) {
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
		Peer peer = new Peer(args[0]);
		System.out.printf("new peer @ host=%s\n", args[0]);
		//Cria o cliente e o servidor, passando ao servidor o cliente como argumento para poder ter acesso a variavel as variaveis
		Client cliente = new Client(args[0], peer.logger);
		Server s = new Server(args[0], Integer.parseInt(args[1]), peer.logger,cliente);
		new Thread(cliente).start();
		new Thread(s).start();
	}
}

class Server implements Runnable {
	String       host;
	int          port;
	ServerSocket server;
	Logger       logger;
	Client client;

	public Server(String host, int port, Logger logger, Client client) throws Exception {
		this.host   = host;
		this.port   = port;
		this.logger = logger;
		server = new ServerSocket(port, 1, InetAddress.getByName(host));
		this.client=client;
	}

	@Override
	public void run() {
		try {
			//logger.info("server: endpoint running at port " + port + " ...");
			while(true) {
				try {
					ArrayList<Thread> list = new ArrayList<>();
					//System.out.println(x.x + " ola");
					Socket client = server.accept();
					//logger.info("server: new connection from " + clientAddress);

					BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
					PrintWriter out = new PrintWriter(client.getOutputStream(), true);

					int token = Integer.parseInt(in.readLine());

					//System.out.println(token + " in server");
					out.print(1);
					out.flush();

					client.close();

					//Token permanece no peer ate escrever unlock
					while(this.client.lock){Thread.sleep(1);}

					//Para prevenir que exista mais que uma thread ligada ao servidor ao mesmo tempo.
					boolean tExists=true;
					while(tExists) {
						tExists=false;
						for (Thread i : list) {
							if (i.isAlive()) {
								tExists=true;
							}
							else{
								list.remove(i);
							}
						}
						Thread.sleep(1);
					}

					Thread t = new Thread(new Connection(token, logger,this.client));
					t.start();

					list.add(t);


				}catch(Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class Connection implements Runnable {
	Logger logger;
	Client client;
	int token;

	public Connection(int token, Logger logger, Client client) {
		this.token  = token;
		this.logger  = logger;
		this.client = client;
	}

	@Override
	public void run() {
		/*
		 * prepare socket I/O channels
		 */
		try {
			Socket socket = new Socket(InetAddress.getByName(client.server), Integer.parseInt(client.port));
			//logger.info("client: connected to server " + socket.getInetAddress() + "[port = " + socket.getPort() + "]");
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//Recebe o token e soma uma unidade, imprimindo e enviando para o socket do servidor no proximo peer.
			System.out.println(token+1);
			out.println(token+1);
			out.flush();
			/*Fica a espera de ler a resposta do servidor para poder fechar a socket, caso contrario, a thread morria  e o servidor criava outra thread,
			  mas no outro peer ainda teria uma conecção aberta, o que causava problemas de sincronização entre os dois peers pois a thread nova ia tentar 
			  criar uma conecção mas ainda existia uma. */
			in.readLine();
			socket.close();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}

class Client implements Runnable {
	String  host;
	Logger  logger;
	Scanner scanner;
	String server;
	String port;
	Boolean lock;

	public Client(String host, Logger logger) throws Exception {
		this.host    = host;
		this.logger  = logger;
		this.scanner = new Scanner(System.in);
		this.lock=false;
	}


	@Override
	public void run() {
		try {
			//logger.info("client: endpoint running ...\n");

			server  = scanner.next();
			port    = scanner.next();
			Socket socket = new Socket(InetAddress.getByName(server), Integer.parseInt(port));
			//logger.info("client: connected to server " + socket.getInetAddress() + "[port = " + socket.getPort() + "]");
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out.println(0);
			out.flush();
			socket.close();

		
			while(true) {

				String readLock = scanner.nextLine();
				if(readLock.equals("lock"))
				{
					lock=true;
				}
				if(readLock.equals("unlock"))
				{
					lock=false;
				}

			}
			

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
