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
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
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
	
	private final Set<NodeInfo> connectedClients;
	
	private final Map<String, Set<RemoteSharedFile>> files; 
	
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
		this.connectedClients = new HashSet<NodeInfo>();
		this.files = new HashMap<String, Set<RemoteSharedFile>>();

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
			EncryptedServerSocket enSocket = enSockFact.getEncryptedServerSocket(client.socket(), credentials);
			
			while(true){

				Request req = enSocket.getInputStream().readEnum( Request.class );
				int port = enSocket.getInputStream().readInt();
				InetSocketAddress isa = new InetSocketAddress( enSocket.getRemoteAddress(), port);
				NodeInfo clientNode = new NodeInfo( enSocket.getClientPublicKey(), isa, false );
	
				switch(req) {
				
					case LOGIN:
						enSocket.getInputStream().checkDigest();
						
						if( connectedClients.contains( clientNode ) ){
							enSocket.getOutputStream().write( Response.ALREADY_CONNECTED );
						}else{
							connectedClients.add( clientNode );
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
							
							for( RemoteSharedFile rsf : list ){
								String id = Serializer.byteArrayToHexString( rsf.getHash() );
								if( ! files.containsKey( id ) ){
									files.put( id, new HashSet<RemoteSharedFile>() );
								}
								if( ! files.get( id ).contains( rsf ) ){
									files.get( id ).add( rsf );
								}
							}
							
							enSocket.getOutputStream().write( Response.OK );
							enSocket.getOutputStream().flush();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}
						break;
						
					case LEAVE:
						
						if( connectedClients.contains( clientNode ) ){
							connectedClients.remove( clientNode );
							enSocket.getOutputStream().write( Response.OK );
						}else{
							enSocket.getOutputStream().write(Response.NOT_CONNECTED);
						}
						enSocket.getOutputStream().flush();
						break;
						
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
		}
	}

	@Override
	public InetSocketAddress getSocketAddress() {
		return null;
	}

}
