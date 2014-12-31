package secureChannel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Key;

import util.Config;
import util.SecurityUtils;

public class CloudControllerSecureChannel {
	private BufferedReader reader;
	private PrintWriter writer;
	private Config config;
	private byte [] controllerChallenge;
	private Key AESKey;
	private byte[] ivParameter;
	public CloudControllerSecureChannel(BufferedReader reader, PrintWriter writer,Config config) {
		this.reader = reader;
		this.writer = writer;
		this.config=config;
	}
	
	public boolean getRSA(String message) throws IOException{
		String [] split=message.split(" ");
		String username=split[0];
		byte[] decoded=SecurityUtils.decodeBase64(split[1].getBytes());
		String clientChallenge=SecurityUtils.decryptRsa(decoded,config.getString("key"));
		
		//2.message
		ivParameter = SecurityUtils.generateRandomNumber(16);
		controllerChallenge = SecurityUtils.generateRandomNumber(32);
		AESKey = SecurityUtils.generateAesKey(256);
		String pubKeyPath=config.getString("keys.dir")+"/"+username+".pub.pem";
		String secondMessage="!ok "+
					SecurityUtils.encryptRsa(clientChallenge.getBytes(), pubKeyPath) +" "+
					SecurityUtils.encryptRsa(controllerChallenge, pubKeyPath) +" "+
					SecurityUtils.encryptRsa(AESKey.getEncoded(), pubKeyPath) +" "+
					SecurityUtils.encryptRsa(ivParameter, pubKeyPath);
		writer.println(secondMessage);
		//
		return getAes();
	}
	
	public boolean getAes() throws IOException{
		String message=reader.readLine();
		byte [] ccChallenge=SecurityUtils.decodeBase64(message.getBytes());
		String decrypted=SecurityUtils.decryptAES(ccChallenge, AESKey.getEncoded(), ivParameter);
		System.out.println("assss");
		String last=new String(controllerChallenge);
		System.out.println("-" + last +"- "+last.length());
		System.out.println("-" + decrypted +"- "+decrypted.length());
		System.out.println(decrypted.equals(last));
		if(decrypted.equals(last)){
			return true; 
		}
		return false;
	}

}
