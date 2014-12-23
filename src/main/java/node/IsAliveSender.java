package node;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IsAliveSender extends TimerTask {
	private DatagramSocket datagramSocket;
	private DatagramPacket datagramPacket;
	private InetAddress inetAddress;
	private int udpPort;
	private byte[] toSend;
	public static final Log logger =LogFactory.getLog(Node.class);	


	public IsAliveSender(int udpPort,int tcpPort, String host,String operators) {
		this.udpPort = udpPort;
		this.toSend = ("!alive "+tcpPort+" "+operators).getBytes();


		try {
			this.inetAddress = InetAddress.getByName(host);
		} catch (UnknownHostException e) {
			logger.error("The Host ist not correct");
		}

		try {
			datagramSocket = new DatagramSocket();
		} catch (SocketException e) {
			logger.error("COULD NOT CREATE DATAGRAMSOCKET",e);
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
