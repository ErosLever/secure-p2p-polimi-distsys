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
		callback.endOfDownload( incompleteFile );
		
	}
	
	public void setActive(boolean active){
		for( NodeQuerySender thread : threads )
			thread.setActive( active );
	}
	
	public boolean isActive() {
		for( NodeQuerySender thread : threads ) {
			if(thread.isActive())
				return true;
		}
		return false;
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
		private EncryptedClientSocket sock;
		private long lastCommunicationTime;
		private BitArray availableChunks;
		
		public NodeQuerySender( NodeInfo ni, DownloadCallback dc ){
			node = ni;
			callback = dc;
			lastCommunicationTime = 0;
		}
		
		public void run(){
			try {
				if( ! active )
					return;
				
				callback.askCommunicationToNode( node, incompleteFile.toRemoteSharedFile( node ) );
				
				refreshRemoteChunks();
				long lastUpdate = System.currentTimeMillis();
				
				for(int i=0;i<incompleteFile.getChunks().length();i++){
					
					if( ! active )
						closeConn(sock);
					
					if( System.currentTimeMillis() - lastUpdate > RESFRESH_CHUNK_AVAILABILITY ){
						refreshRemoteChunks();
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
							
							Thread.sleep( 1000 );
							
						}
					}
				}
				
				
				
				
			} catch (Exception e) {
				DownloadHandler.this.exception = e;
				callback.gotException( incompleteFile, e );
			}
			
		}
		
		private void closeConn(EncryptedClientSocket sock) throws IOException{
			sock.getOutputStream().write( Request.CLOSE_CONN );
			sock.getOutputStream().flush();
			sock.close();
		}
		
		private void refreshRemoteChunks() throws IOException, GeneralSecurityException, ClassNotFoundException{
			
			checkConnectionWithPeerNode();
			
			sock.getOutputStream().write( Request.LIST_AVAILABLE_CHUNKS );
			sock.getOutputStream().writeVariableSize( DownloadHandler.this.remoteFile );
			sock.getOutputStream().sendDigest();
			sock.getOutputStream().flush();
			
			if( ! active )
				closeConn(sock);
			
			Response reply = sock.getInputStream().readEnum( Response.class );
			if( !reply.equals( Response.OK ) )
				throw new IOException("Something went wrong while preparing download from "+node );
			int size = sock.getInputStream().readInt();
			//System.out.println( "Size to read -> "+size);
			BitArray availableChunks = BitArray.deserialize( 
					sock.getInputStream().readFixedRawSize( size ) );
			sock.getInputStream().checkDigest();
			this.availableChunks = availableChunks;
		}
		
		public void downloadChunk( EncryptedClientSocket sock, int i, int chunkSize ) throws IOException, GeneralSecurityException, ClassNotFoundException{
			
			checkConnectionWithPeerNode();

			sock.getOutputStream().write( Request.FETCH_CHUNK );
			sock.getOutputStream().writeVariableSize( incompleteFile.toRemoteSharedFile( node ) );
			sock.getOutputStream().write( i );
			sock.getOutputStream().sendDigest();
			
			Response reply = sock.getInputStream().readEnum( Response.class );
			if( reply.equals( Response.OK ) ){
				
				int size = sock.getInputStream().readInt();
				//System.out.println( "Size to read - "+size);
				InputStream chunk = sock.getInputStream().readFixedRawSize( size );
				incompleteFile.writeChunk( i, chunk );
				chunk.close();
				sock.getInputStream().checkDigest();
				
				callback.receivedChunk( incompleteFile, i );
			}
			
			// serve il cast perchè altrimenti rimuove 
			// l'oggetto di posizione i-esima
			queue.remove( (Integer) i );
			
		}
		
		public void setActive(boolean active){
			this.active = active;
		}

		public boolean isActive() {
			return active;
		}
		
		public void checkConnectionWithPeerNode() throws GeneralSecurityException, IOException, ClassNotFoundException{
			if( sock == null ? true : ! sock.isConnected() ){
				sock = enSockFact.getEncryptedClientSocket( 
						node.getAddress(), node.getPublicKey() );
			}else{
				if( System.currentTimeMillis() - lastCommunicationTime < EncryptedSocketFactory.SOCKET_TIMEOUT )
					return;
				try{
					sock.getOutputStream().write( Request.PING );
					sock.getOutputStream().flush();
					Response reply = sock.getInputStream().readEnum( Response.class );
					if( !reply.equals( Response.PONG ) )
						throw new IOException();
				}catch(IOException e){
					sock.close();
					sock = enSockFact.getEncryptedClientSocket( 
							node.getAddress(), node.getPublicKey() );
				}
			}
			lastCommunicationTime = System.currentTimeMillis();
		}
	}
	
	public static interface DownloadCallback {
		
		public void receivedChunk( IncompleteSharedFile isf, int i );
		
		public void endOfDownload( IncompleteSharedFile isf );
		
		public void gotException( IncompleteSharedFile isf, Exception ex );
		
		public void askCommunicationToNode( NodeInfo node, SharedFile sharedFile ) throws IOException, GeneralSecurityException;
		
		
	}

}
