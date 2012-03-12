package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Scanner;

import polimi.distsys.sp2p.util.Serializer;

public class Foo {

	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws GeneralSecurityException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, GeneralSecurityException {
		
		Scanner scanner = new Scanner(System.in);
		
	if(args.length != 1) {
		System.exit(-1);
	}
	
	if(Integer.valueOf(args[0]) == 1) {
		// caso supernodo
	    InputStream is = Foo.class.getResourceAsStream("supernode.info");
	    PrivateKey priv;
	    PublicKey pub;
	    int port;
	    if(is == null){
	    	KeyPair kp = SecurityHandler.getKeypair();
	    	priv = kp.getPrivate();
	    	pub = kp.getPublic();
	    	port = 9876;
	    	FileOutputStream fos = new FileOutputStream("supernode.info");
	    	StringBuilder sb = new StringBuilder();
	    	sb.append(
	    			Serializer.byteArrayToHexString(
	    					Serializer.serialize(pub)
					)
			);
	    	sb.append(":");
	    	sb.append(
	    			Serializer.byteArrayToHexString(
	    					Serializer.serialize(priv)
					)
			);
	    	sb.append(":");
	    	sb.append(
	    			port
			);
	    	fos.write(sb.toString().getBytes());
	    	fos.close();
	    }else{
	    	Scanner sc = new Scanner(is);
	    	String[] tmp = sc.nextLine().split(":");
	    	pub = Serializer.deserialize(
	    			Serializer.hexStringToByteArray(tmp[0]), 
	    			PublicKey.class);
	    	priv = Serializer.deserialize(
	    			Serializer.hexStringToByteArray(tmp[1]), 
	    			PrivateKey.class);
	    	port = Integer.parseInt(tmp[2]);
	    }
	    	
		try {
			SuperNode s = new SuperNode(port, pub, priv);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		
	}
	
	if(Integer.valueOf(args[0]) == 2) {
		
		// caso nodo semplice
		System.out.println("inserisci il tuo nome utente:");
		String id = scanner.nextLine();
		System.out.println("inserisci la tua password:");
		String psw = scanner.nextLine();
		SimpleNode s;
		try {
			s = new SimpleNode(id, psw);
			s.join();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		

		
	}
		
	/** Codice di test per la prova del  messaggio crittografato
		
	String prova = "questa e' una prova";
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
	
	*/
		
	}
	
	
	

}
