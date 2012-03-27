/**
 * 
 */
package polimi.distsys.sp2p.containers;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;


/**
 * @author Ale
 *
 */
public abstract class SharedFile implements Serializable {
	
	private static final long serialVersionUID = 8321633873353650401L;
	
	protected final Collection<String> filenames;
	protected final byte[] hash;
	protected final long size;
	protected int numberOfPeers;
	
	public SharedFile( String name, byte[] hash, long size) {
		this( name, hash, size, 1 );
	}
	
	public SharedFile( String name, byte[] hash, long size, int numberOfPeers) {
		this( Collections.singletonList( name ), hash, size, numberOfPeers);
	}
	
	public SharedFile( Collection<String> filenames, byte[] hash, long size, int numberOfPeers) {
		this.filenames = filenames;
		this.hash = hash;
		this.numberOfPeers = numberOfPeers;
		this.size = size;
	}
	
	public Collection<String> getFileNames() {
		return filenames;
	}
	
	public byte[] getHash() {
		return hash;
	}
	
	public int getNumberOfPeers() {
		return numberOfPeers;
	}
	
	public boolean hasPeers(){
		return numberOfPeers > 0;
	}
	
	public long getSize(){
		return size;
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


