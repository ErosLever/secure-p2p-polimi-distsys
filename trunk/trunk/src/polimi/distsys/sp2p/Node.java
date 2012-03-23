package polimi.distsys.sp2p;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

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
			throws IOException, NoSuchAlgorithmException, ClassNotFoundException, InvalidKeySpecException {
		this.privateKey = priv;
		this.publicKey = pub;
		this.socket = sock;
		this.enSockFact = new EncryptedSocketFactory( priv, pub );
		//inizializza la lista dei supernodi
		rh = new RoutingHandler();
		
	}
	
	public PublicKey getPublicKey(){
		return publicKey;
	}
	
	protected PrivateKey getPrivateKey(){
		return privateKey;
	}
	
	public NodeInfo getNodeInfo(){
		return new NodeInfo( this );
	}

	public abstract InetSocketAddress getSocketAddress();
	
	public static PublicKey parsePublicKey( byte[] encoded ) throws InvalidKeySpecException, NoSuchAlgorithmException{
		return KeyFactory.getInstance( EncryptedSocketFactory.ASYMM_ALGO )
			.generatePublic( new X509EncodedKeySpec( encoded ) );
	}
	
	public static PrivateKey parsePrivateKey( byte[] encoded ) throws InvalidKeySpecException, NoSuchAlgorithmException{
		return KeyFactory.getInstance( EncryptedSocketFactory.ASYMM_ALGO )
			.generatePrivate( new PKCS8EncodedKeySpec( encoded ) );
	}

}