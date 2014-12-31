package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import secureChannel.CloudControllerSecureChannel;
import util.Config;
import admin.INotificationCallback;
import cli.Command;
import cli.Shell;

public class CommandsHandler implements Runnable {
	private Socket client;
	private Socket Nodeclient;
	private BufferedReader reader;
	private PrintWriter writer;
	private BufferedReader Nodereader;
	private PrintWriter Nodewriter;
	public static final Log logger =LogFactory.getLog(CommandsHandler.class);
	private String loggedInUser;
	private Shell shell;
	private Config config;

	public CommandsHandler(Socket client) throws IOException{
		this.client=client;
		this.loggedInUser=null;
		config=new Config("controller");
		reader=new BufferedReader(new InputStreamReader (client.getInputStream()));
		writer=new PrintWriter(client.getOutputStream(),true);
		shell = new Shell("Server", client.getInputStream(), client.getOutputStream());

		/*
		 * Next, register all commands the Shell should support. In this example
		 * this class implements all desired commands.
		 */
		shell.register(this);
	}

	@Override
	public void run() {

		String command="";
		try {
			while(!Thread.currentThread().isInterrupted() && !client.isClosed() && (command=reader.readLine())!=null){
				try {
					writer.println(shell.invoke(command));
				} 
				catch (Throwable e) {
					logger.error("Wrong input");
				}	
			}
		} catch (IOException e1) {
			try {
				exit();
			} catch (IOException e) {
				logger.error("Exit cant not be resolved");

			}
		}

		try {
			exit();
		} catch (IOException e) {
			logger.error("Exit cant not be resolved");

		}

	}

	private boolean checkUser(String username, String pw) {
		logger.info("Method :checkUser parameters :"+username +" "+pw +" called");
		String password; 
		try{
			password = new Config("user").getString(username+".password");
		}catch(Exception ex){
			return false;
		}
		if (password == null)
			return false;

		if (password.equals(pw))
			return true;

		return false;
	}

	@Command
	public synchronized String login(String username, String password) {
		logger.info("Method :login parameters :"+username +" "+password +" called");


		if(loggedInUser!=null || ((CloudController.loginStatus.get(username)!=null)&& (CloudController.loginStatus.get(username).equals("online")))){		 
			return "You are already logged in!";
		}


		if (checkUser(username, password)) {			
			this.loggedInUser = username;
			CloudController.loginStatus.put(username,"online");
			return "Successfully logged in!";
		}

		return "Wrong username/password combination!";
	}

	@Command
	public  synchronized String logout() {

		logger.info("Method : logout called");
		if(loggedInUser==null)
			return "You have to login first!";


		CloudController.loginStatus.put(loggedInUser,"offline");
		loggedInUser = null;
		return "Successfully logged out!";

	}

	@Command
	public synchronized String credits() throws IOException {
		logger.info("Method : credits called");

		if(loggedInUser==null)
			return "You have to login first!";

		return "You have "+CloudController.userCredits.get(loggedInUser)+" credits left.";
	}

	@Command
	public synchronized String buy(long credits) throws IOException {
		if(loggedInUser==null)
			return "You have to login first!";

		Long erg =CloudController.userCredits.get(loggedInUser)+credits;
		CloudController.userCredits.put(loggedInUser,erg);
		return "You now have "+CloudController.userCredits.get(loggedInUser)+" credits.";
	}


	@Command
	public synchronized String list() throws IOException {
		logger.info("Method : list called");
		if(loggedInUser==null)
			return "You have to login first!";

		String operators="";
		synchronized(CloudController.supOperators){
			for(Integer i:CloudController.supOperators.keySet()){
				if(CloudController.aliveStatus.get(i).equals("online")){
					operators+=CloudController.supOperators.get(i);
				}
			}		
		}
		int firstPlusOcc =operators.indexOf("+");
		int lastPlusOcc =operators.lastIndexOf("+");
		if(firstPlusOcc!=lastPlusOcc){
			operators=operators.substring(0,lastPlusOcc)+operators.substring(lastPlusOcc+1);
		}
		return operators.length()>0?operators:"No available operation";
	}


