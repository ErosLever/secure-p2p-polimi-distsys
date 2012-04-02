package polimi.distsys.sp2p.crypto;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import polimi.distsys.sp2p.crypto.StreamCipherOutputStream.ResettableCipher;

/**
 * 
 * @author eros
 * 
 * EncryptedSocketFactory is intended to, given a KeyPair 
 * <PublicKey, PrivateKey>, use them to create EncryptedSocket
 * both as client and server.
 *
 */
public class EncryptedSocketFactory {
	

	public static final int SOCKET_TIMEOUT = 5*1000; // 5 sec
	public static final int SOCKET_CONNECT_TIMEOUT = SOCKET_TIMEOUT / 2;

	
	/**
	 * Ciphering configuration
	 */
	private static final int SYMM_KEY_SIZE = 128;
	public static final String SYMM_ALGO = "AES";
	
	private static final int ASYMM_KEY_SIZE = 1024;
	public static final String ASYMM_ALGO = "RSA";
	
	private final PrivateKey myPriv;
	private final PublicKey myPub;
	
	public EncryptedSocketFactory(final KeyPair kp){
		this(kp.getPrivate(), kp.getPublic());
	}

	public EncryptedSocketFactory(final PrivateKey myPriv, final PublicKey myPub){
		this.myPriv = myPriv;
		this.myPub = myPub;
	}
	
	public EncryptedClientSocket getEncryptedClientSocket(String host, int port, PublicKey hisPub) throws IOException, GeneralSecurityException{
		return getEncryptedClientSocket(new InetSocketAddress(host, port), hisPub);
	}
	
	public EncryptedClientSocket getEncryptedClientSocket(InetSocketAddress isa, PublicKey hisPub) throws IOException, GeneralSecurityException{
		return new EncryptedClientSocket(isa, hisPub);
	}
	
	public EncryptedServerSocket getEncryptedServerSocket(Socket sock, Set<PublicKey> allowedKeys) throws IOException, GeneralSecurityException{
		return new EncryptedServerSocket(sock, allowedKeys);
	}
	
	private abstract class EncryptedSocket<E> {
		
		protected final Socket socket;
		protected final SecretKey sessionKey;
		protected final StreamCipherInputStream inputStream;
		protected final StreamCipherOutputStream outputStream;
		
		protected EncryptedSocket(Socket sock, E arg) throws GeneralSecurityException, IOException{
			socket = sock;
			socket.setSoTimeout( SOCKET_CONNECT_TIMEOUT );
			sessionKey = handshake(arg);
			inputStream = initInputStream();
			outputStream = initOutputStream();
		}
		
		/**
		 * Since the handshake varies depending on client/server,
		 * I let it an abstract method 
		 *  
		 * @param arg
		 * @return
		 * @throws GeneralSecurityException
		 * @throws IOException
		 */
		protected abstract SecretKey handshake(E arg) throws GeneralSecurityException, IOException;
		
		protected StreamCipherInputStream initInputStream() throws GeneralSecurityException, IOException {
			List<ResettableCipher> lrc = new ArrayList<ResettableCipher>();
			lrc.add( new ResettableCipher( SYMM_ALGO, Cipher.DECRYPT_MODE, sessionKey ) );
			return new StreamCipherInputStream( socket.getInputStream(), lrc );
		}
		
		protected StreamCipherOutputStream initOutputStream() throws GeneralSecurityException, IOException {
			List<ResettableCipher> lrc = new ArrayList<ResettableCipher>();
			lrc.add( new ResettableCipher( SYMM_ALGO, Cipher.ENCRYPT_MODE, sessionKey ) );
			return new StreamCipherOutputStream( socket.getOutputStream(), lrc );
		}
		
		@SuppressWarnings("unused")
		public StreamCipherInputStream getInputStream(){
			return inputStream;
		}
		
		@SuppressWarnings("unused")
		public StreamCipherOutputStream getOutputStream(){
			return outputStream;
		}
		
		@SuppressWarnings("unused")
		public InetAddress getRemoteAddress(){
			return socket.getInetAddress();
		}
		
