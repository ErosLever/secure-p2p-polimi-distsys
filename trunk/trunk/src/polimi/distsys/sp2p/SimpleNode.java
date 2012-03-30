package polimi.distsys.sp2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import polimi.distsys.sp2p.containers.IncompleteSharedFile;
import polimi.distsys.sp2p.containers.LocalSharedFile;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.SharedFile;
import polimi.distsys.sp2p.containers.messages.Message.Request;
import polimi.distsys.sp2p.containers.messages.Message.Response;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedClientSocket;
import polimi.distsys.sp2p.crypto.EncryptedSocketFactory.EncryptedServerSocket;
import polimi.distsys.sp2p.handlers.DownloadHandler;
import polimi.distsys.sp2p.handlers.RoutingHandler;
import polimi.distsys.sp2p.handlers.SimpleNodeServer;
import polimi.distsys.sp2p.handlers.DownloadHandler.DownloadCallback;
import polimi.distsys.sp2p.util.Listener;
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
	private final Set<LocalSharedFile> fileList;
	private final Set<IncompleteSharedFile> incompleteFiles;
	// directory locale dove prendere e salvare i file
	private File downloadDirectory; 
	/** States whether the node is connected to a SuperNode */
	private NodeInfo supernode;
	private EncryptedClientSocket secureChannel;
	
	private InetAddress myAddress;
	
	@SuppressWarnings("unused")
	private final Listener listener;
	
	private final Map<IncompleteSharedFile, DownloadHandler> downHandlers;
	
	private long lastCommunicationTime;
	
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
		incompleteFiles = new HashSet<IncompleteSharedFile>();

		//il nodo non e connesso al network quando viene creato
		supernode = null;
		secureChannel = null;
		
		downHandlers = new HashMap<IncompleteSharedFile, DownloadHandler>();
		
		listener = new Listener( this.socket.getChannel(), new SimpleNodeServer( this ) {
			
			@Override
			public NodeInfo getCorrespondingNode( PublicKey key ) {
				return rh.getConnectedNode( key );
			}
			
			@Override
			public EncryptedServerSocket getEncryptedServerSocket(Socket sock)
					throws IOException, GeneralSecurityException {
				return enSockFact.getEncryptedServerSocket( sock, rh.getTrustedKeys() );
			}
			
			@Override
			public void addTrustedDownload(NodeInfo node, SharedFile file) {
				rh.addConnectedNode( node );
			}
		});

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
	public void join() throws GeneralSecurityException,IllegalStateException, IOException, ClassNotFoundException {

		if(secureChannel == null) {
			
			//fa partire la lista dei supernodi da un punto casuale
			Iterator<NodeInfo> superNodeList = RoutingHandler.getRandomOrderedList(
					rh.getSupernodeList() ).iterator();

			// tenta la connessione su uno dei supernodi disponibili nella lista
			while(superNodeList.hasNext()) {

				NodeInfo dest = superNodeList.next();

				try {

					secureChannel = enSockFact.getEncryptedClientSocket(
							dest.getAddress(), dest.getPublicKey());
					
					secureChannel.getOutputStream().write( Request.LOGIN );
					secureChannel.getOutputStream().write( socket.getLocalPort() );
					secureChannel.getOutputStream().sendDigest();
					secureChannel.getOutputStream().flush();
					
					Response reply = secureChannel.getInputStream().readEnum( Response.class );

					if( reply == Response.OK ){
						supernode = dest;
						byte[] ip = secureChannel.getInputStream().readFixedSizeAsByteArray(4);
						secureChannel.getInputStream().checkDigest();
						this.myAddress = Inet4Address.getByAddress( ip );
						lastCommunicationTime = System.currentTimeMillis();
						break;
					}else{
						secureChannel.close();
					}

				} catch(IOException e) {
					if( secureChannel != null ){
						secureChannel.close();
						secureChannel = null;
					}
					continue;
				}

			} 
		} else {
			throw new IllegalStateException(); }
	}
	
	public void checkConnectionWithSuperNode() throws IllegalStateException, GeneralSecurityException, IOException, ClassNotFoundException{
		if( supernode == null)
			throw new IllegalStateException("Not connected");
		if( ! secureChannel.isConnected() ){
			secureChannel = enSockFact.getEncryptedClientSocket( 
					supernode.getAddress(), supernode.getPublicKey() );
		}else{
			if( System.currentTimeMillis() - lastCommunicationTime < EncryptedSocketFactory.SOCKET_TIMEOUT )
				return;
			try{
				secureChannel.getOutputStream().write( Request.PING );
				secureChannel.getOutputStream().flush();
				Response reply = secureChannel.getInputStream().readEnum( Response.class );
				if( !reply.equals( Response.PONG ) )
					throw new IOException();
			}catch(IOException e){
				secureChannel.close();
				secureChannel = enSockFact.getEncryptedClientSocket( 
						supernode.getAddress(), supernode.getPublicKey() );
			}
		}
		lastCommunicationTime = System.currentTimeMillis();
	}
	
	//LEAVE
	
	/**
	 * disconnette il nodo dalla rete
	 * 
	 * @throws GeneralSecurityException 
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalStateException 
	 */
	public void leave() throws IOException, GeneralSecurityException, IllegalStateException, ClassNotFoundException {
		
		try {
			
			checkConnectionWithSuperNode();

			secureChannel.getOutputStream().write( Request.LEAVE );
			secureChannel.getOutputStream().flush();

			Response reply = secureChannel.getInputStream().readEnum( Response.class );

			if( reply == Response.OK ){
				supernode = null;

			}else{
				throw new IOException("Something went wrong while leaving (IO)");
			}
		} catch (IllegalStateException e) {
			throw e;
		} catch (ClassNotFoundException e) {
			throw new IOException("Something went wrong while leaving (Class)");
		} 
		finally {
			
			closeConnection();
			
				secureChannel = null;
			}
		}

	

	// PUBLISH 
	/**
	 * Metodo che viene chiamato a seguito di una join
	 * scandisce la directory alla ricerca dei file da condividere completi
	 * ed invia la lista al supernodo a cui si e collegati
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalStateException 
	 */
	public void publish( Set<? extends SharedFile> fileList ) throws IOException, GeneralSecurityException, IllegalStateException, ClassNotFoundException {

		checkConnectionWithSuperNode();
		
		Set<SharedFile> toSend = new HashSet<SharedFile>();
		
		//evita duplicati
		for( SharedFile f : fileList) {
			if(!this.fileList.contains(f)) {
				toSend.add(f);
			}
		}
		//manda solo i file che non erano già presenti nella lista
		if (!toSend.isEmpty()) {
			
			secureChannel.getOutputStream().write( Request.PUBLISH );

			secureChannel.getOutputStream().writeVariableSize( toSend );
			secureChannel.getOutputStream().sendDigest();
			secureChannel.getOutputStream().flush();
		
		
		Response reply = secureChannel.getInputStream().readEnum( Response.class );
		if( reply == Response.OK ){
			for( SharedFile sf : toSend )
				if( sf instanceof LocalSharedFile )
					this.fileList.add( (LocalSharedFile) sf );
				else if( sf instanceof IncompleteSharedFile )
					if( ! this.incompleteFiles.contains( sf ) )
						this.incompleteFiles.add( (IncompleteSharedFile) sf );
		}else{
			
			//TODO NON DOVREMMO DISCONNETTERE IL NODO IN QUESTO CASO
			
			/* ci sarebbe da fare una catch della io exception e disconnettere nel caso il nodo dalla rete
			 */
			throw new IOException( "Qualcosa è andato storto!" );
		} 
		} else {
			
			//solo se TUTTI i file richiesti erano già pubblicati
			throw new IOException("Il file richiesto è già stato pubblicato"); 
		}
		
		
	}

	/**
	 * Questo metodo viene chiamato da un nodo connesso per aggiungere un file alla lista dei file disponibili
	 * per la condivisione
	 * 
	 * @param filePath
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalStateException 
	 */
	public void publish( String filePath ) throws IOException, GeneralSecurityException, IllegalStateException, ClassNotFoundException {
		publish( retrieveFileList( filePath ) );
	}

	public void publish( File filePath ) throws IOException, GeneralSecurityException, IllegalStateException, ClassNotFoundException {
		publish( retrieveFileList( filePath ) );
	}

	/**
	 * Metodo usato per rimuovere un file dalla lista di condivisione
	 * 
	 * @param sh
	 * @throws IOException 
	 * @throws GeneralSecurityException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalStateException 
	 */
	public void unpublish( Set<LocalSharedFile> list ) 
			throws IOException, GeneralSecurityException, IllegalStateException, ClassNotFoundException {

		checkConnectionWithSuperNode();

		secureChannel.getOutputStream().write( Request.UNPUBLISH );
		secureChannel.getOutputStream().writeVariableSize( list );
		secureChannel.getOutputStream().sendDigest();
		secureChannel.getOutputStream().flush();

		Response reply = secureChannel.getInputStream().readEnum( Response.class );
		
		if( reply == Response.OK ) {

			fileList.removeAll( list );

		} else {
			throw new IOException( "Something went wrong while un-publishin'" );
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
	@SuppressWarnings("unchecked")
	public List<RemoteSharedFile> search(String query) throws IllegalStateException, GeneralSecurityException, IOException, ClassNotFoundException {
		
		checkConnectionWithSuperNode();
		
		secureChannel.getOutputStream().write( Request.SEARCH );
		secureChannel.getOutputStream().writeVariableSize( query.getBytes("utf-8") );
		secureChannel.getOutputStream().sendDigest();
		secureChannel.getOutputStream().flush();

		Response reply = secureChannel.getInputStream().readEnum( Response.class );

		if( reply == Response.OK )  {
			
			List<RemoteSharedFile> searchList = secureChannel.getInputStream().readObject(List.class);
			secureChannel.getInputStream().checkDigest();
			
			return searchList;
		}else
			throw new IOException( "Server response: got "+reply.name()+" instead of OK");
	
	}
	
	public RemoteSharedFile searchByHash(byte[] hash) throws IllegalStateException, GeneralSecurityException, IOException, ClassNotFoundException {
		
		checkConnectionWithSuperNode();
		
		secureChannel.getOutputStream().write( Request.SEARCH_BY_HASH );
		secureChannel.getOutputStream().write( hash );
		secureChannel.getOutputStream().sendDigest();
		secureChannel.getOutputStream().flush();

		Response reply = secureChannel.getInputStream().readEnum( Response.class );

		if( reply == Response.OK )  {
			
			RemoteSharedFile toReturn = secureChannel.getInputStream().readObject( RemoteSharedFile.class );
			secureChannel.getInputStream().checkDigest();
			
			return toReturn;
		}else
			throw new IOException( "Server response: got "+reply.name()+" instead of OK");
	
	}
	
	public void requestCommunicationChannel( NodeInfo node, SharedFile sharedFile ) throws IOException, GeneralSecurityException{
		synchronized(secureChannel){
			
			secureChannel.getOutputStream().write( Request.OPEN_COMMUNICATION );
			secureChannel.getOutputStream().writeVariableSize( node );
			secureChannel.getOutputStream().writeVariableSize( sharedFile );
			secureChannel.getOutputStream().sendDigest();
			secureChannel.getOutputStream().flush();
			
			Response reply = secureChannel.getInputStream().readEnum( Response.class );
			if( ! reply.equals( Response.OK ) )
				throw new IOException( "Bad response from server" );
			secureChannel.getInputStream().checkDigest();
			
		}
	}
		
	public void startDownload( final RemoteSharedFile file, String filename, final DownloadCallback callback ) throws IOException, IllegalStateException, GeneralSecurityException, ClassNotFoundException{
		checkConnectionWithSuperNode();
		
		File dest = new File( downloadDirectory, filename );
		DownloadHandler dh = new DownloadHandler( enSockFact, file, dest,
				new DownloadCallback(){

					@Override
					public void endOfDownload( IncompleteSharedFile isf ) {
						downHandlers.remove( file );
						callback.endOfDownload( isf );
						try{
							if( isf.isCompleted() ){
								isf.matchCheckSum();
								fileList.add( new LocalSharedFile( isf.getDestinationFile() ) );
								incompleteFiles.remove( isf );
							}
						}catch(IOException e){
							callback.gotException( isf, e );
						} catch (NoSuchAlgorithmException e) {
							callback.gotException( isf, e );
						} catch (GeneralSecurityException e) {
							callback.gotException( isf, e );
						}
					}

					@Override
					public void receivedChunk( IncompleteSharedFile isf, int i ) {
						callback.receivedChunk( isf, i );
						if( ! incompleteFiles.contains( isf ) ){
							incompleteFiles.add( isf );
							try {
								publish( Collections.singleton( isf.toRemoteSharedFile( SimpleNode.this.getNodeInfo() ) ) );
							} catch (Exception e) {
								callback.gotException( isf, e );
							}
						}
					}

					@Override
					public void gotException( IncompleteSharedFile isf, Exception ex ) {
						downHandlers.remove( file );
						callback.gotException( isf, ex );
					}

					@Override
					public void askCommunicationToNode(NodeInfo node,
							SharedFile sharedFile) throws IOException, GeneralSecurityException {
						requestCommunicationChannel( node, sharedFile);
					}
			
		});
		downHandlers.put( dh.getIncompleteFile(), dh);
		dh.start();
		
	}
	
	public void stopAllDownloads() throws IOException, GeneralSecurityException{
		while(downHandlers.size() > 0){
			IncompleteSharedFile file = downHandlers.keySet().iterator().next();
			DownloadHandler dh = downHandlers.get( file );
			dh.setActive( false );
			try {
				// wait 'till it finishes it work
				dh.join();
			} catch (InterruptedException e) {
			}
			Exception ex = dh.checkException(); 
			if( ex != null ){
				if( ex instanceof IOException )
					throw new IOException( ex );
				if( ex instanceof GeneralSecurityException )
					throw new GeneralSecurityException( ex );
			}
			downHandlers.remove( file );
		}
	}
	
	public void resumeDownload( File file, DownloadCallback callback ) throws IOException, IllegalStateException, GeneralSecurityException, ClassNotFoundException{
		byte[] hash = new byte[20];
		//TODO check if file is "legitimate"
		FileInputStream fis = new FileInputStream( file.getPath() + ".tmp" );
		int count = 0;
		while(count < hash.length)
			count += fis.read(hash, count, hash.length - count);
		RemoteSharedFile sharedFile = searchByHash( hash );
		startDownload( sharedFile, file.getPath() , callback);
	}
	
	public void resumeAllDownloads( Map<File,DownloadCallback> files ) throws IOException, IllegalStateException, GeneralSecurityException, ClassNotFoundException{
		for( File file : files.keySet() ){
			resumeDownload( file, files.get( file ) );
		}
	}
	
	public void closeConnection() throws IllegalStateException, GeneralSecurityException, IOException, ClassNotFoundException {
		
		if(secureChannel != null){
			if(secureChannel.isConnected()){
				secureChannel.getOutputStream().write( Request.CLOSE_CONN );
			}
			secureChannel.close();
			secureChannel = null;
		}
		
	}

	// GETTER & SETTER
	public boolean isConnected() {
		return supernode != null;
	}


	public Set<LocalSharedFile> getFileList() {
		return fileList;
	}
	
	public Set<IncompleteSharedFile> getIncompleteFiles() {
		return incompleteFiles;
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

	public NodeInfo getSuperNode() {
		return supernode;
	}


}