	@Command
	public synchronized String compute(String term) throws IOException {
		logger.info("Method : credits +"+ term +" called");
		if(loggedInUser==null)
			return "You have to login first!";
		int numberOfRequest=0;
		//check if term have available operations
		List<String> list=Arrays.asList(term.split(" "));

		for(int i = 1 ; i<list.size();i=i+2){
			numberOfRequest++;
			if(!list().contains(list.get(i))){
				return "Please check available Operators";
			}
		}
		//check if user have enough credits
		if(CloudController.userCredits.get(loggedInUser)<numberOfRequest*50){
			return "You dont have enough credits..!";
		}
		String operator="";
		String toCompute="";
		Integer ergebnis = null;
		ArrayList<Integer> ports=new ArrayList<Integer>();
		int port =0;
		Boolean checkTerm=false;
		Iterator<String> it = list.iterator();
		while(it.hasNext()){
			if(checkTerm){
				operator =it.next();
				Long lastValue=CloudController.statistic.get(operator.charAt(0));
				CloudController.statistic.put(operator.charAt(0), lastValue+1);
				String digit=it.next(); 
				toCompute=ergebnis+" "+operator+" "+digit;
			}else{
				String digit=it.next(); 
				operator =it.next();
				Long lastValue=CloudController.statistic.get(operator.charAt(0));
				CloudController.statistic.put(operator.charAt(0), lastValue+1);
				String digit2=it.next();
				toCompute=digit+" "+operator+" "+digit2;
				checkTerm=true;
			}
			// get available ports from given operator
			synchronized(CloudController.supOperators){
				for(Integer o :CloudController.supOperators.keySet()){
					if (CloudController.supOperators.get(o).indexOf(operator)>=0 &&
							CloudController.aliveStatus.get(o).equals("online")) {
						ports.add(o);
					}
				}
				// get lowest used Node from ports
				port =getLowestUsedNode(ports);
				//create socket to get result from Node 
				Nodeclient=new Socket("localhost",port);
				this.Nodereader=new BufferedReader(new InputStreamReader (Nodeclient.getInputStream()));
				this.Nodewriter=new PrintWriter(Nodeclient.getOutputStream(),true);
				Nodewriter.println("!compute "+toCompute);

				String erg=Nodereader.readLine();
				if (erg.equals("Infinity")){
					return " Error :Division by 0";
				}
				ergebnis=(int)Math.round(Double.parseDouble(erg));
				Long currentCredits =CloudController.userCredits.get(loggedInUser);
				logger.info("Subtracting 50 credits from " +loggedInUser);
				CloudController.userCredits.put(loggedInUser,currentCredits-50);
				//subscribe
				synchronized(CloudController.subscription){
					for(INotificationCallback cl :CloudController.subscription.keySet()){
						for(Entry<String,Integer> map :CloudController.subscription.get(cl).entrySet()){
							String username=map.getKey();
							Integer credits=map.getValue();
							if(username.equals(loggedInUser) && CloudController.userCredits.get(username)<credits){
								cl.notify(username, credits);
							}
						}
					}
				}
				setUsage(port, erg.length()*50);
				//close socket 
				Nodereader.close();
				Nodewriter.close();
				Nodeclient.close();

			}

		}		
		return ergebnis+"";


	}

	public synchronized boolean checkUserCredit(String user){
		logger.info("checkUserCredit "+user +" called");
		return CloudController.userCredits.get(user)<50?false:true;	
	}

	public synchronized Integer getLowestUsedNode (ArrayList<Integer> list){
		logger.info("getLowestUsedNode "+list+ " called");
		int usage=Integer.MAX_VALUE;
		int port=0;
		if(list.size()==1){
			return list.get(0);
		}
		for(Integer i :list){
			Integer k=CloudController.nodeUsage.get(i);

			if(usage> (k==null?0:k)){
				usage=(k==null?0:k);
				port=i;

			}
		}
		return port==0?list.get(0):port;

	}
	public synchronized void setUsage(int port,int usage){
		logger.info("setUsage "+port+" "+usage+" called");
		int last =0;
		if(CloudController.nodeUsage.containsKey(port)){
			last =CloudController.nodeUsage.get(port);
			CloudController.nodeUsage.put(port, last+usage);
		}else CloudController.nodeUsage.put(port,usage);
	}

	@Command
	public synchronized  void exit() throws IOException {
		logger.info("exit called");
		// First try to logout in case a user is still logged in
		if(loggedInUser!=null){
			logout();
		}
		shell.close();
		reader.close();
		writer.close();
		client.close();	

	}
	
	@Command
	public String authenticate(String message) throws IOException {
		CloudControllerSecureChannel sc=new CloudControllerSecureChannel(reader,writer, config);
		sc.getRSA(message);
		return null;
	}


}
