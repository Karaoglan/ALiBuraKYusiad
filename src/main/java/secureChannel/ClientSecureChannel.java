package secureChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.MissingResourceException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import util.Config;
import util.SecurityUtils;
import client.Client;
import client.LoginResponseEnum;

public class ClientSecureChannel {
	private BufferedReader reader;
	private PrintWriter writer;
	private Config config;
	private byte[] encodedAESKey;
	private byte[] encodedIVParam;
	private String username;
	public static final Log logger =LogFactory.getLog(Client.class);

	public ClientSecureChannel(BufferedReader reader, PrintWriter writer,Config config) {
		this.reader = reader;
		this.writer = writer;
		this.config=config;
	}

	public void sendMessage(String message){
		message=SecurityUtils.encryptRsa(message.getBytes(), config.getString("controller.key"));
		writer.println(message);
	}

	public String getMessage(){
		String priKeyPath=config.getString("keys.dir")+"/"+username+".pem";
		byte[] received=null;
		try {
			received = reader.readLine().getBytes();
		} catch (IOException e) {
			logger.error("Can not read message");
		}
		byte [] decoded=SecurityUtils.decodeBase64(received);
		String result=SecurityUtils.decryptRsa(decoded, priKeyPath);
		return result;
	}

	public boolean userExists(String username){
		Config user=new Config("user");

		try{
			user.getInt(username+".credits");
		}catch(MissingResourceException ex){
			return false;
		}
		return true;
	}

	public String sendAuthentication(String username) throws IOException{
		if(!userExists(username)){
			return "USER_DOESNT_EXISTS";
		}
		this.username=username;
		byte [] clientChannel=SecurityUtils.generateRandomNumber(32);

		String toSend="!authenticate "+username+ " " + new String(SecurityUtils.encodeBase64(clientChannel));
		sendMessage(toSend);

		//SecondMessage
		String priKeyPath=config.getString("keys.dir")+"/"+username+".pem";
		String receivedMessage=reader.readLine();
		String [] splitted=receivedMessage.split(" ");

		try{
			if(LoginResponseEnum.USER_ALREADY_ONLINE.equals(LoginResponseEnum.valueOf(SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(receivedMessage.getBytes()), priKeyPath)))){
				return LoginResponseEnum.USER_ALREADY_ONLINE.toString();
			}
		}catch(Exception ex){
			logger.info("Continue to DECRYPT MESSAGE");

		}	
		String okMessage=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[0].getBytes()),priKeyPath);
		String clientChallenge=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[1].getBytes()),priKeyPath);
		if(!okMessage.equals("!ok") || !clientChallenge.equals(new String(clientChannel))){
			return "AUTHENTICATION_FAILED";
		}
		String controllerChallenge=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[2].getBytes()),priKeyPath);
		encodedAESKey=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[3].getBytes()),priKeyPath).getBytes();
		encodedIVParam=SecurityUtils.decryptRsa(SecurityUtils.decodeBase64(splitted[4].getBytes()),priKeyPath).getBytes();
		//3.message

		return sendAES(controllerChallenge);
	}
	public String sendAES(String toSend) throws IOException{
		String erg=SecurityUtils.encryptAES(toSend.getBytes(), encodedAESKey, encodedIVParam);
		writer.println(erg);
		return getMessage();

	}
}
