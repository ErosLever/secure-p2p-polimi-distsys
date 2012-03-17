package polimi.distsys.sp2p;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import polimi.distsys.sp2p.Message.Request;
import polimi.distsys.sp2p.Message.Response;

/**
 * @author Ale
 * 
 * classe usata per gestire i nodi client del sistema p2p
 * contiene le principali operazioni eseguibili da un nodo
 * 
 *  Join
 *  Publish
 *  Search
 *
 */
public class SimpleNode extends Node {

	private final String userID;
	private final byte[] password;
	// lista dei file da condividere in locale
	private ArrayList<LocalSharedFile> fileList;
	// directory locale dove prendere e salvare i file
	private String downloadDirectory; // directory dove vengon considerati i file
	/** States whether the node is connected to a SuperNode */
	private boolean connected;
	
	//lista utilizzata per memorizzare i risultati dell'ultima search
	private ArrayList<RemoteSharedFile> searchResult;



	// COSTRUTTORI
	
	//TODO ERRORE CAZZO: abbiamo cambiato il costruttore del nodo astratto ma ora e incosistente su come generiamo le chiavi e le scriviamo nel main
	public SimpleNode(final int port, final String userID, final String password, String directory) throws IOException, NoSuchAlgorithmException {
		super(port);

		this.downloadDirectory = directory;
		this.userID = userID;
		this.password = SecurityHandler.hashFunction( password );

		connected = false;
		fileList = new ArrayList<LocalSharedFile>();

	}

	//JOIN
	public void join() throws GeneralSecurityException, IOException, ClassNotFoundException {
		join(true);
	}

	/**
	 *  Il nodo instaura una connessione con uno dei supernodi attivi
	 *  
	 * @param checkAlreadyConnected
	 * @throws GeneralSecurityException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void join(boolean checkAlreadyConnected) throws GeneralSecurityException, IOException, ClassNotFoundException {

		if(!checkAlreadyConnected || !connected) {

			Socket connection = null;
			ObjectOutputStream oos = null;
			ObjectInputStream ois = null;

			try{
				NodeInfo dest = rh.getNodesList().iterator().next();
				Message login = createMessage(Request.LOGIN, password, dest);

				InetSocketAddress sa = dest.getAddress();
				connection = new Socket(sa.getHostName(), sa.getPort());
				oos = new ObjectOutputStream(connection.getOutputStream());
				ois = new ObjectInputStream(connection.getInputStream());

				oos.writeObject(login);
				oos.flush();

				login = (Message) ois.readObject();
				if(login.isResponse()){
					Response response = (Response) login.getAction();
					if(response == Response.OK) {
						//Successfully logged in
						return;
					}
				}
			}finally{
				if(oos != null)
					if(!connection.isOutputShutdown())
						oos.close();
				if(ois != null)
					if(!connection.isInputShutdown())
						ois.close();
				if(connection != null)
					if(!connection.isClosed())
						connection.close();
			}
		}

	}


	// PUBLISH 
	/**
	 * Metodo che viene chiamato a seguito di una join
	 * scandisce la directory alla ricerca dei file da condividere completi
	 * ed invia la lista al supernodo a cui si e collegati
	 */
	public void publish() {


		retrieveFileList(downloadDirectory);
		//TODO CONTATTA IL SUPERNODO

	}

	/**
	 * Questo metodo viene chiamato da un nodo connesso per aggiungere un file alla lista dei file disponibili
	 * per la condivisione
	 * 
	 * @param filePath
	 */
	public void publish(String filePath) {

		if(connected) {
			// aggiunge il file alla lista locale
			File f = new File(filePath);
			if(f.exists()) {

				addFileToList(f);

				//TODO CONTATTA IL SUPERNODO

				System.out.println("Ho aggiunto con successo il file" + 
						f.getName() + 
						"  alla lista dei file condivisi");
			} else {

				System.out.println("il file indicato non esiste!");

			}


		} else {
			System.out.println("Non sei connesso alla rete!");
		}


	}

	/**
	 * Metodo usato per rimuovere un file dalla lista di condivisione
	 * 
	 * @param sh
	 */
	public void unPublish(LocalSharedFile sh) {

		if(connected) {

			if(fileList.contains(sh)) {

				fileList.remove(sh);

				//TODO CONTATTA IL SUPERNODO

				System.out.println("Ho rimosso il file" + 
						sh.getName() + 
						" dalla lista dei file condivisi");
			} else {

				System.out.println("il file indicato non esiste!");
			}

		} else {
			System.out.println("Non sei connesso alla rete!");
		}


	}

	/**
	 *  data una directory di partenza costruisce la lista dei file da condividere
	 * @param directoryPath la directory dove vengon presi i file da condividere (non vengono considerate le sotto cartelle)
	 */
	private void retrieveFileList(String directoryPath) {

		// scandisce la directory ( non vengono effettuate ricerche nelle sottocartelle)
		File directory = new File(directoryPath);
		if (directory.exists() && directory.isDirectory()) {

			File[] files = directory.listFiles();
			for ( File f: files) {
				if (f.isFile() && f.canRead() && !f.isHidden()) {

					addFileToList(f);

				}
			}
		}


	}


	/**
	 * aggiunge un file alla lista dei file condivisi del nodo
	 * @param f file da aggiungere
	 */
	private void addFileToList(File f) {

		if (f.isFile() && f.canRead() && !f.isHidden()) {

			LocalSharedFile LocalSharedFile = new LocalSharedFile();

			LocalSharedFile.setName(f.getName());
			LocalSharedFile.setPath(f.getAbsolutePath());
			LocalSharedFile.setHash(SecurityHandler.createHash(f));

			fileList.add(LocalSharedFile);

		}

	}

	//SEARCH
	
	public void search(String query) {
		
		//COSTRUISCE IL MESSAGGIO PER IL SUPERNODO
		//ATTENDE LA RISPOSTA
		//RIEMPIE LA LISTA 
	}
	
	// GETTER & SETTER
	public String getUserID() {
		return userID;
	}

	public boolean isConnected() {
		return connected;
	}

	
	public ArrayList<LocalSharedFile> getFileList() {
		return fileList;
	}

}