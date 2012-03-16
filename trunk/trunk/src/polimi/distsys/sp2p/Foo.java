package polimi.distsys.sp2p;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
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

		// controlla la correttezza dei parametri
		if( !(Integer.valueOf(args[0]) == 1 || Integer.valueOf(args[0]) == 2)) {
			System.out.print("I parametri utilizzati sono scorretti\n" +
					"utilizzare 2 per inizializzare un nodo semplice," +
					"1 per un supernodo ");
			System.exit(-1);
		}

		//init
		PrivateKey priv = null;
		PublicKey pub = null;
		int port = 0;
		String fileName = null;

		if(Integer.valueOf(args[0]) == 1) 
			fileName = "supernode.info"; 

		if(Integer.valueOf(args[0]) == 2)
			fileName = "simplenode.info"; 

		// se il file non esiste o non puo essere letto viene restituito null
		InputStream is = Foo.class.getResourceAsStream(fileName);

		if(is == null){

			Boolean goodValue = false;

			do {

				System.out.println("Inserisci il numero di porta da usare:");
				String s = scanner.nextLine();
				try {

					port = Integer.valueOf(scanner.nextLine());

					if ( port > 1023 || port < 65535 ) 
						goodValue = true; 
					else 
						System.out.print("Il valore inserito non  corretto\n" +
								"il numero di porta deve essere compreso tra 1024 e 65535\n\n");

				} catch (NumberFormatException e) {

					System.out.print("Il valore inserito non  corretto\n" +
							"il numero di porta deve essere compreso tra 1024 e 65535\n\n");

				}

			}while(goodValue);

			initializeNodeFile(fileName, port);


		}else{

			//legge il file e recupero i dati
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

		if(Integer.valueOf(args[0]) == 1) {
			try {

				new SuperNode(port, pub, priv);

			} catch (IOException e) {

				e.printStackTrace();
			}	}

		if(Integer.valueOf(args[0]) == 2) { 

			System.out.println("Inserisci il tuo nome utente:");
			String id = scanner.nextLine();
			System.out.println("Inserisci la tua password:");
			String psw = scanner.nextLine();
			System.out.println("Inserisci la directory in cui salvare i file");
			String dir = scanner.nextLine();
			//TODO: andranno aggiunti dei controlli sull inserimento di nome e password


			SimpleNode s = new SimpleNode(port, id, psw, dir);

			//start textual gui
			new VisualizationHandler(s);
		}


	}



	/**
	 * creo il file con le informazioni del nodo
	 * 
	 * struttura file: 
	 * < private key > : < public key > : < porta >   
	 * 
	 * @param fileName
	 * @param port
	 */
	private static void initializeNodeFile(String fileName, int port) {

		try {

			KeyPair kp = SecurityHandler.getKeypair();
			PrivateKey priv = kp.getPrivate();
			PublicKey pub = kp.getPublic();

			FileOutputStream fos = new FileOutputStream(fileName);
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
		}

		catch(Exception e) { }


	}



}
