package polimi.distsys.sp2p;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.SecretKey;

import polimi.distsys.sp2p.Message.Request;
import polimi.distsys.sp2p.Message.Response;
import polimi.distsys.sp2p.util.Listener;
import polimi.distsys.sp2p.util.Listener.ListenerCallback;


/**
 * 
 */

/**
 * @author Ale
 *
 */
public class SuperNode extends Node implements ListenerCallback {
	
	@SuppressWarnings("unused")
	private final Listener listener;
	private Map<NodeInfo,byte[]> credentials = new TreeMap<NodeInfo, byte[]>();
	
	
	public SuperNode() throws IOException {
		
		this( getRandomPort() );

	}
	
	public SuperNode(final int port) throws IOException {
		this(port, SecurityHandler.getKeypair());
	}	
	
	public SuperNode(final int port, final KeyPair kp) throws IOException{
		this(port, kp.getPublic(), kp.getPrivate());
	}
	
	public SuperNode(final int port, final PublicKey pub, final PrivateKey priv) throws IOException{
		super(port, pub, priv);
		listener = new Listener(port, this);
	}

	private class ConnectionHandler extends Thread {
		
		private final Socket socket;
		private final NodeInfo remote;
		private final SecretKey sharedKey;
		
		private ConnectionHandler(final Socket conn) throws GeneralSecurityException{
			socket = conn;
			remote = SuperNode.this.rh.getNodeInfoBySocketAddress(
				(InetSocketAddress)conn.getRemoteSocketAddress()
			);
			if(remote == null)
				throw new GeneralSecurityException("Unauthorized connection");
			sharedKey = rh.getSharedKey(remote);
		}
		
		@Override
		public void start(){
			
			ObjectInputStream ois = null;
			ObjectOutputStream oos = null;
			
			try {
				
				ois = new ObjectInputStream(socket.getInputStream());
				oos = new ObjectOutputStream(socket.getOutputStream());
				
				while (true) {
	
					try{
						Message msg = (Message) ois.readObject();
						Request req = (Request) msg.getAction();
		
						switch(req) {
						case LOGIN:
		
							if( !credentials.containsKey(remote) ){
								msg = SuperNode.this.createMessage(
										Response.FAIL, 
										"Remote client is not one of my clients".getBytes(), 
										remote
								);
								break;
							}
							
							byte[] hashedPassword = msg.decryptPayload(sharedKey);
							if( ! Arrays.equals( credentials.get(remote), hashedPassword) ){
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
}