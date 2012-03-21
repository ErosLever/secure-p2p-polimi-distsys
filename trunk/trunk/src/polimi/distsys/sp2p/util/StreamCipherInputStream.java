package polimi.distsys.sp2p.util;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.CipherInputStream;

import polimi.distsys.sp2p.util.StreamCipherOutputStream.ResettableCipher;

public class StreamCipherInputStream extends FilterInputStream {
	
	private final List<ResettableCipher> ciphers;
	
	public StreamCipherInputStream(InputStream out, List<ResettableCipher> ciphers) {
		super(out);
		this.ciphers = ciphers;
	}
	
	public synchronized InputStream readFixedSize( int len ) throws GeneralSecurityException{
		//Determine ciphered size
		List<ResettableCipher> reversed = new ArrayList<ResettableCipher>( ciphers );
		Collections.reverse( reversed );
		for( ResettableCipher rc : reversed ){
			len = rc.getInputSize( len );
		}
		InputStream in = new LimitedInputStream( this.in, len );
		for( ResettableCipher rc : ciphers ){
			if( rc.getAlgorithm().equals("RSA") )
				in = new StreamCipherOutputStream.RSAInputStream( in, rc ); 
			else
				in = new CipherInputStream( in, rc.reset() ); 
		}
		return in;
	}
	
	@Override
	public synchronized int read() throws IOException {
		try {
			return readFixedSize( 1 ).read();
		} catch (GeneralSecurityException e) {
			throw new IOException(e);
		}
	}
	
	public synchronized int readInt() throws GeneralSecurityException, IOException{
		byte[] repr = readFixedSizeAsByteArray( Integer.SIZE / 8 );
		int ret = 0;
		for( int i=0; i<repr.length; i++)
			ret += repr[i] << ( i * 8 ) ;
		return ret;
	}

	public synchronized byte[] readFixedSizeAsByteArray(int len) throws GeneralSecurityException, IOException{
		return readFixedSizeAsByteArray(len, true);
	}
	
	public synchronized byte[] readFixedSizeAsByteArray(int len, boolean postCheck) throws GeneralSecurityException, IOException{
		InputStream in = readFixedSize( len );
		if(postCheck){
			byte[] ret = new byte[ len ];
			int received = 0;
			while( received < len ){
				received += in.read( ret, received, len-received );
			}
			return ret; 
		}else{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[512];
			int recv;
			while( (recv = in.read(buf, 0, buf.length)) != -1)
				baos.write(buf, 0, recv);
			return baos.toByteArray();
		}
	}
	
	public synchronized <E>E readObject( Class<E> type, int len ) throws IOException, GeneralSecurityException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream( readFixedSize( len ) );
		Object o = ois.readObject();
        ois.close();
        return type.cast(o);
	}
	
	public synchronized Object readObject( int len ) throws IOException, GeneralSecurityException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream( readFixedSize( len ) );
		Object o = ois.readObject();
        ois.close();
        return o;
	}

	public synchronized <E>E readObject( Class<E> type ) throws IOException, GeneralSecurityException, ClassNotFoundException{
		int size = readInt();
		return readObject( type, size );
	}
	
	public synchronized Object readObject() throws IOException, GeneralSecurityException, ClassNotFoundException{
		int size = readInt();
		return readObject( size );
	}
	
	public synchronized byte[] readVariableSize() throws GeneralSecurityException, IOException{
		int size = readInt();
		return readFixedSizeAsByteArray( size );
	}
	
	public static class LimitedInputStream extends FilterInputStream {

		private int remaining;
		
		public LimitedInputStream(InputStream in, int len) {
			super(in);
			remaining = len;
		}
		
		@Override
		public synchronized int read() throws IOException {
			if( remaining <= 0)
				return -1;
			int ret = in.read();
			if(ret != -1)
				remaining--;
			return ret;
		}
		
		@Override
		public synchronized int read( byte[] buf ) throws IOException {
			return read( buf, 0, buf.length );
		}
		
		@Override
		public synchronized int read( byte[] buf, int start, int toRead) throws IOException {
			if( remaining <= 0)
				return -1;
			int max = Math.min( Math.min(remaining, toRead), buf.length - start );
			int count = in.read( buf, start, max);
			if(count >= 0)
				remaining -= count;
			return count;
		}
		
		@Override
		public synchronized void close(){
			// Do nothing
		}
		
	}

}
