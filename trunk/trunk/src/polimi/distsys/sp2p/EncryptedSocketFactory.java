package polimi.distsys.sp2p;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import polimi.distsys.sp2p.util.PortChecker;
import polimi.distsys.sp2p.util.Serializer;

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
	
	/**
	 * Ciphering configuration
	 */
	private static final int SYMM_KEY_SIZE = 128/8;
	private static final String SYMM_ALGO = "AES";
	
	private static final int ASYMM_KEY_SIZE = 1024/8;
	private static final String ASYMM_ALGO = "RSA";
	
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
		protected final InputStream inputStream;
		protected final OutputStream outputStream;
		
		protected EncryptedSocket(Socket sock, E arg) throws GeneralSecurityException, IOException{
			socket = sock;
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
		
		protected InputStream initInputStream() throws GeneralSecurityException, IOException {
			Cipher cipher = Cipher.getInstance(SYMM_ALGO);
			cipher.init(Cipher.DECRYPT_MODE, sessionKey);
			return new CipherInputStream(socket.getInputStream(), cipher);
		}
		
		protected OutputStream initOutputStream() throws GeneralSecurityException, IOException {
			Cipher cipher = Cipher.getInstance(SYMM_ALGO);
			cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
			return new CipherOutputStream(socket.getOutputStream(), cipher);
		}
		
		public InputStream getInputStream(){
			return inputStream;
		}
		
		public OutputStream getOutputStream(){
			return outputStream;
		}
		
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
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
			super( new Socket(isa.getAddress(), isa.getPort()), hisPub);
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
			
			Cipher cipher = Cipher.getInstance(ASYMM_ALGO);
			byte[] encodedKey = myPub.getEncoded();
			
			/* encodedKey would normally be ASYMM_KEY_SIZE bytes
			 * but asymmetric encryption supports at maximum
			 * (ASYMM_KEY_SIZE - 11) so we have to split the encryption
			 * in two phases. 
			 */
			int count = 0, blockSize = ASYMM_KEY_SIZE -11;
			while(count + blockSize < encodedKey.length){
				cipher.init(Cipher.ENCRYPT_MODE, hisPub);
				socket.getOutputStream().write(
					cipher.doFinal(encodedKey, count, blockSize)	
				);
				count+=blockSize;
			}
			cipher.init(Cipher.ENCRYPT_MODE, hisPub);
			socket.getOutputStream().write(
				cipher.doFinal(encodedKey, count, encodedKey.length - count)	
			);
			socket.getOutputStream().flush();
			
			/* local PublicKey sent encrypted with remote PublicKey
			 * now let's prepare to receive the session key
			 */
			
			/* Asymmetric encryption allow us to decrypt at most
			 * ASYMM_KEY_SIZE bytes, but we will receive two blocks
			 */
			
			/* ByteArrayOutputStream is needed because we can't know
			 * in advance what will be the decrypted size
			 */
			
			byte[] received = new byte[ASYMM_KEY_SIZE];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			Cipher cipher1 = Cipher.getInstance(ASYMM_ALGO);
			cipher1.init(Cipher.DECRYPT_MODE, myPriv);
			
			for(int i=0;i<2;i++){
				count = 0;
				while(count < ASYMM_KEY_SIZE)
					count += socket.getInputStream().read(received, count, ASYMM_KEY_SIZE - count);
				cipher.init(Cipher.DECRYPT_MODE, myPriv);
				baos.write( cipher.doFinal(received) );
			}
			
			received = baos.toByteArray();
			
			// decrypted with the local PrivateKey
			
			Cipher cipher2 = Cipher.getInstance(ASYMM_ALGO);
			cipher2.init(Cipher.DECRYPT_MODE, hisPub);
			received = cipher2.doFinal(received);
			
			// decrypted with the remote PublicKey
			
			SecretKeySpec sessionKey = new SecretKeySpec(received, SYMM_ALGO);
			// Parsed SecretKey from byte array
			
			return sessionKey;
		}
		
		public PublicKey getRemotePublicKey(){
			return hisPub;
		}
		
	}
	
	public class EncryptedServerSocket extends EncryptedSocket<Set<PublicKey>> {

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
			
			/* Let's first decrypt the remote (client) PublicKey
			 * which is encrypted with our PubliKey
			 */
			Cipher cipher = Cipher.getInstance(ASYMM_ALGO);
			byte[] received = new byte[ASYMM_KEY_SIZE];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			for(int i=0;i<2;i++){
				int count = 0;
				while(count < ASYMM_KEY_SIZE)
					count += socket.getInputStream().read(received, count, ASYMM_KEY_SIZE - count);
				cipher.init(Cipher.DECRYPT_MODE, myPriv);
				baos.write( cipher.doFinal(received) );
			}
			
			received = baos.toByteArray();
			
			// decryped the remote PublicKey
			
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(received);
			PublicKey hisPub = KeyFactory.getInstance(ASYMM_ALGO).generatePublic(pubKeySpec);
			
			// Parsed the remote PublicKey from the byte array
			
			// check if it's a valid PublicKey (allowed to communicate with us)
			if( ! pubKeyList.contains( hisPub )){
				throw new GeneralSecurityException("Unknown PublicKey supplied by client");
			}
			
			// generate a session key
			KeyGenerator kgen = KeyGenerator.getInstance(SYMM_ALGO);
			kgen.init( SYMM_KEY_SIZE * 8 );
			SecretKey sessionKey = kgen.generateKey();
			
			byte[] encodedKey = sessionKey.getEncoded();
			
			// encrypt it with local PrivateKey
			
			Cipher cipher1 = Cipher.getInstance(ASYMM_ALGO);
			cipher1.init(Cipher.ENCRYPT_MODE, myPriv);
			encodedKey = cipher1.doFinal(encodedKey);
			
			// encrypt with remote PublicKey
			
			Cipher cipher2 = Cipher.getInstance(ASYMM_ALGO);
			int blockSize = ASYMM_KEY_SIZE - 11;
			for(int i=0;i<2;i++){
				cipher2.init(Cipher.ENCRYPT_MODE, hisPub);
				int bytes = Math.min(blockSize, encodedKey.length - blockSize*i);
				byte[] toWrite = cipher2.doFinal(encodedKey, blockSize*i, bytes);
				socket.getOutputStream().write(toWrite);
			}
			socket.getOutputStream().flush();
			
			// session key sent to the client
			
			return sessionKey;
		}
		
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException{
		
		class SuperNodeThread extends Thread{
			private final Set<PublicKey> pklist;
			private final ServerSocket ss;
			private final EncryptedSocketFactory esf;
			
			
			public SuperNodeThread(EncryptedSocketFactory esf, Set<PublicKey> pklist) throws NoSuchAlgorithmException{
				this.esf = esf;
				this.pklist = pklist;
				this.ss = PortChecker.getBoundedServerSocketChannel().socket();
			}
			
			public void run(){
				
				try{
					//while(true){
						
						Socket s = ss.accept();
						
						EncryptedServerSocket ess = esf.getEncryptedServerSocket(s, pklist);
						
						byte[] tmp = new byte[4];
						int read = ess.getInputStream().read(tmp,0,4);
						
						System.out.println(read);
						System.out.println(Serializer.byteArrayToHexString(tmp));
						System.out.println(new String(tmp));
						ess.close();
						
					//}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			
			public InetSocketAddress getAddress(){
				return new InetSocketAddress(ss.getInetAddress(), ss.getLocalPort());
			}
		}
		
		class SimpleNodeThread extends Thread{
			private final PublicKey superKey;
			private final InetSocketAddress superAddr;
			private final EncryptedSocketFactory esf;
			
			
			public SimpleNodeThread(EncryptedSocketFactory esf, PublicKey superKey, InetSocketAddress isa) throws NoSuchAlgorithmException{
				this.esf = esf;
				this.superKey = superKey;
				this.superAddr = isa;
			}
			
			public void run(){
				
				try{
						
					EncryptedClientSocket ecs = esf.getEncryptedClientSocket(superAddr, superKey);
					ecs.getOutputStream().write("ciao".getBytes(),0,4);
					ecs.close();
						
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASYMM_ALGO);
		kpg.initialize(ASYMM_KEY_SIZE*8);
		KeyPair simplekp = kpg.genKeyPair();
		EncryptedSocketFactory simpleESF = new EncryptedSocketFactory(simplekp);

		kpg.initialize(ASYMM_KEY_SIZE*8);
		KeyPair superkp = kpg.genKeyPair();
		EncryptedSocketFactory superESF = new EncryptedSocketFactory(superkp);

		Set<PublicKey> pklist = new HashSet<PublicKey>();
		pklist.add(simplekp.getPublic());
		SuperNodeThread superN = new SuperNodeThread(superESF, pklist);
		superN.start();
		
		SimpleNodeThread simpleN = new SimpleNodeThread(simpleESF, superkp.getPublic(), superN.getAddress());
		simpleN.start();
		
	}

}
