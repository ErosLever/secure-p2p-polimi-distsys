package polimi.distsys.sp2p.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.Date;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.IncompleteSharedFile;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.SharedFile;
import polimi.distsys.sp2p.containers.messages.Message.Request;
import polimi.distsys.sp2p.containers.messages.Message.Response;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedServerSocket;
import polimi.distsys.sp2p.util.Listener.ListenerCallback;

public abstract class SimpleNodeServer implements ListenerCallback {

	private final SimpleNode node;
	
	public SimpleNodeServer(SimpleNode node){
		this.node = node;
	}
	
	public abstract void addTrustedDownload( NodeInfo node, SharedFile file );
	
	public abstract NodeInfo getCorrespondingNode( PublicKey key );
	
	public abstract EncryptedServerSocket getEncryptedServerSocket( Socket sock ) throws IOException, GeneralSecurityException;
	
	@Override
	public void handleRequest(SocketChannel client) {
		
		try {
			EncryptedServerSocket sock = getEncryptedServerSocket( client.socket() );
			NodeInfo clientNode = getCorrespondingNode( sock.getClientPublicKey() );
			
loop:		while( true ){
				Request req = null;
				try{
					req = sock.getInputStream().readEnum( Request.class );
				}catch(IOException e){
					break;
				}
				
				String clientName = NodeInfo.getNickname( sock.getClientPublicKey() );
				String timestamp = new Timestamp( new Date().getTime() ).toString();
				System.out.println(timestamp+" "+req+" da "+clientName);

				switch( req ){
				
				case LIST_AVAILABLE_CHUNKS:
				{
					SharedFile file = sock.getInputStream().readObject( SharedFile.class );
					sock.getInputStream().checkDigest();
					
					if( ! node.getRoutingHandler().getAllowedDownloads( clientNode )
							.contains( file ) ){
						sock.getOutputStream().write( Response.FAIL );
						System.out.println("Attempted unauthorized access from node: "+ clientNode );
						break;
					}
					
					IncompleteSharedFile toSend = SearchHandler.searchLocal( 
							file, node.getFileList(), node.getIncompleteFiles() );
					
					sock.getOutputStream().write( Response.OK );
					ByteArrayOutputStream serialized = new ByteArrayOutputStream();
					toSend.getChunks().serialize( serialized );
					serialized.close();
					sock.getOutputStream().write( serialized.toByteArray() );
					sock.getOutputStream().sendDigest();
					
					break;
				}
				case FETCH_CHUNK:
				{
					SharedFile file = sock.getInputStream().readObject( SharedFile.class );
					int index = sock.getInputStream().readInt();
					sock.getInputStream().checkDigest();
					
					IncompleteSharedFile found = SearchHandler.searchLocal( 
							file, node.getFileList(), node.getIncompleteFiles() );
					
					int size;
					if( index < found.getChunks().length() -1 )
						size = IncompleteSharedFile.CHUNK_SIZE;
					else
						size = (int) (found.getSize() - (index -1) * IncompleteSharedFile.CHUNK_SIZE);
					
					InputStream chunk = found.getChunkAsInputStream( index );
					
					sock.getOutputStream().write( Response.OK );
					sock.getOutputStream().write( sock.getOutputStream().getOutputSize( size ) );
					sock.getOutputStream().write( chunk );
					chunk.close();
					sock.getOutputStream().sendDigest();
					
					break;
				}
				
				case ADD_TRUSTED_DOWNLOAD:
				{
					if( clientNode.isSuper() ){
						
						NodeInfo toAdd = sock.getInputStream().readObject( NodeInfo.class );
						SharedFile file = sock.getInputStream().readObject( SharedFile.class );
						sock.getInputStream().checkDigest();
						
						addTrustedDownload( toAdd, file );
						sock.getOutputStream().write( Response.OK );
						sock.getOutputStream().sendDigest();
						
					}else{
						sock.getOutputStream().write( Response.FAIL );
					}
					break;
				}
				case PING:
				{
					sock.getOutputStream().write( Response.PONG );
					break;
				}
				case CLOSE_CONN:
					
					break loop;
					
				default:
					
					sock.getOutputStream().write( Response.FAIL );
					break loop;
					
				}
				sock.getOutputStream().flush();
			}
			sock.close();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}

}
