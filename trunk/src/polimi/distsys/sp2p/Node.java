package polimi.distsys.sp2p;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Random;

import javax.crypto.SecretKey;

import polimi.distsys.sp2p.Message.Action;

/**
 * 
 */

/**
 * @author Ale
 *
 */
public abstract class Node {
	
	//range di porte per possiam metterle come parametro di init ( un po piu elegante )
	public static final int minPort = 40000;
	public static final int maxPort = 50000;
	public static final String supernodes = "superNodes.list";
	
	protected final RoutingHandler rh;
	
	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	
	//inizializzo il nodo segnando l'ip locale e la prima porta per la connessione
	protected final int myPort;
	protected final InetAddress myIp;

	public Node() throws IOException {
		
		// Avoid code duplication :D
		this( getRandomPort() );
		
	}


// inizializzo il nodo su una porta specifica
	public Node( final int port) throws IOException {
		
		final KeyPair kp = SecurityHandler.getKeypair();
		privateKey = kp.getPrivate();
		publicKey = kp.getPublic();
		
		myPort = port;
		
		try { 
			myIp = InetAddress.getLocalHost();
		} catch(UnknownHostException e) {
			throw new IOException("You are not connected to a network");
		}
		
		InputStream defaultt = this.getClass().getClassLoader().getResourceAsStream(supernodes);
		if(defaultt != null)
			rh = new RoutingHandler(this,defaultt);
		else
			rh = new RoutingHandler(this);
		
	}
	
	protected SecretKey secretKeyGen(PublicKey someoneElsePublicKey) throws GeneralSecurityException { 
		return SecurityHandler.secretKeyGen(privateKey, someoneElsePublicKey);
		
	}
	
	protected PublicKey getPublicKey() {
		return publicKey;
		
	}
	
	protected static int getRandomPort(){
		return minPort + (new Random()).nextInt(maxPort - minPort);
	}
	
	public InetSocketAddress getSocketAddress(){
		return new InetSocketAddress(myIp, myPort);
	}
	
	protected Message createMessage(Action action, byte[] payload, NodeInfo dest) throws GeneralSecurityException{
		SecretKey sk = rh.getSharedKey(dest);
		return new Message(action, payload, sk);
	}
	
	public NodeInfo getNodeInfo(){
		if(this instanceof SimpleNode)
			return new NodeInfo((SimpleNode)this);
		else
			return new NodeInfo((SuperNode)this);
	}
	
}