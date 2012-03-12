package polimi.distsys.sp2p;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Collections;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.SecretKey;

import polimi.distsys.sp2p.util.Serializer;

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
		
		// Parse/deserialize list of nodes from InputStream
		// format: "host:port:pubkey" pubkey is escaped in hex
		
		Scanner sc = new Scanner(superNodesList);
		while(sc.hasNext()){
			try {
				String[] tmp = sc.nextLine().split(":");
				String host = tmp[0];
				int port = Integer.parseInt(tmp[1]);
				PublicKey pubKey = Serializer.deserialize(
						Serializer.hexStringToByteArray(tmp[2]), 
						PublicKey.class
				);
				addNode( 
						new NodeInfo(pubKey, new InetSocketAddress(host, port), true)
				);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
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
