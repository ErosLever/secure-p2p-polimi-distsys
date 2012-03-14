/**
 * 
 */
package polimi.distsys.sp2p;

/**
 * @author Ale
 *
 */
public class SharedFile {
	
	private String name;
	private String path;
	private byte[] hash;
	
	
	public SharedFile() {
		name = null;
		path = null;
		hash = null;
		
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public byte[] getHash() {
		return hash;
	}
	public void setHash(byte[] hash) {
		this.hash = hash;
	}

}
