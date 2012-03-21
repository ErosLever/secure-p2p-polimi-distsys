package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import polimi.distsys.sp2p.containers.LocalSharedFile;
import polimi.distsys.sp2p.containers.Message;
import polimi.distsys.sp2p.containers.Message.Request;
import polimi.distsys.sp2p.containers.Message.Response;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.handlers.EncryptedSocketFactory;
import polimi.distsys.sp2p.handlers.EncryptedSocketFactory.EncryptedClientSocket;
import polimi.distsys.sp2p.handlers.RoutingHandler;
import polimi.distsys.sp2p.handlers.SecurityHandler;
import polimi.distsys.sp2p.util.PortChecker;
import polimi.distsys.sp2p.util.Serializer;

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

	//file da cui recuperare le informazioni
	private final String infoFile = "simplenode.info";

	private final PrivateKey privateKey;
	private final PublicKey publicKey;	
	//il socket usato sia dal nodo che dal supernodo per ricevere le connessioni
	private final ServerSocket socket;


	// lista dei file da condividere in locale
	private ArrayList<LocalSharedFile> fileList;
	// directory locale dove prendere e salvare i file
	private String downloadDirectory; 

	/** States whether the node is connected to a SuperNode */
	private NodeInfo superNode;

	//lista utilizzata per memorizzare i risultati dell'ultima search
	private ArrayList<RemoteSharedFile> searchResult;

	private EncryptedSocketFactory esf;


	// COSTRUTTORI

	public SimpleNode(String directory) throws IOException, NoSuchAlgorithmException, ClassNotFoundException {
		super();

		//inizializza il socket sulla prima porta disponibile partendo dalla 8000
		socket = PortChecker.getBoundedServerSocketChannel().socket();

		//legge il file per recuperare chiave pubblica e privata del nodo
		InputStream is = new FileInputStream(infoFile);

		Scanner sc = new Scanner(is);
		String[] tmp = sc.nextLine().split(":");
		publicKey = Serializer.deserialize(
				Serializer.base64Decode(tmp[0]), 
				PublicKey.class);
		privateKey = Serializer.deserialize(
				Serializer.base64Decode(tmp[1]), 
				PrivateKey.class);

		this.downloadDirectory = directory;

		//la lista dei file e inizialmente vuota
		fileList = new ArrayList<LocalSharedFile>();

		//il nodo non e connesso al network quando viene creato
		superNode = null;

		esf = new EncryptedSocketFactory(privateKey, publicKey);

	}

	public SimpleNode() throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
		this("."+File.separator);
	}

	//JOIN

	/**
	 *  Il nodo instaura una connessione con uno dei supernodi attivi
	 *  
	 * @param checkAlreadyConnected
	 * @throws GeneralSecurityException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void join() throws GeneralSecurityException, IOException, ClassNotFoundException {

		if(superNode == null) {

			Iterator<NodeInfo> superNodeList = RoutingHandler.getRandomOrderedList(rh.getSupernodeList()).iterator();

			// tenta la connessione su uno dei supernodi disponibili nella lista
			while(superNodeList.hasNext()) {

				NodeInfo dest = superNodeList.next();

				// per salvare una porta ( max 65536 bastano 2 byte )
				byte[] payload = new byte[2];

				//prende gli 8 bit meno significativi
				payload[0] = (byte) (socket.getLocalPort() & 0xff);
				//leggo gli altri 8 bit ( piu a destra )
				payload[1] = (byte) (socket.getLocalPort() >> 8);

				Message login = new Message(Request.LOGIN, payload);

				try {

					EncryptedClientSocket secureChannel = esf.getEncryptedClientSocket(dest.getAddress(), dest.getPublicKey());
					secureChannel.writeMessage(login);
					Message reply = secureChannel.readMessage();

					if( reply.getAction() == Response.OK ){
						superNode = dest;
						break;
					}

					//TODO DEVO CHIUDERE IL SOCKET

				} catch(IOException e) {
					continue;

				}

				/* catch (TimeoutException e) {
					//TODO deve essere throwata dal underlying layer
					continue;

				} 

				 */

			} 
		} else {
			throw new IllegalStateException("Connessione già effettuata"); }
	}

	// PUBLISH 
	/**
	 * Metodo che viene chiamato a seguito di una join
	 * scandisce la directory alla ricerca dei file da condividere completi
	 * ed invia la lista al supernodo a cui si e collegati
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	public void publish() throws IOException, GeneralSecurityException {

		if( superNode != null) {

			// Popola la lista dei file condivisi
			retrieveFileList(downloadDirectory);
			// trasformo l'oggetto in array di bytes
			byte[] payload = Serializer.serialize(fileList);	
			Message publish = new Message(Request.PUBLISH, payload);

			EncryptedClientSocket secureChannel = esf.getEncryptedClientSocket(superNode.getAddress(), superNode.getPublicKey());
			secureChannel.writeMessage(publish);

			// se ci son problemi questo metodo lancerà eccezione dunque non c'e motivo di controllare
			// la risposta
			secureChannel.readMessage();

			/* catch (TimeoutException e) {
		//TODO deve essere throwata dal underlying layer
			 * 
			 * superNode = null;
			 * Disconnetti il nodo e manda un messaggio di timeout

			 */
			
			secureChannel.close();
		}
		else 
			throw new IllegalStateException("Bisogna essere connessi alla rete");

	}

	/**
	 * Questo metodo viene chiamato da un nodo connesso per aggiungere un file alla lista dei file disponibili
	 * per la condivisione
	 * 
	 * @param filePath
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	public void publish(String filePath) throws IOException, GeneralSecurityException {

		if(superNode != null) {

			// aggiunge il file alla lista locale
			File f = new File(filePath);
			if(f.exists()) {

				addFileToList(f);

				RemoteSharedFile tmpFile = new RemoteSharedFile();

				// vado a prendere il file dalla lista dei condivisi per creare il messaggio
				// in questo modo evito di dover calcolare 2 volte l'hash del file
				for(LocalSharedFile lsf: fileList) {
					if(lsf.getPath().equals(f.getAbsolutePath())) {

						tmpFile.setName(f.getName());
						tmpFile.setHash(lsf.getHash());
					}

				}
				// trasformo l'oggetto in array di bytes
				byte[] payload = Serializer.serialize(tmpFile);	
				Message publish = new Message(Request.PUBLISHU, payload);

				EncryptedClientSocket secureChannel = esf.getEncryptedClientSocket(superNode.getAddress(), superNode.getPublicKey());
				secureChannel.writeMessage(publish);

				// se ci son problemi questo metodo lancerà eccezione dunque non c'e motivo di controllare
				// la risposta
				secureChannel.readMessage();

				/* catch (TimeoutException e) {
			//TODO deve essere throwata dal underlying layer
				 * 
				 * superNode = null;
				 * Disconnetti il nodo e manda un messaggio di timeout

				 */
				secureChannel.close();
				
			} else {

				throw new IOException("il file indicato non esiste!");
			}

		} else {
			throw new IllegalStateException("Bisogna essere connessi alla rete");
		}


	}

	/**
	 * Metodo usato per rimuovere un file dalla lista di condivisione
	 * 
	 * @param sh
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	public void unPublish(LocalSharedFile sh) throws IOException, GeneralSecurityException {

		if(superNode != null) {

			if (fileList.contains(sh)){

				RemoteSharedFile tmpFile = new RemoteSharedFile();

				tmpFile.setName(sh.getName());
				tmpFile.setHash(sh.getHash());

				// trasformo l'oggetto in array di bytes
				byte[] payload = Serializer.serialize(tmpFile);	
				Message publish = new Message(Request.PUBLISHD, payload);

				EncryptedClientSocket secureChannel = esf.getEncryptedClientSocket(superNode.getAddress(), superNode.getPublicKey());
				secureChannel.writeMessage(publish);

				Message reply = secureChannel.readMessage();

				if(reply.getAction() == Response.OK ) {

					fileList.remove(sh);

				}

				secureChannel.close();

				/* catch (TimeoutException e) {
			//TODO deve essere throwata dal underlying layer
				 * 
				 * superNode = null;
				 * Disconnetti il nodo e manda un messaggio di timeout

				 */

			} else { 
				throw new IOException("Il file indicato non è condiviso");
			}


		} else {
			throw new IllegalStateException("Bisogna essere connessi alla rete");
		}

	}

	/**
	 *  data una directory di partenza costruisce la lista dei file da condividere
	 * @param directoryPath la directory dove vengon presi i file da condividere (non vengono considerate le sotto cartelle)
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	private void retrieveFileList(String directoryPath) throws NoSuchAlgorithmException, IOException {

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
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
	private void addFileToList(File f) throws NoSuchAlgorithmException, IOException {

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
	public boolean isConnected() {

		return superNode != null;
	}


	public ArrayList<LocalSharedFile> getFileList() {
		return fileList;
	}


	public PublicKey getPublicKey() {
		return publicKey;
	}

	@Override
	public ServerSocket getSocketAddress() {
		return socket;
	}



}