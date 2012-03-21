package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import javax.crypto.SecretKey;

import polimi.distsys.sp2p.containers.Message;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.Message.Request;
import polimi.distsys.sp2p.containers.Message.Response;
import polimi.distsys.sp2p.handlers.SecurityHandler;
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
	private final String CREDENTIALS_FILE = "credentials.list";
	// struttura dati in cui vengono salvate le credenziali dei nodi ( public keys )
	private final ArrayList<PublicKey> credentials;

	//file da cui recuperare le informazioni del nodo
	private final String infoFile = "supernode.info";

	private final PrivateKey privateKey;
	private final PublicKey publicKey;	
	//il socket usato sia dal nodo che dal supernodo per ricevere le connessioni
	private final ServerSocket socket;

	@SuppressWarnings("unused")
	private final Listener listener;
	


	public SuperNode() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		//inizializza il routerHandler
		super();

		//legge il file per recuperare chiave pubblica, privata e porta del nodo
		InputStream is = new FileInputStream(infoFile);

		Scanner sc = new Scanner(is);
		String[] tmp = sc.nextLine().split(":");
		publicKey = Serializer.deserialize(
				Serializer.base64Decode(tmp[0]), 
				PublicKey.class);
		privateKey = Serializer.deserialize(
				Serializer.base64Decode(tmp[1]), 
				PrivateKey.class);
		socket = PortChecker.getBoundedServerSocketChannelOrNull(Integer.parseInt(tmp[2])).socket();
		
		sc.close();
		is.close();
		
		credentials = new ArrayList<PublicKey>();
		
		//legge le credenziali
		is = new FileInputStream(CREDENTIALS_FILE);
		sc = new Scanner(is);
		while(sc.hasNext()) {
			
			PublicKey tmpKey = Serializer.deserialize(
					Serializer.base64Decode(sc.nextLine()), 
					PublicKey.class);
			credentials.add(tmpKey);
		}
		
		//inizializza il listenere
		listener = new Listener(socket.getLocalPort(), this);
		

	}	


	//ALTRIMETODI
	
	private class ConnectionHandler extends Thread {

		private final Socket socket;
		private final NodeInfo remote;

		private ConnectionHandler(final Socket conn) throws GeneralSecurityException{
			socket = conn;
			remote = SuperNode.this.rh.getNodeInfoBySocketAddress(
					(InetSocketAddress)conn.getRemoteSocketAddress()
					);
			/*			if(remote == null)
				throw new GeneralSecurityException("Unauthorized connection");
			sharedKey = rh.getSharedKey(remote);
			 */		}

		@Override
		public void start(){

			InputStream ois = null;
			OutputStream oos = null;

			try {

				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());

				while (true) {

					try{
						Message msg = (Message) ois.readObject();
						Request req = (Request) msg.getAction();

						switch(req) {
						case LOGIN:

							//TODO check if we already know the public key
							// corresponding to the remote node or not

							// save the public key in the shared keys


							/*if( !credentials.containsKey(remote.getPublicKey()) ){
								msg = SuperNode.this.createMessage(
										Response.FAIL, 
										"Remote client is not one of my clients".getBytes(), 
										remote
								);
								break;
							}*/

							byte[] hashedPassword = msg.decryptPayload(sharedKey);
							if( ! Arrays.equals( credentials.get(remote.getPublicKey()), hashedPassword) ){
								msg = SuperNode.this.createMessage(
										Response.FAIL, 
										"Wrong password".getBytes(), 
										remote
										);
								break;
							}

							msg = SuperNode.this.createMessage(Response.OK, "OK".getBytes(), remote);
							break;

						default:

							msg = SuperNode.this.createMessage(Response.FAIL, "Bad Request".getBytes(), remote);
						}

						oos.writeObject(msg);
						oos.flush();
					}catch(ClassNotFoundException cnfe){
						cnfe.printStackTrace();
					} catch (GeneralSecurityException e) {
						e.printStackTrace();
					}

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				if(oos != null)
					if(!socket.isOutputShutdown())
						try {
							oos.close();
						} catch (IOException e) {
						}
				if(ois != null)
					if(!socket.isInputShutdown())
						try {
							ois.close();
						} catch (IOException e) {
						}
				if(!socket.isClosed())
					try {
						socket.close();
					} catch (IOException e) {
					}

			}

		}

	}

	@Override
	public void handleRequest(SocketChannel client) {
		try {
			new ConnectionHandler(client.socket()).start();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}


	public PrivateKey getPrivateKey() {
		return privateKey;
	}


	public PublicKey getPublicKey() {
		return publicKey;
	}


	@Override
	public ServerSocket getSocketAddress() {
		// TODO Auto-generated method stub
		return null;
	}
}
