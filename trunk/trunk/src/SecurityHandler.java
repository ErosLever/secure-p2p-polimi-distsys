import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
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
	
	
	//dato il canale aperto fa Diffie Helmann per ottenere il segreto
	public static byte[] getSharedSecret(Socket s) {
		
		byte[] sharedSecret = null;
	
		try {
		
			// apro gli stream per la comunicazione
			DataOutputStream out = new DataOutputStream(s.getOutputStream());
			DataInputStream in = new DataInputStream(s.getInputStream());
			
			//genero le chiavi e le encodo
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
			KeyPair keyPair = kpg.genKeyPair();
			byte[] keyBytes = keyPair.getPublic().getEncoded();


			// sending public key
			out.writeInt(keyBytes.length);
			out.write(keyBytes);

			//questo pezzo e da vedere vedere comunicazione c/s 
			
			// Receive a public key.
			keyBytes = new byte[in.readInt()];
			in.readFully(keyBytes);

			//decodo la chiave pubblica ricevuta
			KeyFactory kf = KeyFactory.getInstance("DH");
			X509EncodedKeySpec x509Spec = new X509EncodedKeySpec(keyBytes);
			PublicKey theirPublicKey = kf.generatePublic(x509Spec);


			// genero il segreto
			KeyAgreement ka = KeyAgreement.getInstance("DH");
			ka.init(keyPair.getPrivate());
			ka.doPhase(theirPublicKey, true);

			sharedSecret = ka.generateSecret("AES").getEncoded();

			
			out.close();
			in.close();
			

		} catch (UnknownHostException e) {

		} catch (IOException e) {

		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return sharedSecret;

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
}
