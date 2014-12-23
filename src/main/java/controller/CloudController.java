package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;
import cli.Command;
import cli.Shell;

public class CloudController implements ICloudControllerCli, Runnable {

	private String componentName;

	private InputStream userRequestStream;
	private ServerSocket server; //client -Cloud kommunizieren
	private Socket client;//client -Cloud client
	public static final Log logger =LogFactory.getLog(CloudController.class);
	private Shell shell;
	public IsAliveListener alive;
	protected static Map<String,String> loginStatus =Collections.synchronizedMap(new TreeMap<String,String>());
	protected static Map<String,Long> userCredits=Collections.synchronizedMap(new TreeMap<String,Long>());
	protected static Map<Integer,String> aliveStatus=Collections.synchronizedMap(new TreeMap<Integer,String>());
	protected static Map<Integer,String> supOperators=Collections.synchronizedMap(new TreeMap<Integer,String>());
	protected static Map<Integer,Integer> nodeUsage=Collections.synchronizedMap(new TreeMap<Integer,Integer>());
	protected static Map<Integer,Long> nodeTime=Collections.synchronizedMap(new TreeMap<Integer,Long>());
	protected static Map<Integer,InetAddress> nodeInfos=Collections.synchronizedMap(new TreeMap<Integer,InetAddress>());

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
	 */
	public CloudController(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.userRequestStream = userRequestStream;
		try {
			server=new ServerSocket(config.getInt("tcp.port"));
			setDefaultValues();
			this.alive=new IsAliveListener(config.getInt("udp.port"));
			//executorService.execute(alive);
			alive.start();
		} catch (IOException e) {
			logger.error("Server IO failure during create Server Socket !",e);
		}

		/*
		 * First, create a new Shell instance and provide the name of the
		 * component, an InputStream as well as an OutputStream. If you want to
		 * test the application manually, simply use System.in and System.out.
		 */
		shell = new Shell(componentName, userRequestStream, userResponseStream);
		/*
		 * Next, register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		shell.register(this);
	}

	@Override
	public void run() {
		executorService.execute(shell);
		System.out.println(componentName
				+ " is up and waiting for commands!");

		while (!server.isClosed()){
			try {
				client=server.accept();
				executorService.execute(new CommandsHandler(client));
			} catch (IOException e) {
				if (server != null && !server.isClosed()) {
					try {
						server.close();
					} catch (IOException e1)
					{
						logger.error("Server can not be closed!");
					}
					logger.error("Something wrong with client.accept",e);
				}
			} 

		}	
	}

	@Override
	@Command
	public String nodes() throws IOException { 
		logger.info("nodes called");
		String info="";
		int count=1;
		synchronized(aliveStatus){
			for(Entry<Integer, String> map :aliveStatus.entrySet()){
				info+= count+". "+"IP : "+nodeInfos.get(map.getKey()).getHostAddress() +" Port : "+map.getKey() +" status : " +map.getValue()+ " USAGE : "+ nodeUsage.get(map.getKey())+"\n";
				count++;
			}
		}
		return info;
	}

	@Override
	@Command
	public String users() throws IOException {
		logger.info("users called");
		String message="";
		int count=1;
		synchronized(loginStatus){
			for(Entry<String,String> map:loginStatus.entrySet()){
				message+=count+". "+map.getKey() +" " +map.getValue()+ " Credits : " +userCredits.get(map.getKey())+"\n";
				count++;
			}
		}
		
		if(message==""){
			message="No connected users..!";
		}
		return message;
	}

	@Override
	@Command
	public String exit() throws IOException {
		logger.info("exit called");
		executorService.shutdown();
	
		alive.close();
		userRequestStream.close();
		server.close();
		return "Server isnt going to accept any new Connection! Good Bye..!";


	}
	public void setDefaultValues(){
		Config conf = new Config("user");
		Config node1=new Config("node1");
		Config node2=new Config("node2");
		Config node3=new Config("node3");
		Config node4=new Config("node4");
		loginStatus.put("alice","offline");
		 loginStatus.put("bill","offline");
		 userCredits.put("alice",(long) conf.getInt("alice.credits"));
		 userCredits.put("bill",(long) conf.getInt("bill.credits"));
		int port1=node1.getInt("tcp.port");
		int port2=node2.getInt("tcp.port");
		int port3=node3.getInt("tcp.port");
		int port4=node4.getInt("tcp.port");
		try {
			 nodeInfos.put(port1, InetAddress.getByName(node1.getString("controller.host")));
			 nodeInfos.put(port2, InetAddress.getByName(node2.getString("controller.host")));
			 nodeInfos.put(port3, InetAddress.getByName(node3.getString("controller.host")));
			 nodeInfos.put(port4, InetAddress.getByName(node4.getString("controller.host")));
			 aliveStatus.put(port1, "offline");
			 aliveStatus.put(port2, "offline");
			 aliveStatus.put(port3, "offline");
			 aliveStatus.put(port4, "offline");
			 nodeUsage.put(port1,0);
			 nodeUsage.put(port2,0);
			 nodeUsage.put(port3,0);
			 nodeUsage.put(port4,0);
		} catch (UnknownHostException e) {
			logger.error("Wrong host name!");
		}
		
		
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 */
	public static void main(String[] args) {
		CloudController cloudController = new CloudController(args[0],
				new Config("controller"), System.in, System.out);
		cloudController.run();
	}

}
