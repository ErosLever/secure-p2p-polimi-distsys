package polimi.distsys.sp2p.containers;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RemoteSharedFile extends SharedFile implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1422515634395952696L;
	private final Map<NodeInfo, String> peers;
	
	public RemoteSharedFile( byte[] hash, String filename, NodeInfo peer ) {
		super( filename, hash );
		peers = new HashMap<NodeInfo, String>();
		peers.put( peer, filename );
	}

	public Set<NodeInfo> getPeers() {
		return peers.keySet();
	}
	
	public void addPeer( String filename, NodeInfo peer ){
		peers.put( peer, filename );
		numberOfPeers++;
	}
	
	public void removePeer( NodeInfo peer ){
		peers.remove( peer );
		numberOfPeers--;
	}
	
	@Override
	public Collection<String> getFileNames() {
		return peers.values();
	}

}
