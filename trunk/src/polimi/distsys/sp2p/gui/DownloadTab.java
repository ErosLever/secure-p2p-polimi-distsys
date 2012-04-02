package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.IncompleteSharedFile;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.SharedFile;
import polimi.distsys.sp2p.handlers.DownloadHandler;
import polimi.distsys.sp2p.handlers.DownloadHandler.DownloadCallback;



public class DownloadTab extends JPanel {

	private static final long serialVersionUID = 642389033105767922L;


	private SimpleNode sn;
	private JTextArea console;

	private JButton pauseButton;
	private JButton resumeButton;
	private JPanel groupDownload;
	private JScrollPane downloadScrollPane;

	// DOWNLOAD TABLE
	private DefaultTableModel downloadModel;
	private HashMap<Integer,IncompleteSharedFile> downloadingFiles;
	private JTable downloadTable;

	@SuppressWarnings("serial")
	public DownloadTab(SimpleNode s, JTextArea c) {

		this.sn = s;
		this.console = c;

		downloadModel = new DefaultTableModel(){

			@Override
			public boolean isCellEditable(int row, int column) {
				//all cells false
				return false;
			} };


			setLayout(new BorderLayout(0, 0));


			pauseButton = new JButton("Pausa");

			pauseButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) { 

					IncompleteSharedFile i = downloadingFiles.get(downloadTable.getSelectedRow());
					if ( i != null) {

						try {
							sn.stopDownload(i);
							refreshDownload();
							console.append("Ho ripreso il download del file: " + i.getDestinationFile().getName());

						} catch (IOException e) {
							console.append(DisplayedWindow.genericComError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (GeneralSecurityException e) {
							console.append(DisplayedWindow.genericSecError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						}
					} 
				}
			});

			resumeButton = new JButton("Riprendi");

			resumeButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) { 

					File i = downloadingFiles.get(downloadTable.getSelectedRow()).getDestinationFile();
					if ( i != null) {
						try {
							sn.resumeDownload(i, (new DownloadCallback() {

								@Override
								public void receivedChunk( IncompleteSharedFile isf, int i) {
									refreshDownload();
								}

								@Override
								public void gotException( IncompleteSharedFile isf, Exception ex) {
									console.append(ex.getMessage() + DisplayedWindow.newline);
									console.append("Si è verificato un problema" + DisplayedWindow.newline);
									refreshDownload();
								}

								@Override
								public void endOfDownload(IncompleteSharedFile isf) {
									console.append("Download Completato:" + isf.getFileNames().iterator().next() + DisplayedWindow.newline);
									refreshDownload();
								}

								@Override
								public void askCommunicationToNode(NodeInfo node, SharedFile sharedFile)
										throws IOException, GeneralSecurityException {
									//Do nothing
								}
							}));
							
							console.append("Ho ripreso il download del file: " + i.getName());
							refreshDownload();

						} catch (IOException e) {
							console.append(DisplayedWindow.genericComError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (GeneralSecurityException e) {
							console.append(DisplayedWindow.genericSecError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (IllegalStateException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} 
				}
			});


			groupDownload = new JPanel();
			groupDownload.add(pauseButton);
			groupDownload.add(resumeButton);

			downloadTable = new JTable(downloadModel);

			downloadModel.addColumn("Nome");
			downloadModel.addColumn("%");
			downloadModel.addColumn("Status");

			downloadScrollPane = new JScrollPane(downloadTable);
			add(groupDownload,BorderLayout.NORTH);
			add(downloadScrollPane, BorderLayout.CENTER);


	}

	public void refreshDownload() {

		downloadingFiles = new HashMap<Integer, IncompleteSharedFile>();

		//cancella la tabella
		while (downloadModel.getRowCount()>0){
			downloadModel.removeRow(0);
		}

		int counter = 0;
		for(IncompleteSharedFile isf: sn.getIncompleteFiles()) {

			boolean active = false;
			DownloadHandler dh = sn.getDownHandlers().get(isf);
			if(dh != null) 
				active = dh.isActive();

			String name = isf.getDestinationFile().getName();
			String pecent = ( 10000 * isf.getChunks().count() / isf.getChunks().length() ) / 100.0 + " %";

			downloadModel.addRow(new Object[] { 
					name, pecent, active});

			downloadingFiles.put(counter, isf);
			counter++;
		}



	}	

}
