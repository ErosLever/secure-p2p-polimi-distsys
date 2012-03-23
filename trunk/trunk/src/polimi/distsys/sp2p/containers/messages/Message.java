package polimi.distsys.sp2p.containers.messages;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;

import polimi.distsys.sp2p.handlers.SecurityHandler;

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
	private final Action action;
	//private final NodeInfo nodeInfo;
	private final byte[] payload;
	private final byte[] hash;


	public Message(Action act, byte[] payload) throws GeneralSecurityException, IOException{

		this.payload = payload;
		this.action = act;
		this.hash = SecurityHandler.createHash(this.payload);

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

	public byte[] getHash() {
		return hash;
	}

	public byte[] getPayload(){
		return payload;
	}




	public interface Action {}

	public enum Request implements Action {
		LOGIN, PUBLISH, UNPUBLISH, SEARCH, FETCH, LEAVE, CLOSE_CONN
	}

	public enum Response implements Action {
		OK, FAIL, NOSECRET, ALREADY_CONNECTED
	}
}
