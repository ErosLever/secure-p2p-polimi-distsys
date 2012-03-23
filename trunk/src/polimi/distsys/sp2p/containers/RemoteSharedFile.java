package polimi.distsys.sp2p.containers;

import java.io.Serializable;

public class RemoteSharedFile extends SharedFile implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1422515634395952696L;
	private final NodeInfo owner;
	
	public RemoteSharedFile( String name, byte[] hash, NodeInfo owner ) {
		super( name, hash );
		this.owner = owner;
	}

	public RemoteSharedFile( LocalSharedFile local, NodeInfo owner ) {
		this( local.getName(), local.getHash(), owner );
	}

	public NodeInfo getOwner() {
		return owner;
	}

}
