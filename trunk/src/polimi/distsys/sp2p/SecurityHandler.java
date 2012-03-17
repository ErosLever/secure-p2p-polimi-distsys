package polimi.distsys.sp2p;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;


public class SecurityHandler {
	
	//KEY GENERATIONS
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

	//HASHING FUNCTIONS
	/**
	 * 
	 * @param input
	 * @return
	 */
	public static byte[] hashFunction(String input) {
		ByteArrayInputStream bais = null;
		
		try {
		
		bais = new ByteArrayInputStream(input.getBytes("utf-8"));
		
		}  catch (IOException e) {
			e.printStackTrace();
		}

		BufferedInputStream fileBuffer = new BufferedInputStream(bais);
		
		return createHash(fileBuffer, 4096);
	}
	
	/**
	 *  dato un file f crea l'hast di 128 bit utilizzando SHA-1
	 *  
	 * @param f file di cui si vuol creare l hash
	 * @return
	 */
	public static byte[] createHash(File f) {
		
		FileInputStream is = null;
		try {
			
			is = new FileInputStream(f);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		BufferedInputStream fileBuffer = new BufferedInputStream(is);
		
		return createHash(fileBuffer, 4096);
	}
	
	/**
	 * crea l'hash del contenuto di un inputstream
	 * 
	 * @param in buffer da cui creare l hash
	 * @param bufferSize dimensione del buffer da utilizzare per la creazione del hash
	 * @return
	 */
	public static byte[] createHash(BufferedInputStream in, int bufferSize) {

		MessageDigest digest;
		byte [] hash = null;

		try {
			digest = MessageDigest.getInstance("SHA-1");

			byte [] buffer = new byte[bufferSize];
			int sizeRead = -1;
			while ((sizeRead = in.read(buffer)) != -1) {
				digest.update(buffer, 0, sizeRead);
			}
			
			in.close();

			hash = new byte[digest.getDigestLength()];
			hash = digest.digest();

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); 

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return hash;
	}

}
