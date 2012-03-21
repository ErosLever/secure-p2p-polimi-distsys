package polimi.distsys.sp2p.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

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
	private final String info = "superNodes.list";
	//Lista usata per tener traccia dei supernodi ( noti )
	private final Set<NodeInfo> listOfSuperNodes;
	//Lista usata per memorizzare i nodi attualmente connessi al nodo di riferimento
	private  Set<NodeInfo> connectedNodes;
	
	
	/**
	 * il costruttore sia nel caso di nodo che di supernodo inizializza la lista dei supernodi
	 * ( noti a priori e contenuti nel file superNodes.list) e istanzia una lista vuota di noti connessi
	 *  --> partenza "cold start"
	 * @throws ClassNotFoundException 
	 * @throws IOException 
	 */
	public RoutingHandler() throws IOException, ClassNotFoundException {
		
		listOfSuperNodes = new TreeSet<NodeInfo>();
		connectedNodes = new TreeSet<NodeInfo>();
		
		InputStream is = RoutingHandler.class.getResourceAsStream(info);
		
		if(is != null) {
			
			// Parse/deserialize list of nodes from InputStream
			// format: "host:port:pubkey" pubkey is escaped in hex
			Scanner sc = new Scanner(is);
			
			while(sc.hasNext()){
				
					
					String[] tmp = sc.nextLine().split(":");
					String host = tmp[0];
					int port = Integer.parseInt(tmp[1]);
					PublicKey pubKey = Serializer.deserialize(
							Serializer.base64Decode(tmp[2]), 
							PublicKey.class);
							
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
		connectedNodes.add( sa );
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
	
	
	/**
	 *  restituisce la lista dei supernodi presenti nel network
	 *  
	 * @return
	 */
	public Set<NodeInfo> getSupernodeList() {
		
		//TODO cambiare nome list-> set
		// return a copy of the list
		return new TreeSet<NodeInfo>( listOfSuperNodes );
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
