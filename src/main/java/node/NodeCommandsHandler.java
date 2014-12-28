package node;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import model.ComputationRequestInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;
import cli.Command;
import cli.Shell;

public class NodeCommandsHandler implements Runnable {
	private Socket client;
	private BufferedReader reader;
	private PrintWriter writer;
	private Shell shell;
	private Config config;
	private String componentName;

	public static final Log logger =LogFactory.getLog(NodeCommandsHandler.class);

	public NodeCommandsHandler(Socket client,Config config,String componentName) {
		this.config=config;
		this.client=client;
		this.componentName=componentName;
		
		try {
			this.writer=new PrintWriter(client.getOutputStream(),true);
			this.reader=new BufferedReader(new InputStreamReader(client.getInputStream()));
			shell = new Shell("Server", client.getInputStream(), client.getOutputStream());
		} catch (IOException e) {
			logger.error("Failure while creating I/0 streams",e);
		}
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
					exit();

				}
			}	
		} catch (IOException e) {
			logger.info("closed!");
			exit();
		}
		exit();

	}


	@Command
	public synchronized String compute(String term) throws IOException {
		logger.info("compute "+ term +" called");
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		Object erg=null;

		try {
			erg= engine.eval(term);
		} catch (ScriptException e) {
			e.printStackTrace();
		}
		try{
			setLogFile(term +" = "+erg.toString().trim());
		}catch(IOException ex){
			logger.error("Log File cant not be created");
		}
		return erg.toString().trim();
	}

	@Command
	public String share(int res){
		int rmin=config.getInt("node.rmin");
		Node.nodeResource.add(1,res+"");
		if(res>=rmin){
			return "!ok";
		}

		return "!nok";
	}

	@Command
	public void commit(){
		Node.nodeResource.remove(0);
	}

	@Command
	public void rollback(){
		if(Node.nodeResource.size()>1){
			Node.nodeResource.remove(1);
		}

	}
	public synchronized void setLogFile(String erg) throws IOException{
		Date date = new Date() ;
		SimpleDateFormat dateFormat = formatter.get() ;
		File file = new File(dateFormat.format(date)+"_"+componentName + ".log") ;
		BufferedWriter out = new BufferedWriter(new FileWriter(config.getString("log.dir")+"/"+file));
		out.write(erg);
		out.close();

	}

	@Command
	public List<ComputationRequestInfo> getLogs(){
		logger.info("getLogs method");
		List<ComputationRequestInfo> logList=new ArrayList<ComputationRequestInfo>();
		String directory=config.getString("log.dir");
		File folder = new File(directory);
		File[] listOfFiles = folder.listFiles();

		for (File file : listOfFiles) {
			if (file.isFile()) {
				String fileName=file.getName();
				Date date=null;
				try {
					date=formatter.get().parse(fileName.substring(0, fileName.indexOf("_node")));
				} catch (ParseException e) {
					logger.error("Timestamp parse failure");
				}
				String node=componentName;
				FileInputStream fstream=null;
				try {
					fstream=new FileInputStream(directory+"/"+fileName);
					ObjectInputStream ois=new ObjectInputStream(fstream);

					while(true){  
						try{  
							//control
							String line=ois.readLine();
							String term=line.substring(0,line.indexOf("="));
							String erg=line.substring(term.length()+1);
							
							ComputationRequestInfo request=new ComputationRequestInfo(date, term, erg, node);
							logList.add(request);

							
						} catch (EOFException e){
							logger.info("end of line");
						}  
					}
				} catch (FileNotFoundException e) {
					logger.error("File not found..");
				} catch (IOException e) {
					logger.error("Can not read the log file");
				}



			}
		}

		return logList;
	}
	private static final ThreadLocal<SimpleDateFormat> formatter = new ThreadLocal<SimpleDateFormat>(){
		@Override
		protected SimpleDateFormat initialValue()
		{
			return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
		}
	};
	public void exit(){
		try {
			this.client.close();
			this.reader.close();
			this.writer.close();
		} catch (IOException e) {
			logger.info("Socket can not be closed ! Socket is already closed or null!");
		}
	}
}
