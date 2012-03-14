package polimi.distsys.sp2p;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
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
	 * @throws GeneralSecurityException 
	 */
	public static SecretKey secretKeyGen(PrivateKey myPrivKey, PublicKey hisPubKey) throws GeneralSecurityException {

		KeyAgreement ka = KeyAgreement.getInstance("DH");
		ka.init(myPrivKey);
		ka.doPhase(hisPubKey, true);
		return ka.generateSecret("AES");
		
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
