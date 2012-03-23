package polimi.distsys.sp2p;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory;
import polimi.distsys.sp2p.handlers.RoutingHandler;

/**
 * 
 */

/**
 * @author Ale
 *
 */
public abstract class Node {

	
	protected final PrivateKey privateKey;
	protected final PublicKey publicKey;
	protected final ServerSocket socket;
	protected final EncryptedSocketFactory enSockFact;
	protected final RoutingHandler rh;


	protected Node( PublicKey pub, PrivateKey priv, ServerSocket sock ) 
			throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
		this.privateKey = priv;
		this.publicKey = pub;
		this.socket = sock;
		this.enSockFact = new EncryptedSocketFactory( priv, pub );
		//inizializza la lista dei supernodi
		rh = new RoutingHandler();
		
	}
	
	protected PublicKey getPublicKey(){
		return publicKey;
	}
	
	protected PrivateKey getPrivateKey(){
		return privateKey;
	}
	
	public NodeInfo getNodeInfo(){
		return new NodeInfo( this );
	}
	


}