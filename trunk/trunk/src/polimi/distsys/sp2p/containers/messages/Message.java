package polimi.distsys.sp2p.containers.messages;

import java.io.Serializable;

public class Message implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public interface Action {}

	public enum Request implements Action {
		LOGIN, 
		PUBLISH, 
		UNPUBLISH, 
		SEARCH, 
		FETCH, 
		LEAVE, 
		CLOSE_CONN, 
		FORWARD_SEARCH, 
		LIST_AVAILABLE_CHUNKS, 
		FETCH_CHUNK, 
		SEARCH_BY_HASH, 
		FORWARD_SEARCH_BY_HASH, 
		ADD_TRUSTED_DOWNLOAD, 
		OPEN_COMMUNICATION,
		PING
	}

	public enum Response implements Action {
		OK, 
		FAIL,
		ALREADY_CONNECTED, 
		NOT_CONNECTED,
		PONG
	}
	
}
