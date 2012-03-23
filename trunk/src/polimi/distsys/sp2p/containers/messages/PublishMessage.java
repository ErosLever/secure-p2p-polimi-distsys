package polimi.distsys.sp2p.containers.messages;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.util.Serializer;

public class PublishMessage extends Message {

	private static final long serialVersionUID = 1L;

	public PublishMessage( Set<RemoteSharedFile> fileList )
			throws GeneralSecurityException, IOException {
		super( Request.PUBLISH, Serializer.serialize( fileList ));
	}

}
