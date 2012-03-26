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
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedServerSocket;
import polimi.distsys.sp2p.util.Listener;
import polimi.distsys.sp2p.util.PortChecker;
import polimi.distsys.sp2p.util.Serializer;
import polimi.distsys.sp2p.util.Listener.ListenerCallback;


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
		}
		sc.close();

		//inizializza il listener
		listener = new Listener(socket.getChannel(), this);

	}	


	//ALTRIMETODI

	@SuppressWarnings("unchecked")
	@Override
	public void handleRequest(SocketChannel client) {

		try {
			EncryptedServerSocket enSocket = enSockFact.getEncryptedServerSocket(
					client.socket(), credentials);
			NodeInfo clientNode = connectedClients.containsKey( enSocket.getClientPublicKey() )
					? connectedClients.get( enSocket.getClientPublicKey() )
					: null;
			
			while(true){

				Request req = enSocket.getInputStream().readEnum( Request.class );
	
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
							Set<RemoteSharedFile> list = enSocket.getInputStream()
									.readObject( Set.class );
							enSocket.getInputStream().checkDigest();
							
							for( SharedFile sf : list ){
								String filename = sf.getFileNames().iterator().next();
								
								if( ! files.contains( sf ) ){
									RemoteSharedFile rsf = new RemoteSharedFile(
											sf.getHash(), filename, clientNode );
									files.add( rsf );
								}else{
									files.get( files.indexOf( sf ) ).addPeer( filename, clientNode );
								}
							}
							
							enSocket.getOutputStream().write( Response.OK );
							enSocket.getOutputStream().flush();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						break;
						
					case LEAVE:
						
						if( clientNode != null ){
							//TODO unpublish shared files
							connectedClients.remove( clientNode );
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
								String filename = sf.getFileNames().iterator().next();
								RemoteSharedFile rsf = new RemoteSharedFile(
										sf.getHash(), filename, clientNode );
								if( ! files.contains( rsf ) ){
									// weird :-/
									continue;
								}
								files.get( files.indexOf( rsf ) ).removePeer( clientNode );
							}
							enSocket.getOutputStream().write( Response.OK );
							enSocket.getOutputStream().flush();
							
						}
						
					default:
						
						enSocket.getOutputStream().write( Response.FAIL );
	
					case CLOSE_CONN:
						
						enSocket.close();
				}
			
			}
		}catch(IOException e){
			e.printStackTrace();
		}catch(GeneralSecurityException e){
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Override
	public InetSocketAddress getSocketAddress() {
		return null;
	}

}
