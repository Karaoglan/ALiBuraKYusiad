package secureChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import util.Config;
import util.SecurityUtils;

public class CloudControllerSecureChannel {
	private BufferedReader reader;
	private PrintWriter writer;
	private Config config;
	private byte [] controllerChallenge;
	private byte [] encodedAESKey;
	private byte[] encodedIVParam;
	private String username;
	public CloudControllerSecureChannel(BufferedReader reader, PrintWriter writer,Config config) {
		this.reader = reader;
		this.writer = writer;
		this.config=config;
	}

	public void sendMessage(String message){
		String pubKeyPath=config.getString("keys.dir")+"/"+username+".pub.pem";
		message=SecurityUtils.encryptRsa(message.getBytes(), pubKeyPath);
		writer.println(message);
	}

	public String getMessage() throws IOException{
		String priKeyPath=config.getString("key");
		byte[] received=null;
		received = reader.readLine().getBytes();

		byte [] decoded=SecurityUtils.decodeBase64(received);
		String result=SecurityUtils.decryptRsa(decoded, priKeyPath);
		return result;
	}

	public boolean getAuthenticate(String message) throws IOException{
		String [] split=message.split(" ");
		String username=split[0];
		setUsername(username);
		//byte[] decoded=SecurityUtils.decodeBase64(split[1].getBytes());
		//String clientChallenge=SecurityUtils.decryptRsa(decoded,config.getString("key"));
		byte [] clientChallenge=SecurityUtils.decodeBase64(split[1].getBytes());

		//2.message
		encodedIVParam = SecurityUtils.encodeBase64(SecurityUtils.generateRandomNumber(16));
		controllerChallenge = SecurityUtils.generateRandomNumber(32);
		encodedAESKey = SecurityUtils.encodeBase64(SecurityUtils.generateAesKey(256).getEncoded());
		String pubKeyPath=config.getString("keys.dir")+"/"+username+".pub.pem";
		controllerChallenge=SecurityUtils.encodeBase64(controllerChallenge);
		String secondMessage= SecurityUtils.encryptRsa("!ok".getBytes(), pubKeyPath) +" "+
				SecurityUtils.encryptRsa(clientChallenge, pubKeyPath) +" "+
				SecurityUtils.encryptRsa(controllerChallenge, pubKeyPath) +" "+
				SecurityUtils.encryptRsa(encodedAESKey, pubKeyPath) +" "+
				SecurityUtils.encryptRsa(encodedIVParam, pubKeyPath);
		writer.println(secondMessage);
		//
		return getAes();
	}	

	public boolean getAes() throws IOException{
		String message=reader.readLine();
		String last=SecurityUtils.encryptAES(controllerChallenge, encodedAESKey, encodedIVParam);
		if(message.equals(last)){
			return true; 
		}
		return false;
	}

	public void setUsername(String username){
		this.username=username;
	}
}
