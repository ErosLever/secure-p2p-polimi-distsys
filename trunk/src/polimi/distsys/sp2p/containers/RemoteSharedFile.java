package polimi.distsys.sp2p.containers;

import java.io.Serializable;
import java.util.Set;

public class RemoteSharedFile extends SharedFile implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1422515634395952696L;
	private final Set<NodeInfo> peers;
	
	public RemoteSharedFile( String name, byte[] hash, Set<NodeInfo> peers ) {
		super( name, hash );
		this.peers = peers;
	}

	public Set<NodeInfo> getPeers() {
		return peers;
	}

}
