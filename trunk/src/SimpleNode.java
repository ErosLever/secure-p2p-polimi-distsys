import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.rmi.MarshalledObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author Ale
 * 
 * classe usata per gestire i nodi client del sistema p2p
 *
 */
public class SimpleNode extends Node {
	
	private final String userID;
	private final byte[] password;
	//private final byte[] secretKey;
	
	/** States whether the node is connected to a SuperNode */
	private boolean connected;
	private String downloadDirectory; // directory dove vengon considerati i file
	

	public SimpleNode(final String userID, final String password) throws IOException, NoSuchAlgorithmException {
		this( getRandomPort(), userID, password );
	}
	
	public SimpleNode(final int port, final String userID, final String password) throws IOException, NoSuchAlgorithmException {
		super( port );
		
		this.userID = userID;
		this.password = SecurityHandler.hashFunction( password );

		connected = false;
		
	}
	
	// abbozzo autenticazione, c e anche da gestire il multithreading
	public void join() {
		
		if(!connected) {
			
			Socket connection;
			Message login = new Message(Request.LOGIN, userID);

			try {
				
				//TODO togliere l indirizzo hardcoded e gestire la scelta dell indirizzo con il routing handler
				InetSocketAddress sa = (InetSocketAddress) rh.getSuperNodesList().iterator().next();
				
				connection = new Socket(sa.getHostName(), sa.getPort());
				
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
					//System.out.println(getPrivateKey());
					System.out.println(getPublicKey());
					System.out.println(superNodePk);
					
					byte[] secretKey = secretKeyGen( superNodePk );
					System.out.println(secretKey.length);
					
					//TODO: codice per far inserire la password all utente
					//password = "prova";
					
					//costruisco ed invio il nuovo messaggio
					
					login.setRequest(Request.AUTH);
					
					//metto nel paylod l hash della password
					login.setPayLoad(password);
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
