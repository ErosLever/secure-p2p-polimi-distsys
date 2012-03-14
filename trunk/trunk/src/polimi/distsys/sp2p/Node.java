package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.SecretKey;

import polimi.distsys.sp2p.Message.Action;
import polimi.distsys.sp2p.util.PortChecker;

/**
 * 
 */

/**
 * @author Ale
 *
 */
public abstract class Node {
	

	public static final String supernodes = "superNodes.list";
	
	protected final RoutingHandler rh;
	
	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	
	//inizializzo il nodo segnando l'ip locale e la prima porta per la connessione
	protected final int myPort;
	protected final InetAddress myIp;
	protected final ServerSocket socket;

	public Node() throws IOException {
		
		// Avoid code duplication :D
		this( PortChecker.getBoundedServerSocketChannel().socket() );
		
	}
	
	public Node(ServerSocket sock) throws IOException{
		this( sock, SecurityHandler.getKeypair() );
	}


	protected Node(final int port) throws IOException {
		
		this(PortChecker.getBoundedServerSocketChannelOrNull(port).socket());
		
	}
	
	private Node( final ServerSocket sock, KeyPair kp) throws IOException{
		this( sock, kp.getPublic(), kp.getPrivate());
	}
	
	protected Node( final ServerSocket sock, PublicKey pubKey, PrivateKey privKey) throws IOException{
		
		privateKey = privKey;
		publicKey = pubKey;
		
		socket = sock;
		myIp = sock.getInetAddress();
		myPort = sock.getLocalPort();
		
		if(new File(supernodes).exists())
			rh = new RoutingHandler(this,new FileInputStream(supernodes));
		else
			rh = new RoutingHandler(this);
		
	}
	
	protected SecretKey secretKeyGen(PublicKey someoneElsePublicKey) throws GeneralSecurityException { 
		return SecurityHandler.secretKeyGen(privateKey, someoneElsePublicKey);
		
	}
	
	protected PublicKey getPublicKey() {
		return publicKey;
		
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