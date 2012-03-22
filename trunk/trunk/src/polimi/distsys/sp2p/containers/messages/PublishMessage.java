package polimi.distsys.sp2p.containers.messages;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class PublishMessage extends Message {

	private static final long serialVersionUID = 1L;

	public PublishMessage(Action act, byte[] payload)
			throws GeneralSecurityException, IOException {
		super(act, payload);
		// TODO Auto-generated constructor stub
	}

}
