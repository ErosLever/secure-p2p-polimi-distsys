import java.io.IOException;
import java.net.ServerSocket;


/**
 * 
 */

/**
 * @author Ale
 *
 */
public class SuperNode extends Node {
	
	
	public SuperNode() {
		super();
		

	}
	
	public void listen() {
		
		try {
			
		    ServerSocket serverSocket = new ServerSocket(getInitialPort());
		    //bloccante va creato un thread nuovo per ogni accept
		    serverSocket.accept();
		} 
		catch (IOException e) {
		    System.out.println("Could not listen on port");
		    System.exit(-1);
		}

		
	}

}
