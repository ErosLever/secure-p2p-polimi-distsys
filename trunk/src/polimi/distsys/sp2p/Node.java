package polimi.distsys.sp2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import polimi.distsys.sp2p.handlers.RoutingHandler;

/**
 * 
 */

/**
 * @author Ale
 *
 */
public abstract class Node {

	
	protected final RoutingHandler rh;


	protected Node() throws IOException, NoSuchAlgorithmException, ClassNotFoundException {

		//inizializza la lista dei supernodi
		rh = new RoutingHandler();
		
	}
	
	public abstract PublicKey getPublicKey();
	public abstract ServerSocket getSocketAddress();
	


}