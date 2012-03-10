import java.net.Socket;
import javax.crypto.*;

/**
 * @author Ale
 * 
 * classe usata per gestire i nodi client del sistema p2p
 *
 */
public class SimpleNode extends Node {
	
	boolean connected; // lo uso per sapere se il nodo è attualmente connesso o meno ad un supernodo o meno
	

	public SimpleNode() {
		super();
		connected = false;
		
		
	}
	
	
	public void join() {
		
		// private Socket clientSocket = new Socket( /** indirizzo ipdelserver, porta del server **// );
		
		
	
	}
	

}
