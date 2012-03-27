package polimi.distsys.sp2p.handlers;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.LocalSharedFile;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.messages.Message.Request;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedServerSocket;
import polimi.distsys.sp2p.util.Listener.ListenerCallback;

public abstract class SimpleNodeServer implements ListenerCallback {

	private final SimpleNode node;
	
	public SimpleNodeServer(SimpleNode node){
		this.node = node;
	}
	
	public abstract void addTrustedKey( PublicKey key );
	
	public abstract boolean isTrustedKey( PublicKey key );
	
	public abstract EncryptedServerSocket getEncryptedServerSocket( Socket sock ) throws IOException, GeneralSecurityException;
	
	@Override
	public void handleRequest(SocketChannel client) {
		
		try {
			EncryptedServerSocket sock = getEncryptedServerSocket( client.socket() );
			
			while( true ){
				Request req = sock.getInputStream().readEnum( Request.class );
				switch( req ){
				
				case LIST_AVAILABLE_CHUNKS:
					
					RemoteSharedFile file = sock.getInputStream().readObject( RemoteSharedFile.class );
					sock.getInputStream().checkDigest();
					
					RemoteSharedFile toSend = null;
					for( LocalSharedFile sf : node.getFileList() ){
						if( file.equals( sf ) ){
							toSend = new RemoteSharedFile(
									sf.getHash(), sf.getName(), sf.getSize(), node.getNodeInfo() );
							break;
						}
					}
					
					if( toSend == null ){
						for( RemoteSharedFile sf : node.getIncompleteFiles() ){
							if( file.equals( sf ) ){
								toSend = sf;
								break;
							}
						}
					}
					
					//TODO get BitArray representing available chunks
					//TODO send BitArray
					
					break;
				
				case FETCH_CHUNK:
					
					break;
				
				case CLOSE_CONN:
					
					break;
					
				default:
					
					
					
				}
			}
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
	}

}
