package polimi.distsys.sp2p.containers;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.PublicKey;

import polimi.distsys.sp2p.Node;
import polimi.distsys.sp2p.SuperNode;
import polimi.distsys.sp2p.util.Serializer;

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
	
	@Override
	public int hashCode(){
		int count;
		byte[] asd = new byte[ 4 ];
		byte[] bkey = publicKey.getEncoded();
		for( count = 0; count < bkey.length - 4 ; count += 4 ){
			for( int j = 0; j < 4; j++ )
				asd[ j ] ^= bkey[ count + j ];
		}
		for( int j = 0; count + j < bkey.length; j++ )
			asd[ j ] ^= bkey[ count + j ];
		int ret = 0;
		for( int i = 0; i < 4; i++ )
			ret |= asd[ i ] << ( 8 * i );
		return ret;
	}
	
	public static String getNickname( PublicKey key ){
		int len = 6;
		int count;
		byte[] asd = new byte[ len ];
		byte[] bkey = key.getEncoded();
		for( count = 0; count < bkey.length - len ; count += len ){
			for( int j = 0; j < len; j++ )
				asd[ j ] ^= bkey[ count + j ];
		}
		for( int j = 0; count + j < bkey.length; j++ )
			asd[ j ] ^= bkey[ count + j ];
		return Serializer.byteArrayToHexString( asd );
	}

}
