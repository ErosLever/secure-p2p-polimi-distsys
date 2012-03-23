package polimi.distsys.sp2p.containers;

public class RemoteSharedFile extends SharedFile {
	
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