		@SuppressWarnings("unused")
		public void close(){
			// for some unknown reason we have to first close the output
			if(!socket.isOutputShutdown())
				try {
					outputStream.flush();
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			// close input only after the output 
			if(!socket.isInputShutdown())
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public boolean isInputShutdown(){
			return socket.isInputShutdown() || socket.isClosed();
		}
		
		public boolean isOutputShutdown(){
			return socket.isOutputShutdown() || socket.isClosed();
		}
		
		@SuppressWarnings("unused")
		public boolean isConnected(){
			return ! ( isOutputShutdown() || isInputShutdown() );
		}
		
	}
	
	private static Socket createSocketWithTimeout( InetSocketAddress isa ) throws IOException{
		Socket s = new Socket();
		s.connect( isa, SOCKET_TIMEOUT);
		return s;
	}
	
	/**
	 * Assuming we already know the PublicKey of the receiver,
	 * we need only that info to communicate
	 * 
	 * @author eros
	 *
	 */
	public class EncryptedClientSocket extends EncryptedSocket<PublicKey> {
		
		private final PublicKey hisPub;
		
		protected EncryptedClientSocket(InetSocketAddress isa, PublicKey hisPub) throws IOException, GeneralSecurityException{
			super( createSocketWithTimeout( isa ), hisPub);
			this.hisPub = hisPub;
		}
		
		/**
		 * This sends the local PublicKey asymmetrically encrypted 
		 * with the remote PublicKey, the remote server will 
		 * authenticate us with the local PublicKey, and send back 
		 * a challenge which is the SecretKey (session key for AES
		 * symmetric encryption) that is encrypted with both its
		 * PrivateKey and the local PublicKey  
		 */
		protected SecretKey handshake(PublicKey hisPub) throws GeneralSecurityException, IOException {
			
			List<ResettableCipher> lrc = new ArrayList<ResettableCipher>();
			lrc.add( new ResettableCipher( ASYMM_ALGO, Cipher.ENCRYPT_MODE, hisPub ) );
			StreamCipherOutputStream scos = new StreamCipherOutputStream( socket.getOutputStream(), lrc );
			
			scos.write( myPub.getEncoded() );
			scos.flush();
			
			lrc = new ArrayList<ResettableCipher>();
			// prima decifro con la mia privata
			lrc.add( new ResettableCipher( ASYMM_ALGO, Cipher.DECRYPT_MODE, myPriv ) );
			// poi con la sua pubblica
			lrc.add( new ResettableCipher( ASYMM_ALGO, Cipher.DECRYPT_MODE, hisPub ) );
			StreamCipherInputStream scis = new StreamCipherInputStream( socket.getInputStream(), lrc );
			
			byte[] encoded = scis.readFixedSizeAsByteArray( SYMM_KEY_SIZE / 8 );
			SecretKeySpec sessionKey = new SecretKeySpec( encoded, SYMM_ALGO );
			
			return sessionKey;
		}
		
		public PublicKey getRemotePublicKey(){
			return hisPub;
		}
		
	}
	
	public class EncryptedServerSocket extends EncryptedSocket<Set<PublicKey>> {

		private PublicKey clientKey;
		
		protected EncryptedServerSocket(Socket sock, Set<PublicKey> pubKeyList) throws IOException, GeneralSecurityException{
			super( sock , pubKeyList);
		}
		
		/**
		 * Assuming we already know the PublicKey of the possible receivers,
		 * we need to discover who is talking with us, to do that we will check
		 * the received PublicKey if it is within the allowed PublicKeys 
		 * 
		 */
		@Override
		protected SecretKey handshake(Set<PublicKey> pubKeyList) throws GeneralSecurityException, IOException{
			
			List<ResettableCipher> lrc = new ArrayList<ResettableCipher>();
			lrc.add( new ResettableCipher( ASYMM_ALGO, Cipher.DECRYPT_MODE, myPriv ) );
			StreamCipherInputStream scis = new StreamCipherInputStream( socket.getInputStream(), lrc );
			
			byte[] encoded = scis.readFixedSizeAsByteArray( ASYMM_KEY_SIZE / 8 , false);
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec( encoded );
			PublicKey hisPub = KeyFactory.getInstance( ASYMM_ALGO ).generatePublic( pubKeySpec );
			
			// check if it's a valid PublicKey (allowed to communicate with us)
			if( ! pubKeyList.contains( hisPub )){
				throw new GeneralSecurityException("Unknown PublicKey supplied by client");
			}
			
			clientKey = hisPub;
			
			// generate a session key
			KeyGenerator kgen = KeyGenerator.getInstance(SYMM_ALGO);
			kgen.init( SYMM_KEY_SIZE );
			SecretKey sessionKey = kgen.generateKey();
			
			lrc = new ArrayList<ResettableCipher>();
			// prima cifro con la mia privata
			lrc.add( new ResettableCipher( ASYMM_ALGO, Cipher.ENCRYPT_MODE, myPriv ) );
			// poi con la sua pubblica
			lrc.add( new ResettableCipher( ASYMM_ALGO, Cipher.ENCRYPT_MODE, hisPub ) );
			StreamCipherOutputStream scos = new StreamCipherOutputStream( socket.getOutputStream(), lrc );
			
			scos.write( sessionKey.getEncoded() );
			scos.flush();
			
			return sessionKey;
		}
		
		public PublicKey getClientPublicKey(){
			return clientKey;
		}
		
	}
	
}