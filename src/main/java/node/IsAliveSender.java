package node;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;
import exceptions.ConnectionRollbackedException;

public class IsAliveSender extends TimerTask {
	private DatagramSocket datagramSocket;
	private DatagramPacket datagramPacket;
	private DatagramPacket receivedMessage;
	private InetAddress inetAddress;
	private int udpPort;
	private byte[] toSend;
	private byte[] firstMessage;
	private Config config;
	Map<Socket,HashMap<PrintWriter,BufferedReader>> sockets;
	public static final Log logger =LogFactory.getLog(Node.class);	


	public IsAliveSender(int udpPort,int tcpPort, String host,String operators,Config config) throws ConnectionRollbackedException {
		this.udpPort = udpPort;
		firstMessage="!hello".getBytes();
		this.toSend = ("!alive "+tcpPort+" "+operators).getBytes();
		this.config=config;
		
		try {
			this.inetAddress = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			logger.error("The Host ist not correct");
		}

		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.send(new DatagramPacket(firstMessage,firstMessage.length,inetAddress,this.udpPort));
			firstMessage=new byte[1024];
			receivedMessage=new DatagramPacket(firstMessage, firstMessage.length);
			datagramSocket.receive(receivedMessage);
			communicateOtherNodes();
		} catch (SocketException e) {
			logger.error("COULD NOT CREATE DATAGRAMSOCKET...!");
		} catch (IOException e) {
			logger.error("Something wrong while sending packet..!");
		}


	}

	/**
	 * ismini sonra degistir
	 * @throws ConnectionRollbackedException 
	 */
	public void communicateOtherNodes() throws ConnectionRollbackedException{
		String [] recvStr=new String(receivedMessage.getData(), 0,receivedMessage.getLength()).trim().split(" ");
		List<String> transactions=new ArrayList<String>();
		sockets=new HashMap<Socket,HashMap<PrintWriter,BufferedReader>>();
		Integer rmax =Integer.parseInt(recvStr[recvStr.length-1].trim());
		Integer resLevel=rmax/(recvStr.length-1);
		if(recvStr.length>2){
			if(resLevel<config.getInt("node.rmin")){
				throw new ConnectionRollbackedException("Nodes rmin higher than Resource Level.Please increase the resources");
			}
			for(int i =1 ;i<recvStr.length-1;i++){
				String host =recvStr[i].substring(0, recvStr[i].indexOf(":")).trim();
				int port=Integer.parseInt(recvStr[i].substring(host.length()+2).trim());
				try {
					Socket sock=new Socket(host,port);
					BufferedReader reader=new BufferedReader(new InputStreamReader (sock.getInputStream()));
					PrintWriter writer=new PrintWriter(sock.getOutputStream(),true);
					HashMap<PrintWriter,BufferedReader> h=new HashMap<PrintWriter,BufferedReader>();
					h.put(writer, reader);
					sockets.put(sock,h);
					writer.println("!share "+resLevel);
					String transactionErg=reader.readLine();
					transactions.add(transactionErg);
					if(transactionErg.equals("!nok")) break;

				} catch (UnknownHostException e) {
					logger.error("Host is incorrect!");
				} catch (IOException e) {
					logger.error("Something went wrong ..!");
				}

			}

			if(transactions.contains("!nok")){
				for(Socket sock:sockets.keySet()){
					sockets.get(sock).keySet().iterator().next().println("!rollback "+resLevel);
				}
				closeResNodeConnections();
				throw new ConnectionRollbackedException("Connection rollbacked..Please increase the resources");

			}else{
				for(Socket sock:sockets.keySet()){
					sockets.get(sock).keySet().iterator().next().println("!commit "+resLevel);
					
				}
				Node.nodeResource.add(1,resLevel+"");
				Node.nodeResource.remove(0);
				closeResNodeConnections();
			}
		}else {
			Node.nodeResource.add(1,resLevel+"");
			Node.nodeResource.remove(0);
		}


	}

	public void closeResNodeConnections(){
		for(Socket sock :sockets.keySet()){
			try {
				sock.close();
				sockets.get(sock).values().iterator().next().close();
				sockets.get(sock).keySet().iterator().next().close();
			} catch (IOException e) {
				logger.info("asdasda");
			}

		}
	}

	public void exit(){
		datagramSocket.close();
		this.cancel();
	}

	public void run(){
		this.datagramPacket = new DatagramPacket(toSend, toSend.length, inetAddress, udpPort);

		try {
			datagramSocket.send(datagramPacket);
		} catch (IOException e) {
			logger.error("COULD NOT SENT DATAGRAMM PACKET",e);
		}
	}

}
