/**
 * 
 */
package polimi.distsys.sp2p.containers;

import java.io.Serializable;
import java.util.Arrays;


/**
 * @author Ale
 *
 */
public abstract class SharedFile implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 8321633873353650401L;
	protected final String name;
	protected final byte[] hash;
	private final int numberOfPeers;
	
	public SharedFile( String name, byte[] hash) {
		this( name, hash, 1 );
	}
	
	public SharedFile( String name, byte[] hash, int numberOfPeers) {
		this.name = name;
		this.hash = hash;
		this.numberOfPeers = numberOfPeers;
	}
	
	public String getName() {
		return name;
	}
	
	public byte[] getHash() {
		return hash;
	}
	
	public int getNumberOfPeers() {
		return numberOfPeers;
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof SharedFile){
			return Arrays.equals( hash, ((SharedFile)o).hash );
		}else
			return false;
	}
	
	@Override
	public int hashCode(){
		// fa una specie di CRC32 dell'hash
		byte[] partial = new byte[Integer.SIZE/8];
		for(int i=0;i<hash.length;i++)
			partial[i%(Integer.SIZE/8)] ^= hash[i];
		int ret = 0;
		for(int i=0;i<partial.length;i++)
			ret += (partial[i] & 0xFF) << (i*8);
		return ret;
	}
	
}

