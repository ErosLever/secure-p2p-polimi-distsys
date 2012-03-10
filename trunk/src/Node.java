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
	private final int minPort = 40000;
	private final int maxPort = 50000;
	
	private RoutingHandler rh = new RoutingHandler();
	
	private byte[] privateKey;
	private byte[] publicKey;
	
	//inizializzo il nodo segnando l'ip locale e la prima porta per la connessione
	private int initialPort;
	private InetAddress myIp;

	public Node() {
		
		keyGeneration();
		
		int randomizedPort = minPort + (new Random()).nextInt(maxPort - minPort);
		initialPort = randomizedPort;
		
		// recupero il mio ip locale ( nn so se funziona e cmq c'e il problema del nat )
		try { 
			
			myIp = InetAddress.getLocalHost();
		}
		
		catch(UnknownHostException e) {
			//TODO: gestione computer nn connessi alla rete
			System.out.println("You are not connected to a network");
		    System.exit(-1);
		}
	}


// inizializzo il nodo su una porta specifica
	public Node(int port) {
		
		keyGeneration();
		initialPort = port;
		
		try { 
			myIp = InetAddress.getLocalHost();
		}
		catch(UnknownHostException e) {
			System.out.println("You are not connected to a network");
		    System.exit(-1);
		}
	}
	
	// metodo per ottenere le chiavi usato dai costruttori
	private void keyGeneration() {
		
		KeyPair kp = SecurityHandler.getKeypair();
		
		privateKey = kp.getPrivate().getEncoded();
		publicKey = kp.getPublic().getEncoded();
		
	}

	protected int getInitialPort() {
		return initialPort;
	}

	protected byte[] getPrivateKey() {
		return privateKey;
		
	}
	
	protected byte[] getPublicKey() {
		return publicKey;
		
	}
}