package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.SharedFile;
import polimi.distsys.sp2p.containers.messages.Message.Request;
import polimi.distsys.sp2p.containers.messages.Message.Response;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedClientSocket;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedServerSocket;
import polimi.distsys.sp2p.handlers.SearchHandler;
import polimi.distsys.sp2p.util.Listener;
import polimi.distsys.sp2p.util.Listener.ListenerCallback;
import polimi.distsys.sp2p.util.PortChecker;
import polimi.distsys.sp2p.util.Serializer;


/**
 * 
 */

/**
 * @author Ale
 *
 */
public class SuperNode extends Node implements ListenerCallback {

	//file dove vengono memorizzate le public key dei simple node
	private static final String CREDENTIALS_FILE = "credentials.list";
	//file da cui recuperare le informazioni del nodo
	private static final String infoFile = "supernode.info";

	// struttura dati in cui vengono salvate le credenziali dei nodi ( public keys )
	private final Set<PublicKey> credentials;
	
	private final Map<PublicKey, NodeInfo> connectedClients;
	
	private final List<RemoteSharedFile> files; 
	
	@SuppressWarnings("unused")
	private final Listener listener;

	public static SuperNode fromFile() throws IOException, ClassNotFoundException, GeneralSecurityException{
		return fromFile( new File( infoFile ) );
	}

	public static SuperNode fromFile( File file ) throws IOException, ClassNotFoundException, GeneralSecurityException{
		return fromFile( file, new File( CREDENTIALS_FILE ) );
	}
	
	public static SuperNode fromFile( File file, File credentials) throws IOException, ClassNotFoundException, GeneralSecurityException{
		//legge il file per recuperare chiave pubblica, privata, indirizzo e porta del nodo
		Scanner sc = new Scanner( new FileInputStream( file ) );
		String[] tmp = sc.nextLine().split(":");
		PublicKey pub = parsePublicKey( Serializer.base64Decode( tmp[0] ) );
		PrivateKey priv = parsePrivateKey( Serializer.base64Decode( tmp[1] ) ); 
		ServerSocket socket = PortChecker.getBoundedServerSocketChannelOrNull(
				Integer.parseInt(tmp[2])).socket();
		sc.close();
		return new SuperNode(pub, priv, socket,  credentials );
	}

	private SuperNode(PublicKey pub, PrivateKey priv, ServerSocket sock, File credentials ) throws IOException, ClassNotFoundException, GeneralSecurityException {
		//inizializza il routerHandler
		super( pub, priv, sock );

		this.credentials = new HashSet<PublicKey>();
		this.connectedClients = new HashMap<PublicKey, NodeInfo>();
		this.files = new Vector<RemoteSharedFile>();

		//legge le credenziali
		Scanner sc = new Scanner( new FileInputStream( credentials ) );
		while(sc.hasNext()) {
			PublicKey tmpKey = parsePublicKey(Serializer.base64Decode(sc.nextLine())); 
			this.credentials.add( tmpKey );
			rh.addTrustedKey( tmpKey );
		}
		
		for( NodeInfo supernode : rh.getSupernodeList() ){
			this.credentials.add( supernode.getPublicKey() );
			rh.addTrustedKey( supernode.getPublicKey() );
		}
		
		sc.close();

		//inizializza il listener
		listener = new Listener(socket.getChannel(), this);

	}	


	//HANDLE REQUEST

