package polimi.distsys.sp2p;

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
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import polimi.distsys.sp2p.util.PortChecker;

public class EncryptedSocketFactory {
	
	private static final int SYMM_KEY_SIZE = 128/8;
	private static final String SYMM_ALGO = "AES";
	
	private static final int ASYMM_KEY_SIZE = 512/8;
	private static final String ASYMM_ALGO = "DSA";
	
	private final PrivateKey myPriv;
	private final PublicKey myPub;
	
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
		
		protected abstract SecretKey handshake(E arg) throws GeneralSecurityException, IOException;
		
		protected InputStream initInputStream() throws GeneralSecurityException, IOException {
			Cipher cipher = Cipher.getInstance(SYMM_ALGO);
			cipher.init(Cipher.ENCRYPT_MODE, sessionKey);
			return new CipherInputStream(socket.getInputStream(), cipher);
		}
		
		protected OutputStream initOutputStream() throws GeneralSecurityException, IOException {
			Cipher cipher = Cipher.getInstance(SYMM_ALGO);
			cipher.init(Cipher.DECRYPT_MODE, sessionKey);
			return new CipherOutputStream(socket.getOutputStream(), cipher);
		}
		
		public InputStream getInputStream(){
			return inputStream;
		}
		
		public OutputStream getOutputStream(){
			return outputStream;
		}
		
	}
	
	public class EncryptedClientSocket extends EncryptedSocket<PublicKey> {
		
		private final PublicKey hisPub;
		
		protected EncryptedClientSocket(InetSocketAddress isa, PublicKey hisPub) throws IOException, GeneralSecurityException{
			super( new Socket(isa.getAddress(), isa.getPort()), hisPub);
			this.hisPub = hisPub;
		}
		
		protected SecretKey handshake(PublicKey hisPub) throws GeneralSecurityException, IOException {
			
			Cipher cipher = Cipher.getInstance(ASYMM_ALGO);
			cipher.init(Cipher.ENCRYPT_MODE, hisPub);
			CipherOutputStream cos = new CipherOutputStream(socket.getOutputStream(), cipher);
			
			Cipher cipher1 = Cipher.getInstance(ASYMM_ALGO);
			cipher1.init(Cipher.DECRYPT_MODE, myPriv);
			CipherInputStream cis1 = new CipherInputStream(socket.getInputStream(), cipher1);
			
			Cipher cipher2 = Cipher.getInstance(ASYMM_ALGO);
			cipher2.init(Cipher.DECRYPT_MODE, hisPub);
			CipherInputStream cis = new CipherInputStream(cis1, cipher1);
			
			cos.write(myPub.getEncoded());
			cos.flush();
			
			byte[] sessionKey = new byte[SYMM_KEY_SIZE];
			int count = 0;
			while(count < SYMM_KEY_SIZE)
				cis.read(sessionKey, count, SYMM_KEY_SIZE - count);
			
			SecretKeySpec sks = new SecretKeySpec(sessionKey, SYMM_ALGO); 
			
			return sks;
		}
		
		public PublicKey getRemotePublicKey(){
			return hisPub;
		}
		
	}
	
	public class EncryptedServerSocket extends EncryptedSocket<Set<PublicKey>> {

		protected EncryptedServerSocket(Socket sock, Set<PublicKey> pubKeyList) throws IOException, GeneralSecurityException{
			super( sock , pubKeyList);
		}
		
		@Override
		protected SecretKey handshake(Set<PublicKey> pubKeyList) throws GeneralSecurityException, IOException{
			
			Cipher cipher = Cipher.getInstance(ASYMM_ALGO);
			cipher.init(Cipher.DECRYPT_MODE, myPriv);
			CipherInputStream cis = new CipherInputStream(socket.getInputStream(), cipher);
			
			byte[] hisKey = new byte[ASYMM_KEY_SIZE];
			int count = 0;
			while(count < ASYMM_KEY_SIZE)
				cis.read(hisKey, count, ASYMM_KEY_SIZE - count);
			
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(hisKey);
			PublicKey hisPub = KeyFactory.getInstance(ASYMM_ALGO).generatePublic(pubKeySpec);

			if( ! pubKeyList.contains( hisPub )){
				throw new GeneralSecurityException("Unknown PublicKey supplied by client");
			}
			
			KeyGenerator kgen = KeyGenerator.getInstance(SYMM_ALGO);
			kgen.init( SYMM_KEY_SIZE * 8 );
			SecretKey sessionKey = kgen.generateKey();
			
			Cipher cipher1 = Cipher.getInstance(ASYMM_ALGO);
			cipher1.init(Cipher.ENCRYPT_MODE, myPriv);
			CipherOutputStream cos1 = new CipherOutputStream(socket.getOutputStream(), cipher1);
			
			Cipher cipher2 = Cipher.getInstance(ASYMM_ALGO);
			cipher1.init(Cipher.ENCRYPT_MODE, hisPub);
			CipherOutputStream cos = new CipherOutputStream(cos1, cipher2);
			
			cos.write( sessionKey.getEncoded() );
			cos.flush();
			
			return sessionKey;
		}
		
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASYMM_ALGO);
		kpg.initialize(ASYMM_KEY_SIZE*8);
		KeyPair kp = kpg.genKeyPair();
		PublicKey simplePub = kp.getPublic();
		PrivateKey simplePriv = kp.getPrivate();
		
		kpg = KeyPairGenerator.getInstance(ASYMM_ALGO);
		kpg.initialize(ASYMM_KEY_SIZE*8);
		kp = kpg.genKeyPair();
		PublicKey superPub = kp.getPublic();
		PrivateKey superPriv = kp.getPrivate();
		
		ServerSocket ss = PortChecker.getBoundedServerSocketChannel().socket();
		Socket s = ss.accept();
		
		EncryptedSocketFactory simple_esf = new EncryptedSocketFactory(simplePriv, simplePub);
		EncryptedSocketFactory supere_esf = new EncryptedSocketFactory(superPriv, superPub);
		
		EncryptedClientSocket ecs = simple_esf.getEncryptedClientSocket(ss.getInetAddress().getHostAddress(), ss.getLocalPort(), superPub);
		
		Set<PublicKey> pklist = new TreeSet<PublicKey>();
		pklist.add(simplePub);
		
		EncryptedServerSocket ess = supere_esf.getEncryptedServerSocket(s, pklist);
		
		ecs.getOutputStream().write("ciao".getBytes());
		ecs.getOutputStream().flush();
		
		byte[] tmp = new byte[512];
		ess.getInputStream().read(tmp,0,4);
		
		System.out.println(new String(tmp));
	}

}
