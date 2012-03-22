package polimi.distsys.sp2p.containers;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import polimi.distsys.sp2p.handlers.SecurityHandler;


public class LocalSharedFile extends SharedFile {
	
	private final File file;
	
	public LocalSharedFile( String path ) throws NoSuchAlgorithmException, IOException {
		this( new File( path ) );
	}

	public LocalSharedFile( File file ) throws NoSuchAlgorithmException, IOException {
		super( file.getName(), SecurityHandler.createHash( file ) );
		this.file = file;
	}

	public File getFile() {
		return file;
	}

}