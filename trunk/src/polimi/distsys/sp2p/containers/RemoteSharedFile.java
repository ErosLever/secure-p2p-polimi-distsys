package polimi.distsys.sp2p.containers;
import java.net.InetAddress;



public class RemoteSharedFile extends SharedFile {
	
	private InetAddress ip;
	private int port;
	
	public RemoteSharedFile() {
		super();
		
		port = 0;
		ip = null;
		
	}
	public InetAddress getIp() {
		return ip;
	}
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

}
