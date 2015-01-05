package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import secureChannel.ClientSecureChannel;
import util.Config;
import cli.Command;
import cli.Shell;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private InputStream userRequestStream;
	private PrintWriter writer=null;
	private BufferedReader reader=null;
	private Socket clientSocket =null;
	private Shell shell;
	public static final Log logger =LogFactory.getLog(Client.class);
	private ExecutorService executor =Executors.newCachedThreadPool();
	private ClientSecureChannel clSecure;
	private boolean loggedIn;

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
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.userRequestStream = userRequestStream;

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
		loggedIn=false;

		try {
			clientSocket =new Socket(config.getString("controller.host"),config.getInt("controller.tcp.port"));
			this.reader=new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			this.writer=new PrintWriter(clientSocket.getOutputStream(),true);
			clSecure=new ClientSecureChannel(reader, writer, config);
		} catch (UnknownHostException e) {
			logger.error("Can not connect to Host !");
		} catch (IOException e) {
			logger.error("Client can not connect to Server!");
			try {
				exit();
			} catch (IOException e1) {
				logger.error("Can not close!");
			};  
		}
	}

	@Override
	public void run() {
		executor.execute(shell);
		System.out.println(componentName
				+ " up and waiting for commands!");

	}


	@Override
	public String login(String username, String password) throws IOException {
		logger.info("login "+username +" "+password+" called");
		writer.println("!login "+username+" "+password);
		return reader.readLine();
	}

	@Override
	@Command
	public String logout() throws IOException {
		logger.info("logout called");
		if(!loggedIn){
			return "You have to login first";
		}
		clSecure.sendMessage("!logout");
		loggedIn=false;
		return clSecure.getMessage();
	}

	@Override
	@Command
	public String credits() throws IOException {
		logger.info("credits called");
		if(!loggedIn){
			return "You have to login first";
		}
		clSecure.sendMessage("!credits");

		return clSecure.getMessage();
	}

	@Override
	@Command
	public String buy(long credits) throws IOException {
		logger.info("buy "+credits+" called");
		if(!loggedIn){
			return "You have to login first";
		}
		clSecure.sendMessage("!buy "+credits);

		return clSecure.getMessage();
	}

	@Override
	@Command
	public String list() throws IOException {
		logger.info("list called");
		if(!loggedIn){
			return "You have to login first";
		}
		clSecure.sendMessage("!list");

		return clSecure.getMessage();
	}

	@Override
	@Command
	public String compute(String term) throws IOException {
		logger.info("compute "+ term + " called");
		if(!loggedIn){
			return "You have to login first";
		}
		String usage ="Usage : !compute <Number> [+,-,/,*] <Number>";
		if(checkTermToCompute(term)){
			clSecure.sendMessage("!compute "+term);
			return clSecure.getMessage();
		}else return usage;
	}

	@Override
	@Command
	public String exit() throws IOException {
		logger.info("exit called");
		executor.shutdownNow();
		if(clientSocket != null){
			this.clientSocket.close();
			this.userRequestStream.close();
		}
		return "Good Bye...";


	}
	public boolean checkTermToCompute(String term){
		logger.info("Checking if the "+ term +" is computable");
		String [] splittedArray =term.split(" ");
		if(splittedArray.length<3 || splittedArray.length%2!=1){
			return false;
		}
		for(int i =0 ;i<splittedArray.length;i++){
			if(i % 2==1){
				if("+-/*".indexOf(splittedArray [i])<0) return false;		
			}else{
				try{
					Integer.parseInt(splittedArray[i]);
				}catch(NumberFormatException e){
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {
		try{
			Client	client = new Client(args[0], new Config("client"), System.in,
					System.out);
			client.run();
		}catch(RejectedExecutionException ex){
			return;
		}

	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---
	@Command
	@Override
	public String authenticate(String username) throws IOException {
		
		if(loggedIn){
			return LoginResponseEnum.USER_ALREADY_ONLINE.toString();
		}
		
		String result=clSecure.sendAuthentication(username);
		if(LoginResponseEnum.AUTHENTICATION_SUCCESSFULL.equals(LoginResponseEnum.valueOf(result))){
			loggedIn=true;
		}
		return result;
	}

}
