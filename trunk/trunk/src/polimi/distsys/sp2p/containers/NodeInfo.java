package polimi.distsys.sp2p.containers;

import java.net.InetSocketAddress;
import java.security.PublicKey;

import polimi.distsys.sp2p.Node;
import polimi.distsys.sp2p.SuperNode;

public class NodeInfo implements Comparable<NodeInfo> {

	private final PublicKey publicKey;
	private final InetSocketAddress address;
	private final boolean isSuper;


	public NodeInfo(Node sn){
		
		publicKey = sn.getPublicKey();
		address = (new 	InetSocketAddress(sn.getSocketAddress().getInetAddress(), sn.getSocketAddress().getLocalPort()));
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
	public int compareTo(NodeInfo o) {
		if(o.isSuper && !this.isSuper)
			return -1;
		else if(!o.isSuper && this.isSuper)
			return 1;
		else
			return address.hashCode() - o.address.hashCode();
	}

}
