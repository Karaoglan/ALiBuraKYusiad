package controller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import node.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;

public class IsAliveListener extends Thread {
	private DatagramSocket socket;
	private DatagramPacket packet;
	private byte[] receivePacket;
	public static final Log logger =LogFactory.getLog(Node.class);
	public IsAliveListener(int udpPort) {
		this.receivePacket=new byte[256];
		try {
			this.socket=new DatagramSocket(udpPort);
			this.packet=new DatagramPacket(receivePacket, receivePacket.length);
		} catch (SocketException e) {
			logger.error("CAN not create datagramsocket",e);
		}
	}
	@Override
	public void run(){


		while(!socket.isClosed()){

			try {
				socket.receive(packet);
			} catch (IOException e) {
				logger.error("I/O failure...!",e);
			}

			String receivedString = new String(packet.getData(), 0, packet.getLength()).trim();
			Config conf=new Config("controller");
			if(receivedString.equals("!hello")){

				byte sendMessage[];
				String info="!init \n";
				synchronized(CloudController.aliveStatus){
					for(Entry<Integer, String> map :CloudController.aliveStatus.entrySet()){
						if(map.getValue().equals("online")){
							info+=CloudController.nodeInfos.get(map.getKey()).getHostName() +":"+map.getKey()+" \n";
						}
					}
				}
				info+=conf.getInt("controller.rmax");
				sendMessage=info.getBytes();
				DatagramPacket send=null;
				try {
					send = new DatagramPacket(sendMessage,sendMessage.length,packet.getSocketAddress());
				} catch (SocketException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					socket.send(send);
				} catch (IOException e) {
					logger.error("I/O failure...!",e);
				}

			}else if (receivedString.length()==0){
				close();
				
			}
			
			else{
				String[] splitStr = receivedString.split(" ");
				int port=Integer.parseInt(splitStr[1]);
				InetAddress adr=packet.getAddress();
				CloudController.nodeInfos.put(port, adr);
				CloudController.aliveStatus.put(port,"online");
				CloudController.supOperators.put(port,splitStr[2]);
				CloudController.nodeTime.put(port,System.currentTimeMillis());

				int checkPeriod = conf.getInt("node.checkPeriod"); 
				final int offlineNode =conf.getInt("node.timeout");
				Timer t=new Timer();

				t.scheduleAtFixedRate(new TimerTask(){

					@Override
					public void run() {
						controlCheckout(offlineNode);

					}}, checkPeriod, checkPeriod);


			}
		}
	}

	public void close(){
		this.socket.close();

	}

	public synchronized void controlCheckout(int offNode){
		synchronized(CloudController.nodeTime){
			for(Integer i :CloudController.nodeTime.keySet()){
				long diff=System.currentTimeMillis()-CloudController.nodeTime.get(i);

				if(CloudController.aliveStatus.get(i).equals("online")){	
					if(offNode<diff){
						CloudController.aliveStatus.put(i,"offline");
					}
				}
			}
		}
	}
}