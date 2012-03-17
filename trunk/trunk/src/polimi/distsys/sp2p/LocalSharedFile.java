package polimi.distsys.sp2p;


public class LocalSharedFile extends SharedFile {
	
private String path;
	
	public LocalSharedFile() {
		super();
		
		path = null;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}