package polimi.distsys.sp2p;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import polimi.distsys.sp2p.Message.Request;
import polimi.distsys.sp2p.Message.Response;

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
	public void join() throws GeneralSecurityException, IOException, ClassNotFoundException {
		join(true);
	}
	
	public void join(boolean checkAlreadyConnected) throws GeneralSecurityException, IOException, ClassNotFoundException {
		
		if(!checkAlreadyConnected || !connected) {
			
			Socket connection;
			NodeInfo dest = rh.getNodesList().iterator().next();
			Message login = createMessage(Request.LOGIN, null, dest);

			InetSocketAddress sa = dest.getAddress();
			connection = new Socket(sa.getHostName(), sa.getPort());
			
			ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());

			oos.writeObject(login);
			oos.flush();
			
			login = (Message) ois.readObject();
			if(login.isResponse()){
				Response response = (Response) login.getAction();
				if(response == Response.OK) {
					login = createMessage(Request.AUTH, password, dest);
					oos.writeObject(login);
					
					//TODO continue here
				}
				//TODO: to be continued...
			} 
			
			oos.close();
			ois.close();
			connection.close();
		}
	}
	

}
