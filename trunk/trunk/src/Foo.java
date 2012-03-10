import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AlgorithmParameters;
import java.security.AlgorithmParametersSpi;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHGenParameterSpec;
import javax.crypto.spec.DHParameterSpec;
import javax.swing.ImageIcon;

import org.bouncycastle.asn1.x9.DHDomainParameters;


public class Foo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		
	// TEST VARI!!!!
		
	String prova = "questa è una prova";
	System.out.println(prova);

	
	try {
		
		
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
		KeyPair keyPair = kpg.genKeyPair();
	
		
		// genero la chiave usando la mia pubblica + privata giusto per testare la lunghezza
		KeyAgreement ka = KeyAgreement.getInstance("DH");
		ka.init(keyPair.getPrivate());
		ka.doPhase(keyPair.getPublic(), true);

		byte[] sharedSecret = ka.generateSecret("AES").getEncoded();
	
	//uso la chiave generata per criptare e decriptare un messaggio
	byte[] bytestream = prova.getBytes();
	
	bytestream = SecurityHandler.encryptMessage(sharedSecret, bytestream);
	
	System.out.println(new String(bytestream));
	
	System.out.println(new String(SecurityHandler.decryptMessage(sharedSecret, bytestream)));
	
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (InvalidKeyException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} 
	
	
		
	}
	
	
	

}