	@SuppressWarnings("unchecked")
	@Override
	public void handleRequest(SocketChannel client) {

		EncryptedServerSocket enSocket = null;
		try {
			enSocket = enSockFact.getEncryptedServerSocket( client.socket(), rh.getTrustedKeys() );
			NodeInfo clientNode = connectedClients.containsKey( enSocket.getClientPublicKey() )
					? connectedClients.get( enSocket.getClientPublicKey() )
					: null;
			
loop:		while(true){

				Request req =null;
				try{
					req = enSocket.getInputStream().readEnum( Request.class );
				}catch(IOException e){
					break;
				}
	
				String clientName = NodeInfo.getNickname( enSocket.getClientPublicKey() );
				String timestamp = new Timestamp( new Date().getTime() ).toString();
				System.out.println(timestamp+" "+req+" da "+clientName);
				
				switch(req) {
				
					case LOGIN:
						
						if( clientNode != null ){
							enSocket.getOutputStream().write( Response.ALREADY_CONNECTED );
						}else{
							int port = enSocket.getInputStream().readInt();
							InetSocketAddress isa = new InetSocketAddress( enSocket.getRemoteAddress(), port);
							clientNode = new NodeInfo( enSocket.getClientPublicKey(), isa, false );
							enSocket.getInputStream().checkDigest();
							
							connectedClients.put( enSocket.getClientPublicKey(), clientNode );
							enSocket.getOutputStream().write( Response.OK );
							enSocket.getOutputStream().write( enSocket.getRemoteAddress().getAddress() );
							enSocket.getOutputStream().sendDigest();
						}
						enSocket.getOutputStream().flush();
		
						break;
		
					case PUBLISH:
						
						try {
							Set<SharedFile> list = enSocket.getInputStream()
									.readObject( Set.class );
							enSocket.getInputStream().checkDigest();
							
							for( SharedFile sf : list ){
								
								if( ! files.contains( sf ) ){
									RemoteSharedFile rsf = new RemoteSharedFile(
											sf.getHash(), sf.getFileNames(), sf.getSize(), clientNode );
									files.add( rsf );
								}else{
									files.get( files.indexOf( sf ) ).addPeer( clientNode, sf.getFileNames() );
								}
							}
							
							enSocket.getOutputStream().write( Response.OK );
						} catch (Exception e) {
							e.printStackTrace();
							enSocket.getOutputStream().write( Response.FAIL );
						}
						enSocket.getOutputStream().sendDigest();
						enSocket.getOutputStream().flush();
						break;
						
					case LEAVE:
						
						if( clientNode != null ){
							
							Set<RemoteSharedFile> toDelete = new HashSet<RemoteSharedFile>();
							for( RemoteSharedFile rsf : files ){
								if( rsf.getPeers().contains( clientNode ) ){
									rsf.removePeer( clientNode );
									if( ! rsf.hasPeers() )
										toDelete.add( rsf );
								}
							}
							for( RemoteSharedFile rsf : toDelete ){
								files.remove( rsf );
							}
							
							connectedClients.remove( clientNode.getPublicKey() );
							enSocket.getOutputStream().write( Response.OK );
						}else{
							enSocket.getOutputStream().write(Response.NOT_CONNECTED);
						}
						enSocket.getOutputStream().flush();
						break;
						
					case UNPUBLISH:
						
						if( clientNode != null ){
							
							Set<RemoteSharedFile> list = enSocket.getInputStream()
									.readObject( Set.class );
							enSocket.getInputStream().checkDigest();
							
							for( SharedFile sf : list ){
								if( ! files.contains( sf ) ){
									// weird :-/
									continue;
								}
								RemoteSharedFile rsf = files.get( files.indexOf( sf ) );
								rsf.removePeer( clientNode );
								if(!rsf.hasPeers())
									files.remove( rsf );
							}
							enSocket.getOutputStream().write( Response.OK );
							enSocket.getOutputStream().flush();
							
						}else{
							enSocket.getOutputStream().write( Response.NOT_CONNECTED );
							enSocket.getOutputStream().flush();
						}
						break;
					
					case SEARCH:
						
						String query = new String( enSocket.getInputStream().readVariableSize(), "utf-8" );
						enSocket.getInputStream().checkDigest();
						
						//search in local files
						List<RemoteSharedFile> toSend = SearchHandler.localSearch(query, files);
						
						// forward query to other supernodes
						for( NodeInfo supernode : rh.getSupernodeList() ){
							try{
								// avoid myself ;)
								if( this.isRepresentedBy( supernode ) )
									continue;
								
								//connect to the other supernode
								EncryptedClientSocket ecs = enSockFact.getEncryptedClientSocket( 
										supernode.getAddress(), supernode.getPublicKey() );
								
								//forward
								ecs.getOutputStream().write( Request.FORWARD_SEARCH );
								ecs.getOutputStream().writeVariableSize( query.getBytes("utf-8") );
								ecs.getOutputStream().sendDigest();
								
								//read response
								Response reply = ecs.getInputStream().readEnum( Response.class );
								if( reply.equals( Response.OK ) ){
									List<RemoteSharedFile> result = ecs.getInputStream().readObject( List.class );
									ecs.getInputStream().checkDigest();
									
									toSend = SearchHandler.mergeLists(toSend, result);
								}
							}catch(IOException e){
								//do nothing
							}
						}
						
						toSend = SearchHandler.filterOutNode( toSend, clientNode );
						
						//send back to the client
						enSocket.getOutputStream().write( Response.OK );
						enSocket.getOutputStream().writeVariableSize( toSend );
						enSocket.getOutputStream().sendDigest();
						enSocket.getOutputStream().flush();
						break;
					
					case SEARCH_BY_HASH:
						
						byte[] hash = enSocket.getInputStream().readFixedSizeAsByteArray( 20 );
						enSocket.getInputStream().checkDigest();
						
						RemoteSharedFile fileToSend = SearchHandler.localSearchByHash( hash, files );
						
						// forward query to other supernodes
						for( NodeInfo supernode : rh.getSupernodeList() ){
							try{
								// avoid myself ;)
								if( this.isRepresentedBy( supernode ) )
									continue;
								
								//connect to the other supernode
								EncryptedClientSocket ecs = enSockFact.getEncryptedClientSocket( 
										supernode.getAddress(), supernode.getPublicKey() );
								
								//forward
								ecs.getOutputStream().write( Request.FORWARD_SEARCH_BY_HASH );
								ecs.getOutputStream().write( hash );
								ecs.getOutputStream().sendDigest();
								
								//read response
								Response reply = ecs.getInputStream().readEnum( Response.class );
								if( reply.equals( Response.OK ) ){
									RemoteSharedFile result = ecs.getInputStream().readObject( RemoteSharedFile.class );
									ecs.getInputStream().checkDigest();
									if( fileToSend == null )
										fileToSend = result;
									else if( result != null ){
										fileToSend.merge( result );
									}
								}
							}catch(IOException e){
								// do nothing
							}
						}
						
						fileToSend.removePeer( clientNode );
						
						//send back to the client
						enSocket.getOutputStream().write( Response.OK );
						enSocket.getOutputStream().writeVariableSize( fileToSend );
						enSocket.getOutputStream().sendDigest();
						enSocket.getOutputStream().flush();
						break;
					
					case FORWARD_SEARCH:
						
						//read query
						String forwardQuery = new String( enSocket.getInputStream().readVariableSize(), "utf-8" );
						enSocket.getInputStream().checkDigest();
						
						//search in local files
						List<RemoteSharedFile> replyList = SearchHandler.localSearch(forwardQuery, files);
						
						//reply
						enSocket.getOutputStream().write( Response.OK );
						enSocket.getOutputStream().writeVariableSize( replyList );
						enSocket.getOutputStream().sendDigest();
						enSocket.getOutputStream().flush();
						break;
						
					case FORWARD_SEARCH_BY_HASH:
						
						//read query
						hash = enSocket.getInputStream().readFixedSizeAsByteArray( 20 );
						enSocket.getInputStream().checkDigest();
						
						//search in local files
						RemoteSharedFile fileReply = SearchHandler.localSearchByHash( hash, files );
						
						//reply
						enSocket.getOutputStream().write( Response.OK );
						enSocket.getOutputStream().writeVariableSize( fileReply );
						enSocket.getOutputStream().sendDigest();
						enSocket.getOutputStream().flush();
						break;
						
					case OPEN_COMMUNICATION: // params: NodeInfo, SharedFile
					{
						NodeInfo node = enSocket.getInputStream().readObject( NodeInfo.class );
						SharedFile file = enSocket.getInputStream().readObject( SharedFile.class );
						enSocket.getInputStream().checkDigest();
						
						EncryptedClientSocket comm = enSockFact.getEncryptedClientSocket( node.getAddress(), node.getPublicKey() );
						comm.getOutputStream().write( Request.ADD_TRUSTED_DOWNLOAD );
						comm.getOutputStream().writeVariableSize( clientNode );
						comm.getOutputStream().writeVariableSize( file );
						comm.getOutputStream().sendDigest();
						
						Response reply = comm.getInputStream().readEnum( Response.class );
						comm.getInputStream().checkDigest();
						comm.close();
						enSocket.getOutputStream().write( reply );
						enSocket.getOutputStream().sendDigest();
						enSocket.getOutputStream().flush();
						
						break;
					}
					
					case PING:
					{
						enSocket.getOutputStream().write( Response.PONG );
						enSocket.getOutputStream().flush();
					}
					default:
						
						enSocket.getOutputStream().write( Response.FAIL );
	
					case CLOSE_CONN:
						
						break loop;
				}
			
			}
			enSocket.close();
		}catch(IOException e){
			//e.printStackTrace();
			if( enSocket != null )
				enSocket.close();
		}catch(GeneralSecurityException e){
			//e.printStackTrace();
			if( enSocket != null )
				enSocket.close();
		} catch (ClassNotFoundException e) {
			//e.printStackTrace();
		}
	}


	@Override
	public InetSocketAddress getSocketAddress() {
		return null;
	}

}
