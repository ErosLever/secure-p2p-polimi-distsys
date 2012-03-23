package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import polimi.distsys.sp2p.containers.NodeInfo;
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
	
	private final Listener listener;

	public static SuperNode fromFile() throws IOException, ClassNotFoundException, NoSuchAlgorithmException{
		return fromFile( new File( infoFile ) );
	}

	public static SuperNode fromFile( File file ) throws IOException, ClassNotFoundException, NoSuchAlgorithmException{
		return fromFile( file, new File( CREDENTIALS_FILE ) );
	}
	
	public static SuperNode fromFile( File file, File credentials) throws IOException, ClassNotFoundException, NoSuchAlgorithmException{
		//legge il file per recuperare chiave pubblica, privata e porta del nodo
		Scanner sc = new Scanner( new FileInputStream( file ) );
		String[] tmp = sc.nextLine().split(":");
		PublicKey pub = Serializer.deserialize(
				Serializer.base64Decode(tmp[0]), 
				PublicKey.class);
		PrivateKey priv = Serializer.deserialize(
				Serializer.base64Decode(tmp[1]), 
				PrivateKey.class);
		ServerSocket socket = PortChecker.getBoundedServerSocketChannelOrNull(
				Integer.parseInt(tmp[2])).socket();
		sc.close();
		return new SuperNode(pub, priv, socket,  credentials );
	}

	private SuperNode(PublicKey pub, PrivateKey priv, ServerSocket sock, File credentials ) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		//inizializza il routerHandler
		super( pub, priv, sock );

		this.credentials = new HashSet<PublicKey>();
		this.connectedClients = new HashSet<NodeInfo>();

		//legge le credenziali
		Scanner sc = new Scanner( new FileInputStream( credentials ) );
		while(sc.hasNext()) {

			PublicKey tmpKey = Serializer.deserialize(
					Serializer.base64Decode(sc.nextLine()), 
					PublicKey.class);
			this.credentials.add( tmpKey );
		}
		sc.close();

		//inizializza il listener
		listener = new Listener(socket.getChannel(), this);

	}	


	//ALTRIMETODI

	@Override
	public void handleRequest(SocketChannel client) {

		try {

			EncryptedServerSocket enSocket = enSockFact.getEncryptedServerSocket(client.socket(), credentials);

			Request req = enSocket.getInputStream().readEnum( Request.class );

			switch(req) {
				case LOGIN:
					int port = enSocket.getInputStream().readInt();
					enSocket.getInputStream().checkDigest();
					
					InetSocketAddress isa = new InetSocketAddress( 
							enSocket.getRemoteAddress(), port);
					
					NodeInfo clientNode = new NodeInfo( 
							enSocket.getClientPublicKey(), isa, false );
					if( connectedClients.contains( clientNode ) ){
						enSocket.getOutputStream().write( Response.ALREADY_CONNECTED );
					}else{
						connectedClients.add( clientNode );
						enSocket.getOutputStream().write( Response.OK );
					}
					enSocket.getOutputStream().flush();
	
					break;
	
				default:
					enSocket.getOutputStream().write( Response.FAIL );
			}
		}catch(IOException e){
			e.printStackTrace();
		}catch(GeneralSecurityException e){
			e.printStackTrace();
		}
	}

}
