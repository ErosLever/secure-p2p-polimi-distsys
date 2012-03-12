package polimi.distsys.sp2p;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.SecretKey;

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
	private final Node thisNode;
	private final Set<NodeInfo> listOfNodes;
	private final Map<NodeInfo, SecretKey> sharedKeys;
	
	public RoutingHandler(Node n) {
		this(n, new TreeSet<NodeInfo>());	
	}
	
	public RoutingHandler(Node n,Set<NodeInfo> list) {
		listOfNodes = Collections.synchronizedSet( new TreeSet<NodeInfo>(list) );
		sharedKeys = new TreeMap<NodeInfo, SecretKey>();
		thisNode = n;
	}
	
	public RoutingHandler(Node n, InputStream superNodesList) {
		this(n);
		//TODO Parse/deserialize list of nodes from InputStream
		/*
		Scanner sc = new Scanner(superNodesList);
		while(sc.hasNextLine()){
			String[] tmp = sc.nextLine().split(":");
			addSuperNode(new InetSocketAddress( tmp[0], Integer.parseInt(tmp[1]) ));
		}*/
	}
	
	public void addNode(NodeInfo sa) {
		listOfNodes.add( sa );
	}
	
	public void removeFromList(NodeInfo sa) {
		listOfNodes.remove( sa );
		sharedKeys.remove( sa );
	}
	
	public SecretKey getSharedKey(NodeInfo ni) throws GeneralSecurityException{
		if(sharedKeys.containsKey(ni))
			return sharedKeys.get(ni);
		SecretKey sk = thisNode.secretKeyGen(ni.getPublicKey());
		sharedKeys.put(ni, sk);
		return sk;
	}
	
	public Set<NodeInfo> getNodesList() {
		// return a copy of the list
		return new TreeSet<NodeInfo>( listOfNodes );
	}
	
	public NodeInfo getNodeInfoBySocketAddress(InetSocketAddress isa){
		for(NodeInfo ni : listOfNodes)
			if(ni.getAddress().equals(isa))
				return ni;
		for(NodeInfo ni : sharedKeys.keySet())
			if(ni.getAddress().equals(isa))
				return ni;
		return null;
	}
}
