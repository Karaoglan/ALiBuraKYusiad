package admin;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.AccessException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.ComputationRequestInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;
import cli.Command;
import cli.Shell;
import controller.IAdminConsole;

/**
 * Please note that this class is not needed for Lab 1, but will later be
 * used in Lab 2. Hence, you do not have to implement it for the first
 * submission.
 */
public class AdminConsole extends UnicastRemoteObject implements IAdminConsole, Runnable,INotificationCallback {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;
	private int rmiport;
	private String bindingName;
	private String rmiHost;
	private Registry registry;
	private Shell shell;
	private IAdminConsole adminCon=null;
	private ExecutorService executorService=Executors.newCachedThreadPool();
	public static final Log logger =LogFactory.getLog(AdminConsole.class);

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
	public AdminConsole(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) throws RemoteException{
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.rmiport=config.getInt("controller.rmi.port");
		this.bindingName=config.getString("binding.name");
		this.rmiHost=config.getString("controller.host");
		shell=new Shell("admin ", userRequestStream, this.userResponseStream);
		shell.register(this);
		
		try {
			this.registry=LocateRegistry.getRegistry(rmiHost, rmiport);
		} catch (RemoteException e) {
			logger.error("Can not connect to RMI");
		}
		
		try {
			adminCon=(IAdminConsole) registry.lookup(bindingName);
		} catch (RemoteException | NotBoundException e) {
				logger.error("Lookup can not be resolved for "+ bindingName);
		}
		
	}

	@Override
	public void run() {
		executorService.execute(shell);
		System.out.println(componentName+" is up and waiting for commands");
	}
	
	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		return adminCon.subscribe(username, credits, this);
	}
	
	@Command
	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {
		// TODO Auto-generated method stub
		return  adminCon.getLogs();
				//adminCon.getLogs();;
	}
	
	@Command
	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {
	   return adminCon.statistics();
	
	}
	
		/**
	 * @param args
	 *            the first argument is the name of the {@link AdminConsole}
	 *            component
	 */
	public static void main(String[] args) throws RemoteException {
		AdminConsole adminConsole = new AdminConsole(args[0], new Config(
				"admin"), System.in, System.out);
		adminConsole.run();
	}
	
	@Command
	public String subscribe(String username, int credits) throws RemoteException {
		
		try{
		 subscribe(username, credits, this);
		}catch(RemoteException r){
			return "You could not subscribe";
		}
		return "Successfully subscribed for user "+username;
	}

	@Override
	public void notify(String username, int credits) throws RemoteException {
		System.out.println("Notification:" + username +" has less than "+ credits+" credits");
		
	}
	
	@Override
	public void closeAdmin(INotificationCallback cl) throws RemoteException{
		adminCon.closeAdmin(cl);
		
	}
	@Command
	public String exit(){
		try {
			closeAdmin(this);
		} catch (RemoteException e) {
			logger.error("Can not be closed..");
		}
		try {
			UnicastRemoteObject.unexportObject(this,true);
		} catch (NoSuchObjectException e1) {
			logger.error("NoSuchObject");
		}
		this.executorService.shutdownNow();
		try {
			this.userRequestStream.close();
		} catch (IOException e) {

		}
		return "Good Bye";
		
	}

}
