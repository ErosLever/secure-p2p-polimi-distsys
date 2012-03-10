import java.io.File;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * 
 */

/**
 * @author Ale
 * 
 * classe per la gestione degli indirizzi: da fare
 *
 */
public class RoutingHandler {

	//devo fare una lista di IP/Port di tutti i super nodi + i metodi per recuperarli
	ArrayList<SocketAddress> listaIndirizzi; 
	
	public RoutingHandler() {
		listaIndirizzi = new ArrayList<SocketAddress>();	
		
	}
	
	public RoutingHandler(String path) {
		listaIndirizzi = new ArrayList<SocketAddress>();
		
		
	}
	
	public void addToList() {
		
		
	}
	
	public void removeFromList() {
		
	}
	
	public ArrayList<SocketAddress> getList() {
		
		return listaIndirizzi;
	}
}
