import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.MarshalledObject;

/**
 * @author Ale
 * 
 * classe usata per gestire i nodi client del sistema p2p
 *
 */
public class SimpleNode extends Node {
	
	String userID;
	String password;
	byte[] secretKey;
	boolean connected; // lo uso per sapere se il nodo è attualmente connesso o meno ad un supernodo o meno
	String downloadDirectory; // directory dove vengon considerati i file
	

	public SimpleNode(String userID) {
		super();
		
		this.userID = userID;
		connected = false;
		
	}
	
	// abbozzo autenticazione, c e anche da gestire il multithreading
	public void join() {
		
		if(!connected) {
			
			Socket connection;
			Message login = new Message(Request.LOGIN, userID);

			try {
				
				//TODO togliere l indirizzo hardcoded e gestire la scelta dell indirizzo con il routing handler
				connection = new Socket("localhost", 54321);
				
				//creo gli stream
				ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());

				oos.writeObject(login);
				oos.flush();
				
				login = (Message) ois.readObject();
				Response response = login.getResponse();
				
				
				if(response == Response.OK) {
					
					// recupero la chiave del supernodo dal messaggio
					byte[] superNodePk = login.getPublicKey();
					
					//genero la chiave condivisa
					System.out.println(getPrivateKey());
					System.out.println(getPublicKey());
					System.out.println(superNodePk);
					
					secretKey = SecurityHandler.secretKeyGen(getPrivateKey(), superNodePk);
					System.out.println(secretKey.length);
					
					//TODO: codice per far inserire la password all utente
					password = "prova";
					
					//costruisco ed invio il nuovo messaggio
					
					login.setRequest(Request.AUTH);
					
					//metto nel paylod l hash della password
					login.setPayLoad(SecurityHandler.hashFunction(password));
					//cripto il payload
					login.setPayLoad(SecurityHandler.encryptMessage(secretKey, login.getPayLoad()));
					
					oos.writeObject(login);
					
					//TODO: to be continued...
				} 

			
				
				oos.close();
				ois.close();
				connection.close();

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	

}
