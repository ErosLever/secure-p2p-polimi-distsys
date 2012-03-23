/**
 * 
 */
package polimi.distsys.sp2p.containers;

import java.io.Serializable;


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


