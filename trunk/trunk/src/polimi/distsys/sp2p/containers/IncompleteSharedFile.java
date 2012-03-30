package polimi.distsys.sp2p.containers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;

import polimi.distsys.sp2p.crypto.StreamCipherInputStream.LimitedInputStream;
import polimi.distsys.sp2p.util.BitArray;

public class IncompleteSharedFile extends SharedFile {

	public static final int CHUNK_SIZE = 256*1024;
	
	private final File dest;
	private final RandomAccessFile randFile;
	private final BitArray chunks;
	
	public static IncompleteSharedFile fromFile( File dest ) throws IOException{
		//TODO validate input
		
		File tmp = new File( dest.getPath() + ".tmp" );
		FileInputStream fis = new FileInputStream( tmp );
		byte[] hash = new byte[ 20 ];
		int count = 0;
		while(count < hash.length)
			count += fis.read(hash, count, hash.length - count);
		fis.close();
		
		return new IncompleteSharedFile( dest, hash, dest.length() );
		
	}
	
	public IncompleteSharedFile( RemoteSharedFile sharedFile, File destination ) throws IOException{
		this( destination, sharedFile.hash, sharedFile.size );
	}
	
	private IncompleteSharedFile( File destination, byte[] hash, long size ) throws IOException {
		super( destination.getName(), hash, size);
		this.dest = destination;
		if(! dest.exists())
			dest.createNewFile();
		this.randFile = new RandomAccessFile( dest, "rw" );
		if( randFile.length() != size )
			randFile.setLength( size );
		this.chunks = getChunksFromFile();
		if( ! getTempFile().exists() )
			persist();
	}
	
	public IncompleteSharedFile( LocalSharedFile sharedFile ) throws IOException{
		super( sharedFile.getName(), sharedFile.hash, sharedFile.size );
		this.dest = sharedFile.getFile();
		this.randFile = new RandomAccessFile( this.dest, "r" );
		this.chunks = new BitArray( (int) Math.ceil( 1.0 * size / CHUNK_SIZE ) );
		for( int i=0;i<chunks.length();i++ )
			chunks.set( i );
	}
	
	private File getTempFile(){
		return new File( dest.getPath() + ".tmp" );
	}
	
	private BitArray getChunksFromFile() throws IOException{
		File tmp = getTempFile();
		if( tmp.exists() ){
			FileInputStream fis = new FileInputStream( tmp );
			fis.skip( hash.length );
			BitArray chunks = BitArray.deserialize( fis );
			fis.close();
			return chunks;
		}else{
			return new BitArray( (int) Math.ceil( 1.0 * size / CHUNK_SIZE ) );
		}
	}
	
	public BitArray getChunks(){
		return chunks;
	}
	
	public synchronized byte[] readChunk(int i) throws IOException{
		
		randFile.seek( 1L * CHUNK_SIZE * i );
		int toRead = (int) Math.min( CHUNK_SIZE, size - i*CHUNK_SIZE );
		byte[] chunk = new byte[toRead];
		int count = 0;
		while( count < toRead && count >= 0 ){
			count += randFile.read( chunk, count, toRead - count );
		}
		return chunk;
	}
	
	public synchronized InputStream getChunkAsInputStream(int i) throws IOException{
		FileInputStream fis = new FileInputStream( dest );
		fis.skip( 1L * CHUNK_SIZE * i );
		int toRead = (int) Math.min( CHUNK_SIZE, size - i*CHUNK_SIZE );
		return new LimitedInputStream( fis, toRead );
	}

	
	public synchronized void writeChunk(int i, byte[] chunk) throws IOException{
		
		randFile.seek( 1L * CHUNK_SIZE * i );
		randFile.write( chunk );
		
		chunks.set( i );
		
		persist();
	}
	
	public synchronized void writeChunk(int i, InputStream chunk) throws IOException{
		int toRead = (int) Math.min( CHUNK_SIZE, size - i*CHUNK_SIZE );
		chunk = new LimitedInputStream( chunk, toRead );
		randFile.seek( 1L * CHUNK_SIZE * i );
		int count;
		byte[] buf = new byte[1024];
		while( (count = chunk.read(buf)) != -1 ){
			randFile.write( buf, 0, count );
			toRead -= count;
		}
		if( toRead > 0 ){
			throw new IOException("Stream end reached early");
		}
		chunks.set( i );
		
		persist();
	}
	
	public synchronized void persist() throws IOException{
		File tmp = getTempFile();
		FileOutputStream out = new FileOutputStream( tmp );
		out.write( hash );
		chunks.serialize( out );
		out.close();
	}
	
	public boolean isCompleted(){
		return chunks.count() == chunks.length();
	}
	
	public void matchCheckSum() throws GeneralSecurityException, IOException{
		if( isCompleted() ){
			LocalSharedFile test = new LocalSharedFile( dest );
			if( ! this.equals( test ) )
				throw new GeneralSecurityException( "Checksum does not match" );
		}else
			throw new IllegalStateException( "The file is still incomplete" );
	}
	
	public RemoteSharedFile toRemoteSharedFile( NodeInfo node ){
		return new RemoteSharedFile( hash, dest.getName(), size, node);
	}

}
