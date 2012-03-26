package polimi.distsys.sp2p.containers;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.PublicKey;

import polimi.distsys.sp2p.Node;
import polimi.distsys.sp2p.SuperNode;

public class NodeInfo implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2169907759845274030L;
	private final PublicKey publicKey;
	private final InetSocketAddress address;
	private final boolean isSuper;


	public NodeInfo(Node sn){
		
		publicKey = sn.getPublicKey();
		address = sn.getSocketAddress();
		isSuper = sn instanceof SuperNode;
	}

	public NodeInfo(PublicKey pk, InetSocketAddress sock, boolean isSuper){
		publicKey = pk;
		address = sock;
		this.isSuper = isSuper;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public InetSocketAddress getAddress() {
		return address;
	}

	public boolean isSuper() {
		return isSuper;
	}

	@Override
	public boolean equals(Object o) {
		if( o instanceof NodeInfo ) {
			return ((NodeInfo)o).publicKey.equals( publicKey );
		}
		return false;
	}

}
