package polimi.distsys.sp2p;

import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * 
 */

/**
 * @author Ale
 *
 * in questa classe l'idea e di incapsulare tutti i dati necessari per la comunicazione
 */

public class Message implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 3955511950935291985L;
	private static final int KEY_SIZE = 128/8;
	private final Action action;
	//private final NodeInfo nodeInfo;
	private final byte[] payload;
	private final byte[] hash;
	
	
	public Message(Action act, byte[] payload, SecretKey sk) throws GeneralSecurityException{
		
		action = act;
		//nodeInfo = ni;
		
		byte[] sharedkey = extendSharedKeyToSize(sk, KEY_SIZE);
		
		SecretKeySpec keySpec = new SecretKeySpec(sharedkey, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		
		this.payload = cipher.doFinal(payload);
		hash = MessageDigest.getInstance("SHA-1").digest(payload);
		
	}

	public boolean isRequest() {
		return action instanceof Request;
	}

	public boolean isResponse() {
		return action instanceof Response;
	}
	
	public Action getAction(){
		return action;
	}

/*	public NodeInfo getNodeInfo() {
		return nodeInfo;
	}
*/
	public byte[] decryptPayload(SecretKey sk) throws GeneralSecurityException {
		
		byte[] sharedkey = extendSharedKeyToSize(sk, KEY_SIZE);
		SecretKeySpec keySpec = new SecretKeySpec(sharedkey, "AES");
		Cipher cipher = Cipher.getInstance("AES");
		cipher.init(Cipher.DECRYPT_MODE, keySpec);
		byte[] decrypted = cipher.doFinal(payload);
		
		byte[] hash = MessageDigest.getInstance("SHA-1").digest(decrypted);
		if(!Arrays.equals(hash, this.hash))
			throw new GeneralSecurityException("Message corrupted");
		
		return decrypted;
	}

	private static byte[] extendSharedKeyToSize(final SecretKey sk, final int size){
		final byte[] sharedkey = new byte[size];
		int count = 0;
		while(count < size){
			final int toCopy = Math.min(size-count, sk.getEncoded().length);
			System.arraycopy(sk.getEncoded(), 0, sharedkey, count, toCopy);
			count += toCopy;
		}
		return sharedkey;
	}
	
	
	public interface Action {}
	
	public enum Request implements Action {
		LOGIN, AUTH, PUBLISH, SEARCH, FETCH, LEAVE
	}
	
	public enum Response implements Action {
		OK, SUCCESS, FAIL, NOSECRET
	}
}
