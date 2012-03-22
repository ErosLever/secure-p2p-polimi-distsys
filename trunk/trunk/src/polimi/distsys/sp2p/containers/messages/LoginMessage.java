package polimi.distsys.sp2p.containers.messages;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class LoginMessage extends Message {

	private static final long serialVersionUID = 1L;
	
	public LoginMessage(int port)
			throws GeneralSecurityException, IOException {
		super( Request.LOGIN, serializeInt( port) );
	}
	
	public int getPort(){
		return deserializeInt( getPayload() );
	}

	private static byte[] serializeInt(int n){
		byte[] serialized = new byte[ Integer.SIZE/8 ];
		
		for( int i=0; i<Integer.SIZE/8; i++ )
			serialized[ i ] = (byte) ( ( n >> ( i * 8 ) ) & 0xFF );
		
		return serialized;
	}

	private static int deserializeInt(byte[] serialized){
		int num = 0;
		
		for(int i=0; i<Integer.SIZE/8; i++ )
			num += ( serialized[ i ] & 0xFF ) << ( 8 * i);
		
		return num;
	}

}
