package node;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;
import cli.Command;
import cli.Shell;
import exceptions.ConnectionRollbackedException;

public class Node implements INodeCli, Runnable {

	private String componentName;
	private Config config;
	private ServerSocket server;
	private Socket client;
	private Shell shell;
	private int tcpPort;
	private String host;
	private Timer timer;
	private IsAliveSender isAliveSender =null;
	// first index real ressource value second uncommited 
	public static List<String> nodeResource =Collections.synchronizedList(new ArrayList<String>(2));
	public static final Log logger =LogFactory.getLog(Node.class);
	private final  ExecutorService executorService = Executors.newCachedThreadPool();


	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 * @throws ConnectionRollbackedException 
	 */
	public Node(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.tcpPort=config.getInt("tcp.port");
		this.host=config.getString("controller.host");
		nodeResource.add("0");

		try {
			this.server=new ServerSocket (tcpPort);

		} catch (IOException e) {
			logger.error("Servers can not be created !",e);
		}


		/*
		 * Next, register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		shell=new Shell(componentName,userRequestStream,userResponseStream);
		shell.register(this);
		timer=new Timer();
		int nodeAlive=config.getInt("node.alive");
		String operator=config.getString("node.operators");
		try {
			isAliveSender=new IsAliveSender(config.getInt("controller.udp.port"),tcpPort,host,operator,config);
		} catch (ConnectionRollbackedException e) {
			System.out.println(e.getMessage());
			return;
		}

		timer.scheduleAtFixedRate(isAliveSender, 0,nodeAlive);
		logger.info("DatagrammPackets will be send in every "+nodeAlive +" ms");


	}

	@Override
	public void run() {
		executorService.execute(shell);
		System.out.println(componentName
				+ " up and waiting for commands!");

		while(!server.isClosed()){
			try {
				this.client=server.accept();
				logger.info("Client "+client.getInetAddress() +" "+client.getLocalPort() +" accepted");
				executorService.execute(new NodeCommandsHandler(client,config,componentName));
			} catch (IOException e) {
				logger.error("Something went wrong while acepting client requests!");
				exit();
			} 
		}

	}


	@Override
	@Command
	public String exit() {
		logger.info("exit called");
		executorService.shutdownNow();
		this.timer.cancel();
		if(this.isAliveSender!=null){
			this.isAliveSender.exit();
		}
		try {
			this.server.close();
		} catch (IOException e) {
			logger.info("Server is already closed");
		}
		return "Node closed Good Bye ..!";
	}
	/**
	 * @param args
	 *            the first argument is the name of the {@link Node} component,
	 *            which also represents the name of the configuration
	 */
	public static void main(String[] args) {
		Node node = new Node(args[0], new Config(args[0]), System.in,
				System.out);
		node.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Command
	@Override
	public String resources() throws IOException {
		System.out.println(Arrays.toString(nodeResource.toArray()));
		return nodeResource.get(0);
	}


}
