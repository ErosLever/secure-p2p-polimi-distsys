package polimi.distsys.sp2p.handlers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

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
				Request req = sock.getInputStream().readEnum( Request.class );
				switch( req ){
				
				case LIST_AVAILABLE_CHUNKS:
				{
					SharedFile file = sock.getInputStream().readObject( SharedFile.class );
					sock.getInputStream().checkDigest();
					
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
					
					byte[] toSend = found.readChunk( index );
					
					sock.getOutputStream().write( Response.OK );
					sock.getOutputStream().write( toSend );
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
				case CLOSE_CONN:
					
					break loop;
					
				default:
					
					sock.getOutputStream().write( Response.FAIL );
					
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
