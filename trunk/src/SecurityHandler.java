
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;


public class SecurityHandler {

	/**
	 * metodo per la generazione delle chiavi asimmetriche
	 * 
	 * @return KeyPair che contiene una chiave pubblica e una privata
	 */
	public static KeyPair getKeypair() {

		KeyPairGenerator kpg;
		KeyPair keyPair = null;
		
		try {
			
			kpg = KeyPairGenerator.getInstance("DH");
			keyPair = kpg.genKeyPair();

		} catch (NoSuchAlgorithmException e) {
			System.out.println("Key generation problem!, algorithm not found");
		}

		return keyPair;


	}
	
	
	/**
	 * metodo che uso esegue fisicamente il Diffie Hellman e
	 * costruisce il segreto condiviso tra due entita
	 * 
	 * @param myPrivKey
	 * @param hisPubKey
	 * @return la chiave condivisa in forma di array di bytes
	 */
	public static byte[] secretKeyGen(byte[] myPrivKey, byte[]hisPubKey) {

		
		byte[] secretEncoded = null;
		KeyFactory kf;
		KeyAgreement ka;

		try {
			
			kf = KeyFactory.getInstance("DH");
			
			// trasformo il flusso di byte nella chiave pubblica
			X509EncodedKeySpec x509SpecPub = new X509EncodedKeySpec(hisPubKey);
			PublicKey pubK = kf.generatePublic(x509SpecPub);
			
			// trasformo il flusso di byte nella chiave privata
			
			//TODO SI BLOCCA QUI L ESECUZIONE
			// INAPPROPRIATE KEY SPECIFICATION...
			
			X509EncodedKeySpec x509SpecPrivat = new X509EncodedKeySpec(myPrivKey);
			PrivateKey priK = kf.generatePrivate(x509SpecPrivat);
			

			// genero la chiave condivisa
			ka = KeyAgreement.getInstance("DH");
		
			ka.init(priK);
			ka.doPhase(pubK, true);
			secretEncoded = ka.generateSecret("AES").getEncoded();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return secretEncoded;

	}
	
	
	
	public static byte[] encryptMessage(byte[] key, byte[] message) {

		byte[] output = null;

		try {
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			Cipher cipher;

			cipher = Cipher.getInstance("AES");

			cipher.init(Cipher.ENCRYPT_MODE, keySpec);
			output = cipher.doFinal(message);


		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return output;
	}
	
	public static byte[] decryptMessage(byte[] key, byte[] message) {

		byte[] output = null;

		try {
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			Cipher cipher;

			cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.DECRYPT_MODE, keySpec);
			output = cipher.doFinal(message);

			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return output;
	}

	/**
	 * 
	 * @param input
	 * @return
	 */
	public static byte[] hashFunction(String input) {
		
		byte[] output = null;
		
		MessageDigest cript;
		try {
			cript = MessageDigest.getInstance("SHA-1");
			output = cript.digest(input.getBytes("utf-8"));
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
		
	}
}
