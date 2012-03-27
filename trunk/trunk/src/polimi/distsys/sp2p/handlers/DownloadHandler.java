package polimi.distsys.sp2p.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.messages.Message.Request;
import polimi.distsys.sp2p.containers.messages.Message.Response;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedClientSocket;
import polimi.distsys.sp2p.util.BitArray;

public class DownloadHandler extends Thread {
	
	public static final int CHUNK_SIZE = 512 * 1024; 
	public static final int RESFRESH_CHUNK_AVAILABILITY = 30 * 1000; 
	
	private final EncryptedSocketFactory enSockFact;
	
	private final RemoteSharedFile file;
	private final RandomAccessFile dest;
	
	private final File tmp;
	private final BitArray receivedChunks;
	private final List<Integer> queue;
	private final List<NodeQuerySender> threads;
	
	private final DownloadCallback callback;
	
	private Exception exception;
	
	public DownloadHandler( EncryptedSocketFactory enSockfact, RemoteSharedFile file, File dest, DownloadCallback callback ) throws FileNotFoundException, IOException{
		this.enSockFact = enSockfact;
		this.file = file;
		this.dest = new RandomAccessFile( dest, "ab" );
		if( this.dest.length() != file.getSize() )
			this.dest.setLength( file.getSize() );
		this.tmp = new File( dest.getPath() + ".tmp" );
		this.exception = null;
		this.queue = Collections.synchronizedList( new Vector<Integer>() );
		threads = new Vector<NodeQuerySender>( file.getNumberOfPeers() );
		this.callback = callback; 
		
		BitArray receivedChunks;
		if( tmp.exists() ){
			FileInputStream fis = new FileInputStream( tmp );
			fis.skip( file.getHash().length );
			receivedChunks = BitArray.deserialize( fis );
			fis.close();
		}else{
			receivedChunks = new BitArray( (int) Math.ceil( 1.0 * file.getSize() / CHUNK_SIZE ) );
			FileOutputStream fos = new FileOutputStream( tmp );
			fos.write( file.getHash() );
			receivedChunks.serialize( fos );
			fos.close();
		}
		this.receivedChunks = receivedChunks;
	}
	
	public void run(){
		for( NodeInfo peer : file.getPeers() ){
			threads.add( new NodeQuerySender( peer, callback ) );
			threads.get( threads.size() -1 ).start();
		}
		for( Thread t : threads )
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		callback.endOfDownload( receivedChunks );
		
	}
	
	public void setActive(boolean active){
		for( NodeQuerySender thread : threads )
			thread.setActive( active );
		
	}
	
	public Exception checkException(){
		return exception;
	}
	
	public class NodeQuerySender extends Thread {
		
		private final NodeInfo node;
		private final DownloadCallback callback;
		private boolean active = true;
		
		public NodeQuerySender( NodeInfo ni, DownloadCallback dc ){
			node = ni;
			callback = dc;
		}
		
		public void run(){
			try {
				if( ! active )
					return;
				EncryptedClientSocket sock = enSockFact.getEncryptedClientSocket( 
						node.getAddress(), node.getPublicKey() );
				
				BitArray availableChunks = refreshRemoteChunks( sock );
				long lastUpdate = System.currentTimeMillis();
				
				for(int i=0;i<receivedChunks.length();i++){
					
					if( ! active )
						closeConn(sock);
					
					if( System.currentTimeMillis() - lastUpdate > RESFRESH_CHUNK_AVAILABILITY ){
						availableChunks = refreshRemoteChunks( sock );
						lastUpdate = System.currentTimeMillis();
					}
					
					// mi serve
					if( ! receivedChunks.get( i ) ){
						// è disponibile
						if( availableChunks.get( i ) ){
							
							synchronized(queue){
								// se è già in coda
								if( queue.contains( i ) ){
									// lo salto
									continue;
								}
								// altrimenti lo scarico
								queue.add( i );
							}
							
							downloadChunk( sock, i );
							
						}
					}
				}
				
				
				
				
			} catch (Exception e) {
				DownloadHandler.this.exception = e;
				callback.gotException( e );
			}
			
		}
		
		private void closeConn(EncryptedClientSocket sock) throws IOException{
			sock.getOutputStream().write( Request.CLOSE_CONN );
			sock.getOutputStream().flush();
			sock.close();
		}
		
		private BitArray refreshRemoteChunks(EncryptedClientSocket sock) throws IOException, GeneralSecurityException{
			sock.getOutputStream().write( Request.LIST_AVAILABLE_CHUNKS );
			sock.getOutputStream().writeVariableSize( DownloadHandler.this.file );
			sock.getOutputStream().sendDigest();
			sock.getOutputStream().flush();
			
			if( ! active )
				closeConn(sock);
			
			Response reply = sock.getInputStream().readEnum( Response.class );
			if( !reply.equals( Response.OK ) )
				throw new IOException("Something went wrong while preparing download from "+node );
			BitArray availableChunks = BitArray.deserialize( 
					sock.getInputStream().readFixedSize( 
							DownloadHandler.this.receivedChunks.length() ) );
			sock.getInputStream().checkDigest();
			return availableChunks;
		}
		
		public void downloadChunk( EncryptedClientSocket sock, final int i ) throws IOException, GeneralSecurityException{
			sock.getOutputStream().write( Request.FETCH_CHUNK );
			sock.getOutputStream().write( i );
			sock.getOutputStream().sendDigest();
			
			Response reply = sock.getInputStream().readEnum( Response.class );
			if( reply.equals( Response.OK ) ){
				
				final byte[] chunk = sock.getInputStream().readFixedSizeAsByteArray( CHUNK_SIZE );
				sock.getInputStream().checkDigest();
				
				synchronized( file ){
					dest.seek( 1L * CHUNK_SIZE * i );
					dest.write( chunk );
				}
				
				synchronized( tmp ){
					receivedChunks.set( i );
					FileOutputStream out = new FileOutputStream( tmp );
					receivedChunks.serialize( out );
					out.close();
					// callback
					new Thread(){
						public void run(){
							callback.receivedChunk( i, chunk );
						}
					}.start();
				}
			}
			
			// serve il cast perchè altrimenti rimuove 
			// l'oggetto di posizione i-esima
			queue.remove( (Integer) i );
			
		}
		
		public void setActive(boolean active){
			this.active = active;
		}
		
	}
	
	public static interface DownloadCallback {
		
		public void receivedChunk( int i , byte[] value );
		
		public void endOfDownload( BitArray writtenChunks );
		
		public void gotException( Exception ex );
		
	}

}
