package polimi.distsys.sp2p.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import polimi.distsys.sp2p.containers.IncompleteSharedFile;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.SharedFile;
import polimi.distsys.sp2p.containers.messages.Message.Request;
import polimi.distsys.sp2p.containers.messages.Message.Response;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedClientSocket;
import polimi.distsys.sp2p.util.BitArray;

public class DownloadHandler extends Thread {
	
	public static final int CHUNK_SIZE = IncompleteSharedFile.CHUNK_SIZE;
	//public static final int SUB_CHUNK = 1024;
	public static final int RESFRESH_CHUNK_AVAILABILITY = 30 * 1000; 
	
	private final EncryptedSocketFactory enSockFact;
	
	private final RemoteSharedFile remoteFile;
	private final IncompleteSharedFile incompleteFile;
	
	private final List<Integer> queue;
	private final List<NodeQuerySender> threads;
	
	private final DownloadCallback callback;
	
	private Exception exception;
	
	public DownloadHandler( EncryptedSocketFactory enSockfact, RemoteSharedFile file, File dest, DownloadCallback callback ) throws FileNotFoundException, IOException{
		this.enSockFact = enSockfact;
		this.remoteFile = file;
		this.incompleteFile = new IncompleteSharedFile( file, dest );
		this.exception = null;
		this.queue = Collections.synchronizedList( new Vector<Integer>() );
		threads = new Vector<NodeQuerySender>( file.getNumberOfPeers() );
		this.callback = callback; 
	}
	
	public void run(){
		for( NodeInfo peer : remoteFile.getPeers() ){
			threads.add( new NodeQuerySender( peer, callback ) );
			threads.get( threads.size() -1 ).start();
		}
		for( Thread t : threads )
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		callback.endOfDownload( incompleteFile.getChunks() );
		
	}
	
	public void setActive(boolean active){
		for( NodeQuerySender thread : threads )
			thread.setActive( active );
	}
	
	public IncompleteSharedFile getIncompleteFile(){
		return incompleteFile;
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
				
				callback.askCommunicationToNode( node, incompleteFile.toRemoteSharedFile( node ) );
				
				EncryptedClientSocket sock = enSockFact.getEncryptedClientSocket( 
						node.getAddress(), node.getPublicKey() );
				
				BitArray availableChunks = refreshRemoteChunks( sock );
				long lastUpdate = System.currentTimeMillis();
				
				for(int i=0;i<incompleteFile.getChunks().length();i++){
					
					if( ! active )
						closeConn(sock);
					
					if( System.currentTimeMillis() - lastUpdate > RESFRESH_CHUNK_AVAILABILITY ){
						availableChunks = refreshRemoteChunks( sock );
						lastUpdate = System.currentTimeMillis();
					}
					
					// mi serve
					if( ! incompleteFile.getChunks().get( i ) ){
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
							int chunkSize = CHUNK_SIZE;
							if( i == incompleteFile.getChunks().length() -1){
								//l'utlimo chunk può essere più piccolo
								chunkSize = (int) (incompleteFile.getSize() - i * CHUNK_SIZE);
							}
							downloadChunk( sock, i, chunkSize );
							
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
			sock.getOutputStream().writeVariableSize( DownloadHandler.this.remoteFile );
			sock.getOutputStream().sendDigest();
			sock.getOutputStream().flush();
			
			if( ! active )
				closeConn(sock);
			
			Response reply = sock.getInputStream().readEnum( Response.class );
			if( !reply.equals( Response.OK ) )
				throw new IOException("Something went wrong while preparing download from "+node );
			BitArray availableChunks = BitArray.deserialize( 
					sock.getInputStream().readFixedSize( 
							DownloadHandler.this.incompleteFile.getChunks().length() ) );
			sock.getInputStream().checkDigest();
			return availableChunks;
		}
		
		public void downloadChunk( EncryptedClientSocket sock, int i, int chunkSize ) throws IOException, GeneralSecurityException{
			sock.getOutputStream().write( Request.FETCH_CHUNK );
			sock.getOutputStream().writeVariableSize( incompleteFile.toRemoteSharedFile( node ) );
			sock.getOutputStream().write( i );
			sock.getOutputStream().sendDigest();
			
			Response reply = sock.getInputStream().readEnum( Response.class );
			if( reply.equals( Response.OK ) ){
				
				InputStream chunk = sock.getInputStream().readFixedSize( chunkSize );
				incompleteFile.writeChunk( i, chunk );
				chunk.close();
				sock.getInputStream().checkDigest();
				
				
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
		
		public void askCommunicationToNode( NodeInfo node, SharedFile sharedFile ) throws IOException, GeneralSecurityException;
		
	}

}
