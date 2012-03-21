/**
 * 
 */
package polimi.distsys.sp2p.containers;


/**
 * @author Ale
 *
 */
public abstract class SharedFile {
	
	protected String name;
	protected byte[] hash;
	
	public SharedFile() {
		name = null;
		hash = null;
		
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public byte[] getHash() {
		return hash;
	}
	public void setHash(byte[] hash) {
		this.hash = hash;
	}
	
}


