import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

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
	private final Set<SocketAddress> superNodes;
	
	public RoutingHandler() {
		this(new TreeSet<SocketAddress>());	
	}
	
	public RoutingHandler(Set<SocketAddress> list) {
		superNodes = Collections.synchronizedSet( new TreeSet<SocketAddress>(list) );
	}
	
	public RoutingHandler(InputStream superNodesList) {
		this();
		Scanner sc = new Scanner(superNodesList);
		while(sc.hasNextLine()){
			String[] tmp = sc.nextLine().split(":");
			addSuperNode(new InetSocketAddress( tmp[0], Integer.parseInt(tmp[1]) ));
		}
	}
	
	public void addSuperNode(SocketAddress sa) {
		superNodes.add( sa );
	}
	
	public void removeFromList(SocketAddress sa) {
		superNodes.remove( sa );
	}
	
	public Set<SocketAddress> getSuperNodesList() {
		// return a copy of the list
		return new TreeSet<SocketAddress>( superNodes );
	}
}
