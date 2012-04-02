package polimi.distsys.sp2p.containers;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RemoteSharedFile extends SharedFile implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1422515634395952696L;
	private final Set<NodeInfo> peers;
	
	public RemoteSharedFile( byte[] hash, String filename, long size, NodeInfo peer ) {
		this( hash, Collections.singleton( filename ), size, peer );
	}
	
	public RemoteSharedFile( byte[] hash, Collection<String> filenames, long size, NodeInfo peer ) {
		super( filenames, hash, size, 1 );
		peers = new HashSet<NodeInfo>();
		peers.add( peer );
	}

	public Set<NodeInfo> getPeers() {
		return peers;
	}
	
	public void addPeer( NodeInfo peer, String filename ){
		addPeer( peer, Collections.singleton( filename ) );
	}
	
	public void addPeer( NodeInfo peer, Collection<String> filenames ){
		if( ! peers.contains( peer ) ){
			peers.add( peer );
			numberOfPeers++;
		}
		for( String filename : filenames )
			if( ! this.filenames.contains( filename ) )
				this.filenames.add( filename );
	}
	
	public void removePeer( NodeInfo peer ){
		boolean removed = peers.remove( peer );
		if( removed )
			numberOfPeers--;
	}
	
	public void merge( RemoteSharedFile rsf ){
		for( NodeInfo ni : rsf.peers ){
			if( ! this.peers.contains( ni ) )
				addPeer( ni, rsf.getFileNames() );
		}
	}
	
}
