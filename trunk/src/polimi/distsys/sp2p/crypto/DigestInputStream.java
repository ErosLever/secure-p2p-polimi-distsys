package polimi.distsys.sp2p.crypto;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestInputStream extends InputStream {

	private InputStream in;
	private MessageDigest digest;
	private boolean active;
	
	public DigestInputStream() throws NoSuchAlgorithmException{
		this( null );
	}

	public DigestInputStream(InputStream in) throws NoSuchAlgorithmException{
		this( in, "SHA-1" );
	}

	public DigestInputStream(InputStream in, String algo) throws NoSuchAlgorithmException{
		super();
		digest = MessageDigest.getInstance( algo );
		active = true;
		setInput( in );
	}
	
	public void setInput(InputStream in){
		this.in = in;
	}
	
	public void activate(){
		active = true;
	}
	
	public void deactivate(){
		active = false;
	}
	
	public byte[] getDigest(){
		return digest.digest();
	}
	
	@Override
	public synchronized int read(byte[] buf) throws IOException {
		return read( buf, 0, buf.length );
	}
	
	@Override
	public synchronized int read(byte[] buf, int start, int maxRead) throws IOException {
		int count = in.read(buf, start, maxRead );
		if( active && count != -1 ){
			digest.update( buf, start, count );
		}
		return count; 
	}
	
	@Override
	public synchronized int read() throws IOException {
		int ret = in.read();
		if( ret != -1 && active ){
			digest.update( (byte) (ret & 0xFF) );
		}
		return ret;
	}

}
