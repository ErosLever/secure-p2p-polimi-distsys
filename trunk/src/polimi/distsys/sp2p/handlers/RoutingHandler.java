package polimi.distsys.sp2p.handlers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import polimi.distsys.sp2p.Node;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.util.Serializer;

/**
 * 
 */

/**
 * @author Ale
 * 
 * Questa classe contiene le informazioni relative agli altri nodi della rete
 * i supernodi hanno un ip/porta fissato che viene letto dal file superNodes.list, mentre i nodi semplici
 * possono connettersi e disconnettersi dinamicamente al network.
 * Ogni nodo conosce la posizione degli altri attualmente connessi a se
 * ( sia caso nodo->supernodo che nodo->nodo)
 *
 */
public class RoutingHandler {
	
	//File contenente gli indirizzi dei supernodi
	private final String info = "supernodes.list";
	//Lista usata per tener traccia dei supernodi ( noti )
	private final Set<NodeInfo> listOfSuperNodes;
	//Lista usata per memorizzare i nodi attualmente connessi al nodo di riferimento
	private final Set<PublicKey> trustedKeys;
	private final Set<NodeInfo> connectedNodes;
	
	
	/**
	 * il costruttore sia nel caso di nodo che di supernodo inizializza la lista dei supernodi
	 * ( noti a priori e contenuti nel file superNodes.list) e istanzia una lista vuota di noti connessi
	 *  --> partenza "cold start"
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * @throws InvalidKeySpecException 
	 */
	public RoutingHandler() throws IOException, ClassNotFoundException, InvalidKeySpecException, NoSuchAlgorithmException {
		
		listOfSuperNodes = new HashSet<NodeInfo>();
		connectedNodes = new HashSet<NodeInfo>();
		trustedKeys = new HashSet<PublicKey>();
		
		InputStream is = new FileInputStream(info);
		
		if(is != null) {
			
			// Parse/deserialize list of nodes from InputStream
			// format: "host:port:pubkey" pubkey is escaped in hex
			Scanner sc = new Scanner(is);
			
			while(sc.hasNext()){
				
					
					String[] tmp = sc.nextLine().split(":");
					String host = tmp[0];
					int port = Integer.parseInt(tmp[1]);
					PublicKey pubKey = Node.parsePublicKey( Serializer.base64Decode(tmp[2]) ); 
							
					listOfSuperNodes.add(new NodeInfo(pubKey, new InetSocketAddress(host, port), true));
				
			}
		}
		else {
			
			//TODO gestione caso che non trovi il file supernode.info
			throw new IOException();
		}
	}
	
	/**
	 * aggiunge un nodo alla lista di quelli connessi
	 * il supernodo lo deve fare ogni volta che un nodo fa una join
	 * al nodo semplice serve per memorizzare temporaneamente gli altri nodi con cui deve comunicare per i download / upload
	 * 
	 * @param sa
	 */
	public void addConnectedNode(NodeInfo sa) {
		addTrustedKey( sa.getPublicKey() );
		connectedNodes.add( sa );
	}

	public void addTrustedKey( PublicKey key ) {
		trustedKeys.add( key );
	}

	/**
	 * rimuove un nodo dalla lista dei nodi connessi
	 * supernodo: lo fa quando un nodo chiama la LEAVE oppure se risulta inattivo per un tot di tempo ( disconnessione anomala )
	 * 
	 * @param sa
	 */
	public void removeConnectedNode(NodeInfo sa) {
		connectedNodes.remove( sa );
	}
	
	public NodeInfo getConnectedNode( PublicKey key ){
		for( NodeInfo node : connectedNodes )
			if( node.getPublicKey().equals( key ) )
				return node;
		return null;
	}
	
	/**
	 *  restituisce la lista dei supernodi presenti nel network
	 *  
	 * @return
	 */
	public Set<NodeInfo> getSupernodeList() {
		
		//TODO cambiare nome list-> set
		// return a copy of the list
		return new HashSet<NodeInfo>( listOfSuperNodes );
	}
	
	public Set<PublicKey> getTrustedKeys(){
		return trustedKeys;
	}
	
	/**
	 * recupera le informazioni su un nodo connesso raggiungibile ( oggetto classe NodeInfo,
	 * sia esso supernodo o nodo semplice ) partendo da un indirizzo IP
	 * @param isa
	 * @return
	 */
	public NodeInfo getNodeInfoBySocketAddress(InetSocketAddress isa){
		for(NodeInfo supernode : listOfSuperNodes) {
			if(supernode.getAddress().equals(isa))
				return supernode;
		}
		for (NodeInfo simplenode : connectedNodes) {
			if(simplenode.getAddress().equals(isa))
				return simplenode;
		}
			
		return null;
	}
	
	/**
	 * dato un set restituisce la lista ordinata in modo "molto" pseudo casuale
	 * @param listToRandomize
	 * @return
	 */
	public static List<NodeInfo> getRandomOrderedList(Set<NodeInfo> listToRandomize) {
		
		int size = listToRandomize.size();
		
		NodeInfo[] list = listToRandomize.toArray(new NodeInfo[0]);
		
		int randomNumber = new Random().nextInt(size);
		
		List<NodeInfo> returnList = new ArrayList<NodeInfo>(size);
		
		for(int i= 0; i<size; i++)
			returnList.add( list[(randomNumber+i)%size] );
		
		return returnList;
		
	}
}
