package secureChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import util.Config;
import util.SecurityUtils;

public class ClientSecureChannel {
	private BufferedReader reader;
	private PrintWriter writer;
	private Config config;
	private byte[] AESKey;
	private byte[] ivParam;
	public ClientSecureChannel(BufferedReader reader, PrintWriter writer,Config config) {
		this.reader = reader;
		this.writer = writer;
		this.config=config;
	}

	public String sendRSA(String username) throws IOException{
		byte [] clientChannel=SecurityUtils.generateRandomNumber(32);
		String encRsa=SecurityUtils.encryptRsa(clientChannel,config.getString("controller.key"));
		writer.println("!authenticate "+username+ " " +encRsa);

		//SecondMessage
		String priKeyPath=config.getString("keys.dir")+"/"+username+".pem";
		String receivedMessage=reader.readLine();
		String [] splitted=receivedMessage.split(" ");

		String clientChallenge=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[1].getBytes()),priKeyPath);
		String cloudChallenge=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[2].getBytes()),priKeyPath);
		AESKey=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[3].getBytes()),priKeyPath).getBytes();
		ivParam=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[4].getBytes()),priKeyPath).getBytes();
		//3.message
		return sendAES(cloudChallenge);
	}
	public String sendAES(String toSend) throws IOException{
		writer.println(SecurityUtils.encryptAES(toSend.getBytes(), AESKey, ivParam));
		String erg= reader.readLine();
		System.out.println(erg);
		return erg;
	}




}
