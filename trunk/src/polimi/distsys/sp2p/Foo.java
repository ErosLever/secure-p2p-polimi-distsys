package polimi.distsys.sp2p;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Foo {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Scanner scanner = new Scanner(System.in);
		
	if(args.length != 1) {
		System.exit(-1);
	}
	
	if(Integer.valueOf(args[0]) == 1) {
		// caso supernodo
		try {
			SuperNode s = new SuperNode(9876);
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
