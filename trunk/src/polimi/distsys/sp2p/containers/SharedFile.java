/**
 * 
 */
package polimi.distsys.sp2p.containers;


/**
 * @author Ale
 *
 */
public abstract class SharedFile {
	
	protected final String name;
	protected final byte[] hash;
	
	public SharedFile( String name, byte[] hash) {
		this.name = name;
		this.hash = hash;
	}
	
	public String getName() {
		return name;
	}
	
	public byte[] getHash() {
		return hash;
	}
	
}


