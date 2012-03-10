import java.io.Serializable;
import java.security.MessageDigest;

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
	private Request request;
	private Response response;
	private byte[] publicKey;
	private String userID;
	
	//parte che viene criptata del messaggio
	// gestita in maniera diversa a seconda del tipo di messaggio
	private byte[] payLoad;
	
	
	public Message(Request r, String id){
		
		setRequest(r);
		setResponse(null);
		setPublicKey(null);
		setUserID(id);
		
		
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public Response getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	public void setPublicKey(byte[] publicKey) {
		this.publicKey = publicKey;
	}

	public String getUserID() {
		return userID;
	}

	public void setUserID(String userID) {
		this.userID = userID;
	}

	public byte[] getPayLoad() {
		return payLoad;
	}

	public void setPayLoad(byte[] payLoad) {
		this.payLoad = payLoad;
	}
	
	

}
