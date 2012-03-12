package polimi.distsys.sp2p;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class SecretMessage implements Serializable {

	private static final long serialVersionUID = -280934385779335526L;
	
	private final byte[] msg;
	private final byte[] hash;
	
	public SecretMessage(final String msg, final SecretKey sk) throws GeneralSecurityException {
		SecretKeySpec keySpec = new SecretKeySpec(sk.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		this.msg = cipher.doFinal(msg.getBytes());
		this.hash = MessageDigest.getInstance("SHA-1").digest(msg.getBytes());
	}
	
	public String decrypt(final SecretKey sk) throws GeneralSecurityException{
		SecretKeySpec keySpec = new SecretKeySpec(sk.getEncoded(), "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		byte[] decrypted = cipher.doFinal(msg);
		
		byte[] hash = MessageDigest.getInstance("SHA-1").digest(decrypted);
		if(!Arrays.equals(hash, this.hash))
			throw new GeneralSecurityException("Message corrupted");
		return new String(decrypted);
	}

}
