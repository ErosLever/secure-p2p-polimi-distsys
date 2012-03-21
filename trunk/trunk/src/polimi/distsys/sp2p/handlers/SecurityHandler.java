package polimi.distsys.sp2p.handlers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class SecurityHandler {

	private static final int BUFFER_SIZE = 4096;
	private static final int ASYMM_KEY_SIZE = 1024/8;
	private static final String ASYMM_ALGO = "RSA";
	
	
	//KEY GENERATION
	
	/**
	 * Genera la coppia di chiave pubbliche private dei nodi
	 * per essere utilizzate con RSA
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public static final KeyPair keygen() throws NoSuchAlgorithmException {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASYMM_ALGO);
		kpg.initialize(ASYMM_KEY_SIZE*8);
		KeyPair simplekp = kpg.genKeyPair();
		
		return simplekp;
	}

	//HASHING FUNCTIONS

	/**
	 * crea l'hash del contenuto di un inputstream
	 * 
	 * @param in buffer da cui creare l hash
	 * @param bufferSize dimensione del buffer da utilizzare per la creazione del hash
	 * @return
	 * @throws NoSuchAlgorithmException 
	 * @throws IOException 
	 */
	public static byte[] createHash(InputStream in) throws NoSuchAlgorithmException, IOException {

		MessageDigest digest;
		byte [] hash = null;

		digest = MessageDigest.getInstance("SHA-1");

		byte [] buffer = new byte[BUFFER_SIZE];
		int sizeRead = -1;
		while ((sizeRead = in.read(buffer)) != -1) {
			digest.update(buffer, 0, sizeRead);

		}

		in.close();

		hash = new byte[digest.getDigestLength()];
		hash = digest.digest();

		return hash;
	}
	
	/**
	 * 
	 * @param input
	 * @return
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static byte[] createHash(String input) throws IOException, NoSuchAlgorithmException {

		return createHash(new ByteArrayInputStream(input.getBytes("utf-8")));
	}

	/**
	 *  dato un file f crea l'hast di 128 bit utilizzando SHA-1
	 *  
	 * @param f file di cui si vuol creare l hash
	 * @return
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static byte[] createHash(File f) throws NoSuchAlgorithmException, IOException {

		return createHash(new FileInputStream(f));
	}
	
	public static byte[] createHash(byte[] b) throws NoSuchAlgorithmException, IOException {

		return createHash(new ByteArrayInputStream(b));
	}

}
