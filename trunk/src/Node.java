import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.util.Random;

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
	
	private final byte[] privateKey;
	private final byte[] publicKey;
	
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
		privateKey = kp.getPrivate().getEncoded();
		publicKey = kp.getPublic().getEncoded();
		
		myPort = port;
		
		try { 
			myIp = InetAddress.getLocalHost();
		} catch(UnknownHostException e) {
			throw new IOException("You are not connected to a network");
		}
		
		InputStream defaultt = this.getClass().getClassLoader().getResourceAsStream(supernodes);
		if(defaultt != null)
			rh = new RoutingHandler(defaultt);
		else
			rh = new RoutingHandler();
		
	}
	
	protected byte[] secretKeyGen(byte[] someoneElsePublicKey) { 
		return SecurityHandler.secretKeyGen(privateKey, someoneElsePublicKey);
		
	}
	
	protected byte[] getPublicKey() {
		return publicKey;
		
	}
	
	protected static int getRandomPort(){
		return minPort + (new Random()).nextInt(maxPort - minPort);
	}
	
}