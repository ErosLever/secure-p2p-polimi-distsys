package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import polimi.distsys.sp2p.containers.LocalSharedFile;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.messages.Message.Request;
import polimi.distsys.sp2p.containers.messages.Message.Response;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedClientSocket;
import polimi.distsys.sp2p.handlers.RoutingHandler;
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
	private static final String infoFile = "simplenode.info";

	// lista dei file da condividere in locale
	private Set<LocalSharedFile> fileList;
	// directory locale dove prendere e salvare i file
	private File downloadDirectory; 
	/** States whether the node is connected to a SuperNode */
	private NodeInfo superNode;
	// 
	private InetAddress myAddress;
	
	// COSTRUTTORI
	public static SimpleNode fromFile() 
			throws IOException, ClassNotFoundException, GeneralSecurityException {
		return fromFile( new File( infoFile ) );
	}

	public static SimpleNode fromFile( File file ) 
			throws IOException, ClassNotFoundException, GeneralSecurityException {
		return fromFile( file, new File( System.getProperty("user.dir") ) );
	}
	
	public static SimpleNode fromFile( File file, File workingDir) 
			throws IOException, ClassNotFoundException, GeneralSecurityException {
		Scanner sc = new Scanner( new FileInputStream( file ) );
		String[] tmp = sc.nextLine().split(":");
		sc.close();
		PublicKey pub = parsePublicKey( Serializer.base64Decode( tmp[0] ) );
		PrivateKey priv = parsePrivateKey( Serializer.base64Decode( tmp[1] ) ); 
		return new SimpleNode(pub, priv, workingDir );
	}
	
	private SimpleNode(PublicKey pub, PrivateKey priv, File workingDir) throws IOException, GeneralSecurityException, ClassNotFoundException {
		//inizializza il socket sulla prima porta disponibile partendo dalla 8000
		super( pub, priv, PortChecker.getBoundedServerSocketChannel().socket() );

		this.downloadDirectory = workingDir;

		//la lista dei file e inizialmente vuota
		fileList = new HashSet<LocalSharedFile>();

		//il nodo non e connesso al network quando viene creato
		superNode = null;

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

			Iterator<NodeInfo> superNodeList = RoutingHandler.getRandomOrderedList(
					rh.getSupernodeList() ).iterator();

			// tenta la connessione su uno dei supernodi disponibili nella lista
			while(superNodeList.hasNext()) {

				NodeInfo dest = superNodeList.next();
				EncryptedClientSocket secureChannel = null;

				try {

					secureChannel = enSockFact.getEncryptedClientSocket(
							dest.getAddress(), dest.getPublicKey());
					
					secureChannel.getOutputStream().write( Request.LOGIN );
					secureChannel.getOutputStream().write( socket.getLocalPort() );
					secureChannel.getOutputStream().sendDigest();
					secureChannel.getOutputStream().flush();
					
					Response reply = secureChannel.getInputStream().readEnum( Response.class );

					if( reply == Response.OK ){
						superNode = dest;
						byte[] ip = secureChannel.getInputStream().readFixedSizeAsByteArray(4);
						secureChannel.getInputStream().checkDigest();
						this.myAddress = Inet4Address.getByAddress( ip );
						break;
					}

					secureChannel.getOutputStream().write( Request.CLOSE_CONN );
					secureChannel.close();

				} catch(IOException e) {
					continue;
				} finally {
					if( secureChannel != null ){
						secureChannel.close();
					}
				}

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
	public void publish( Set<LocalSharedFile> fileList ) throws IOException, GeneralSecurityException {

		if( superNode != null) {

			EncryptedClientSocket secureChannel = enSockFact.getEncryptedClientSocket(
					superNode.getAddress(), superNode.getPublicKey());
			secureChannel.getOutputStream().write( Request.PUBLISH );
			
			Set<RemoteSharedFile> toSend = new HashSet<RemoteSharedFile>();
			for( LocalSharedFile lsf : fileList )
				toSend.add( new RemoteSharedFile( lsf, getNodeInfo() ) );
			secureChannel.getOutputStream().writeVariableSize( toSend );
			secureChannel.getOutputStream().sendDigest();
			secureChannel.getOutputStream().flush();

			// se ci son problemi questo metodo lancerà eccezione dunque non c'e motivo di controllare
			// la risposta
			Response reply = secureChannel.getInputStream().readEnum( Response.class );
			if( reply == Response.OK ){
				this.fileList.addAll( fileList );
			}else{
				throw new IOException( "Something went wrong while publishin'" );
			}
			/* catch (TimeoutException e) {
			 * TODO deve essere throwata dal underlying layer
			 * 
			 * superNode = null;
			 * Disconnetti il nodo e manda un messaggio di timeout

			 */
			secureChannel.close();
		} else { 
			throw new IllegalStateException("Bisogna essere connessi alla rete");
		}
	}

	/**
	 * Questo metodo viene chiamato da un nodo connesso per aggiungere un file alla lista dei file disponibili
	 * per la condivisione
	 * 
	 * @param filePath
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	public void publish( String filePath ) throws IOException, GeneralSecurityException {
		publish( retrieveFileList( filePath ) );
	}

	public void publish( File filePath ) throws IOException, GeneralSecurityException {
		publish( retrieveFileList( filePath ) );
	}

	/**
	 * Metodo usato per rimuovere un file dalla lista di condivisione
	 * 
	 * @param sh
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 */
	public void unpublish( Set<LocalSharedFile> list ) 
			throws IOException, GeneralSecurityException {

		if( superNode != null ){

			Set<RemoteSharedFile> toSend = new HashSet<RemoteSharedFile>();
			
			for( LocalSharedFile lsf : list )
				if( fileList.contains( lsf ) )
					toSend.add( new RemoteSharedFile( lsf, this.getNodeInfo() ) );
					
			EncryptedClientSocket secureChannel = enSockFact.getEncryptedClientSocket(
					superNode.getAddress(), superNode.getPublicKey());
			
			secureChannel.getOutputStream().write( Request.UNPUBLISH );
			secureChannel.getOutputStream().write( toSend );
			secureChannel.getOutputStream().sendDigest();
			secureChannel.getOutputStream().flush();

			Response reply = secureChannel.getInputStream().readEnum( Response.class );
			
			if( reply == Response.OK ) {

				fileList.removeAll( list );

			} else {
				throw new IOException( "Something went wrong while un-publishin'" );
			}

			secureChannel.close();

				/* catch (TimeoutException e) {
			//TODO deve essere throwata dal underlying layer
				 * 
				 * superNode = null;
				 * Disconnetti il nodo e manda un messaggio di timeout

				 */

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
	private Set<LocalSharedFile> retrieveFileList(String directoryPath) throws NoSuchAlgorithmException, IOException {
		return retrieveFileList( new File( directoryPath ) );
	}
	
	private Set<LocalSharedFile> retrieveFileList(File file) throws NoSuchAlgorithmException, IOException {

		// scandisce la directory ( non vengono effettuate ricerche nelle sottocartelle)
		Set<LocalSharedFile> fileList = new HashSet<LocalSharedFile>();
		if( file.exists() && file.isDirectory() ){

			File[] files = file.listFiles();
			for( File f : files) {
				if( f.isFile() && f.canRead() && ! f.isHidden() )
					fileList.add( new LocalSharedFile( f ) );
				else if( f.isDirectory() )
					fileList.addAll( retrieveFileList( f ) );
			}
		} else {
			fileList.add( new LocalSharedFile( file ) );
		}
		return fileList;

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


	public Set<LocalSharedFile> getFileList() {
		return fileList;
	}
	
	public void setDownloadDirectory( File file ){
		downloadDirectory = file;
	}
	
	public File getDownloadDirectory(){
		return downloadDirectory;
	}

	@Override
	public InetSocketAddress getSocketAddress() {
		return new InetSocketAddress( myAddress, socket.getLocalPort() );
	}

	public NodeInfo getNodeInfo(){
		return new NodeInfo( this );
	}


}