package util;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;

import client.Client;

/**
 * Please note that this class is not needed for Lab 1, but can later be
 * used in Lab 2.
 * 
 * Provides security provider related utility methods.
 */
public final class SecurityUtils {
	public static final Log logger =LogFactory.getLog(Client.class);

	private SecurityUtils() {
	}

	/**
	 * Registers the {@link BouncyCastleProvider} as the primary security
	 * provider if necessary.
	 */
	public static synchronized void registerBouncyCastle() {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.insertProviderAt(new BouncyCastleProvider(), 0);
		}
	}
	
	public static synchronized byte[] encodeBase64(byte[] message){
		return Base64.encode(message);
	}
	public static synchronized byte[] decodeBase64(byte[] message){
		return Base64.decode(message);
	}
	public static synchronized byte[] generateRandomNumber(int length){
		SecureRandom secureRandom = new SecureRandom();
		final byte[] number = new byte[length];
		secureRandom.nextBytes(number);
		return number;
	}
	public static synchronized Key generateAesKey(int keySize){
		KeyGenerator generator = null;
		try {
			generator = KeyGenerator.getInstance("AES");
		} catch (NoSuchAlgorithmException e) {
		}
		// KEYSIZE is in bits
		generator.init(keySize);
		SecretKey key = generator.generateKey();
		return key;
	}
	
	public static synchronized String encryptRsa(byte[] message, String pubKeyPath){
		registerBouncyCastle();
		File file=new File(pubKeyPath);
		 Cipher cipher = null;
		 String encrypted = null;
		 
		try {
			Key pubKey =Keys.readPublicPEM(file);
			cipher=Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE,pubKey);
			byte[] last=cipher.doFinal(message);
			encrypted= new String(encodeBase64(last));
	      
		} catch (IOException e) {
			logger.error("Can not read Public Key from "+file.getName(),e);
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException");
		} catch (NoSuchPaddingException e) {
			logger.error("NoSuchPaddingException");
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException");
		} catch (IllegalBlockSizeException e) {
			logger.error("IllegalBlockSizeException");
		} catch (BadPaddingException e) {
			logger.error("BadPaddingException");
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return encrypted;
	}
	
	public static synchronized String decryptRsa(byte[] message, String priKeyPath){
		registerBouncyCastle();
		File file=new File(priKeyPath);
		 Cipher cipher = null;
		 String decrypted = null;
		try {
			Key key =Keys.readPrivatePEM(file);
			cipher=Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE,key);
			byte[] last=cipher.doFinal(message);
			decrypted= new String(last);
	      
		} catch (IOException e) {
			logger.error("Can not read Private Key from "+file.getName(),e);
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException");
		} catch (NoSuchPaddingException e) {
			logger.error("NoSuchPaddingException");
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException");
		} catch (IllegalBlockSizeException e) {
			logger.error("IllegalBlockSizeException");
		} catch (BadPaddingException e) {
			logger.error("BadPaddingException");
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return decrypted;
	}
	
	public static synchronized String encryptAES(byte[] message,byte[] secretKey,byte[] ivParam){
		 registerBouncyCastle();
		 Cipher cipher = null;
		 String encrypted = null;
		try {
			
			cipher=Cipher.getInstance("AES/CTR/NoPadding","BC");
			SecretKeySpec key=new SecretKeySpec(secretKey,"AES/CTR/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE,key,new IvParameterSpec(ivParam));
			byte[] last=cipher.doFinal(message);
			encrypted= new String(encodeBase64(last));
	      
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException");
		} catch (NoSuchPaddingException e) {
			logger.error("NoSuchPaddingException");
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException");
		} catch (IllegalBlockSizeException e) {
			logger.error("IllegalBlockSizeException");
		} catch (BadPaddingException e) {
			logger.error("BadPaddingException");
		} catch (InvalidAlgorithmParameterException e) {
			logger.error("InvalidAlgorithmParameterException",e);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return encrypted;
	}
	public static synchronized String decryptAES(byte[] message,byte[] secretKey,byte[] ivParam){
		 registerBouncyCastle();
		 Cipher cipher = null;
		 String decrypted = null;
		try {
			
			cipher=Cipher.getInstance("AES/CTR/NoPadding","BC");
			
			SecretKeySpec key=new SecretKeySpec(secretKey,"AES/CTR/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE,key,new IvParameterSpec(ivParam));
			byte[] last=cipher.doFinal(message);
			decrypted= new String(last);
	      
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException");
		} catch (NoSuchPaddingException e) {
			logger.error("NoSuchPaddingException");
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException");
		} catch (IllegalBlockSizeException e) {
			logger.error("IllegalBlockSizeException");
		} catch (BadPaddingException e) {
			logger.error("BadPaddingException");
		} catch (InvalidAlgorithmParameterException e) {
			logger.error("InvalidAlgorithmParameterException");
		} catch(Exception ex){
			ex.printStackTrace();
		}
		return decrypted;
	}
}
