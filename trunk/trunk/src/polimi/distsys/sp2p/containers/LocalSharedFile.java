package polimi.distsys.sp2p.containers;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import polimi.distsys.sp2p.handlers.SecurityHandler;

@SuppressWarnings("serial")
public class LocalSharedFile extends SharedFile {
	
	private final transient File file;
	
	public LocalSharedFile( String path ) throws NoSuchAlgorithmException, IOException {
		this( new File( path ) );
	}

	public LocalSharedFile( File file ) throws NoSuchAlgorithmException, IOException {
		super( file.getName(), SecurityHandler.createHash( file ), file.length() );
		this.file = file;
	}
	
	public File getFile() {
		return file;
	}
	
	public String getName(){
		return file.getName();
	}
	
	public RemoteSharedFile toRemoteSharedFile( NodeInfo node ){
		return new RemoteSharedFile( hash, file.getName(), size, node );
	}

}