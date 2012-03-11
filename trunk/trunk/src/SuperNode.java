import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;


/**
 * 
 */

/**
 * @author Ale
 *
 */
public class SuperNode extends Node {
	
	private HashMap<String, byte[]> listaKey = new HashMap<String, byte[]>();
	
	//da vedere come fare a riempirla quando inizializziamo i supernodi
	private HashMap<String,byte[]> listaPassword = new HashMap<String, byte[]>();
	
	
	public SuperNode() throws IOException {
		
		this( getRandomPort() );

	}
	
	public SuperNode(final int port) throws IOException {
		super(port);
		
		listen();		
	}
	
	public void listen() {

		try {


			ServerSocket serverSocket = new ServerSocket();

			serverSocket.bind(new InetSocketAddress(myPort));
			//bloccante va creato un thread nuovo per ogni accept
			while (true) {

				Socket connection = serverSocket.accept();

				ObjectInputStream ois = new ObjectInputStream(connection.getInputStream());
				ObjectOutputStream oos = new ObjectOutputStream(connection.getOutputStream());


				Message msg = (Message) ois.readObject();
				Request req = msg.getRequest();

				switch(req) {
				case LOGIN: {

					// genero la chiava segreta e la salvo
					if(!listaKey.containsKey(msg.getUserID())) {

						byte[] tmp = secretKeyGen(msg.getPublicKey());
						listaKey.put(msg.getUserID(), tmp);

					}

					msg.setResponse(Response.OK);
					msg.setPublicKey(getPublicKey());

					break;
				}

				case AUTH: {
					
					System.out.println("son arrivato all auth");
					
					if(!listaKey.containsKey(msg.getUserID())) {
						msg.setResponse(Response.NOSECRET);
						break;

					} else {

						byte[] hashCriptato = msg.getPayLoad();
						byte[] hashPass = SecurityHandler.decryptMessage(listaKey.get(msg.getUserID()), hashCriptato);
						
						
						
						System.out.println(new String(hashPass));

						if(listaPassword.containsKey(msg.getUserID()) && listaPassword.get(msg.getUserID()).equals(hashPass)) {
							msg.setResponse(Response.SUCCESS);
							//TODO: qui dovremmo generare un token di sessione?
						}

						else {
							msg.setResponse(Response.FAIL);
							
							System.out.println("fail check");
						}

						break; 
					}
				}
				
				default:
					//gestione casi
					System.out.println("Bad Request");

				}

				oos.writeObject(msg);
				oos.flush();


				oos.close();
				ois.close();
				connection.close();
			}


		} 
		catch (IOException e) {
		    System.out.println("Could not listen on port");
		    System.exit(-1);
		    
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}

}
