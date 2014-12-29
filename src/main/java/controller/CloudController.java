package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.ComputationRequestInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;
import admin.INotificationCallback;
import cli.Command;
import cli.Shell;

public class CloudController extends UnicastRemoteObject implements ICloudControllerCli, IAdminConsole,Runnable {

	private String componentName;

	private InputStream userRequestStream;
	private ServerSocket server; //client -Cloud kommunizieren
	private Socket client;//client -Cloud client
	public static final Log logger =LogFactory.getLog(CloudController.class);
	private Shell shell;
	public IsAliveListener alive;
	private String bindingName;
	private int rmiPort;
	private String rmiHost;
	private Registry registry;
	protected static Map<String,String> loginStatus =Collections.synchronizedMap(new TreeMap<String,String>());
	protected static Map<String,Long> userCredits=Collections.synchronizedMap(new TreeMap<String,Long>());
	protected static Map<Integer,String> aliveStatus=Collections.synchronizedMap(new TreeMap<Integer,String>());
	protected static Map<Integer,String> supOperators=Collections.synchronizedMap(new TreeMap<Integer,String>());
	protected static Map<Integer,Integer> nodeUsage=Collections.synchronizedMap(new TreeMap<Integer,Integer>());
	protected static Map<Integer,Long> nodeTime=Collections.synchronizedMap(new TreeMap<Integer,Long>());
	protected static Map<Integer,InetAddress> nodeInfos=Collections.synchronizedMap(new TreeMap<Integer,InetAddress>());
	protected static Map<Character,Long> statistic=Collections.synchronizedMap(new TreeMap<Character,Long>());
	protected static Map<INotificationCallback,TreeMap<String,Integer>> subscription=Collections.synchronizedMap(new HashMap<INotificationCallback,TreeMap<String,Integer>>());
	private static  ExecutorService executorService = Executors.newCachedThreadPool();


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
			InputStream userRequestStream, PrintStream userResponseStream) throws RemoteException {
		this.componentName = componentName;
		this.userRequestStream = userRequestStream;
		this.bindingName=config.getString("binding.name");
		this.rmiHost=config.getString("controller.host");
		this.rmiPort=config.getInt("controller.rmi.port");
		try {
			this.registry=LocateRegistry.createRegistry(rmiPort);
		} catch (RemoteException e1) {
			logger.error("Can not create regitry");
		}
		try {
			registry.bind(bindingName, this);
		} catch (AlreadyBoundException e1) {
			logger.info("Already bounded");
		}

		try {
			server=new ServerSocket(config.getInt("tcp.port"));
			setDefaultValues();
			this.alive=new IsAliveListener(config.getInt("udp.port"));
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
		alive.stop();
		alive.close();
		userRequestStream.close();
		server.close();
		UnicastRemoteObject.unexportObject(this,true);
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
			statistic.put('+',(long) 0);
			statistic.put('-',(long) 0);
			statistic.put('/',(long) 0);
			statistic.put('*',(long) 0);
		} catch (UnknownHostException e) {
			logger.error("Wrong host name!");
		}


	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 */
	public static void main(String[] args) throws RemoteException {
		CloudController cloudController = new CloudController(args[0],
				new Config("controller"), System.in, System.out);
		cloudController.run();
	}
	@Command
	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		if(!subscription.isEmpty() && subscription.containsKey(callback)){
			TreeMap<String, Integer> temp = subscription.get(callback);
			if(temp.containsKey(username)){
				temp.replace(username, temp.get(username),Math.max(temp.get(username),credits));
			}else {
				temp.put(username, credits);
				subscription.put(callback,temp);
			}
		}else{
			TreeMap<String, Integer> temp = new TreeMap<String, Integer>();
			temp.put(username,credits);
			subscription.put(callback,temp);
		}
		return true;
	}

	@Command
	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {

		List<ComputationRequestInfo> list=new ArrayList<ComputationRequestInfo>();
		synchronized(aliveStatus){
			for(Entry<Integer, String> map :aliveStatus.entrySet()){
				if(map.getValue().equals("online")){
					try {
						Socket sock=new Socket(nodeInfos.get(map.getKey()),map.getKey());
						ObjectOutputStream oos=new ObjectOutputStream(sock.getOutputStream());
						oos.flush();
						System.out.println("osssss");
						System.out.println("burda");
						//oos.writeUTF("!getLogs");
						//oos.writeObject("!getLogs");
						oos.writeObject("!getLogs");
						oos.flush();
						oos.close();

						ObjectInputStream ois=new ObjectInputStream(sock.getInputStream());

						list.addAll((List<ComputationRequestInfo>) ois.readObject());
						System.out.println("burda1");
						//oos.close();
						ois.close();
						sock.close();
					} catch (IOException e) {
						logger.error("Can not create TCP SOCKET");
					} catch (ClassNotFoundException e) {
						logger.error("ClassNotFound ...");
					}

				}
			}
		}
		return list;
	}

	@Command
	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
		for(Character c :statistic.keySet()){
			System.out.println(statistic.get(c));
		}
		LinkedHashMap<Character,Long> map= new LinkedHashMap<Character,Long>(statistic);
		return  map;
	}

	@Override
	public void closeAdmin(INotificationCallback cl) throws RemoteException{
		subscription.remove(cl);

	}


}
