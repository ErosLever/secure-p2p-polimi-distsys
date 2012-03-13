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

public class EncryptedSocketFactory {
	
	private static final int SYMM_KEY_SIZE = 128/8;
	private static final String SYMM_ALGO = "AES";
	
	private static final int ASYMM_KEY_SIZE = 1024/8;
	private static final String ASYMM_ALGO = "RSA";
	
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
		
	}
	
	public class EncryptedClientSocket extends EncryptedSocket<PublicKey> {
		
		private final PublicKey hisPub;
		
		protected EncryptedClientSocket(InetSocketAddress isa, PublicKey hisPub) throws IOException, GeneralSecurityException{
			super( new Socket(isa.getAddress(), isa.getPort()), hisPub);
			this.hisPub = hisPub;
		}
		
		protected SecretKey handshake(PublicKey hisPub) throws GeneralSecurityException, IOException {
			
			Cipher cipher = Cipher.getInstance(ASYMM_ALGO);
			byte[] encodedKey = myPub.getEncoded();
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
			
			Cipher cipher2 = Cipher.getInstance(ASYMM_ALGO);
			cipher2.init(Cipher.DECRYPT_MODE, hisPub);
			received = cipher2.doFinal(received);
			
			SecretKeySpec sessionKey = new SecretKeySpec(received, SYMM_ALGO);
			
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
		
		@Override
		protected SecretKey handshake(Set<PublicKey> pubKeyList) throws GeneralSecurityException, IOException{
			
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
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(received);
			PublicKey hisPub = KeyFactory.getInstance(ASYMM_ALGO).generatePublic(pubKeySpec);
			
			if( ! pubKeyList.contains( hisPub )){
				throw new GeneralSecurityException("Unknown PublicKey supplied by client");
			}
			
			KeyGenerator kgen = KeyGenerator.getInstance(SYMM_ALGO);
			kgen.init( SYMM_KEY_SIZE * 8 );
			SecretKey sessionKey = kgen.generateKey();
			
			byte[] encodedKey = sessionKey.getEncoded();
			
			Cipher cipher1 = Cipher.getInstance(ASYMM_ALGO);
			cipher1.init(Cipher.ENCRYPT_MODE, myPriv);
			encodedKey = cipher1.doFinal(encodedKey);
			
			Cipher cipher2 = Cipher.getInstance(ASYMM_ALGO);
			int blockSize = ASYMM_KEY_SIZE - 11;
			for(int i=0;i<2;i++){
				cipher2.init(Cipher.ENCRYPT_MODE, hisPub);
				int bytes = Math.min(blockSize, encodedKey.length - blockSize*i);
				byte[] toWrite = cipher2.doFinal(encodedKey, blockSize*i, bytes);
				socket.getOutputStream().write(toWrite);
			}
			socket.getOutputStream().flush();
			
			return sessionKey;
		}
		
	}
	
	public static void main(String[] args) throws IOException, GeneralSecurityException{
		KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASYMM_ALGO);
		kpg.initialize(ASYMM_KEY_SIZE*8);
		KeyPair kp = kpg.genKeyPair();
		PublicKey simplePub = kp.getPublic();
		System.out.println("simplepub "+simplePub.getEncoded().length*8);
		PrivateKey simplePriv = kp.getPrivate();
		System.out.println("simplepriv "+simplePriv.getEncoded().length*8);
		
		class MyThread extends Thread{
			private PublicKey pk;
			public ServerSocket ss;
			private PrivateKey priv;
			public final PublicKey pub;
			
			public MyThread(PublicKey pk) throws NoSuchAlgorithmException{
				this.pk = pk;
				ss = PortChecker.getBoundedServerSocketChannel(8192).socket();
				KeyPairGenerator kpg = KeyPairGenerator.getInstance(ASYMM_ALGO);
				kpg.initialize(ASYMM_KEY_SIZE*8);
				KeyPair kp = kpg.genKeyPair();
				pub = kp.getPublic();
				System.out.println("superpub "+pub.getEncoded().length*8);
				priv = kp.getPrivate();
				System.out.println("superpriv "+priv.getEncoded().length*8);

			}
			public void run(){
				
				try{
					Socket s = ss.accept();
					
					EncryptedSocketFactory super_esf = new EncryptedSocketFactory(priv, pub);
					
					Set<PublicKey> pklist = new HashSet<PublicKey>();
					pklist.add(pk);
					
					EncryptedServerSocket ess = super_esf.getEncryptedServerSocket(s, pklist);
					
					byte[] tmp = new byte[4];
					ess.getInputStream().read(tmp);
					
					System.out.println(new String(tmp));
					ess.getOutputStream().close();
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		MyThread my = new MyThread(simplePub);
		my.start();
		
		EncryptedSocketFactory simple_esf = new EncryptedSocketFactory(simplePriv, simplePub);
		
		EncryptedClientSocket ecs = simple_esf.getEncryptedClientSocket("127.0.0.1", 8192, my.pub);
		
		ecs.getOutputStream().write("ciao".getBytes());
		ecs.getOutputStream().flush();
		ecs.getOutputStream().close();
	}

}
