package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.IncompleteSharedFile;
import polimi.distsys.sp2p.containers.NodeInfo;
import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.SharedFile;
import polimi.distsys.sp2p.handlers.DownloadHandler.DownloadCallback;

public class SearchTab extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SimpleNode sn;
	private JTextArea console;

	private JTextField searchQuery;
	private JButton searchButton;
	private JPanel groupSearch;
	private JScrollPane searchScrollPane;

	//SEARCH TABLE
	private DefaultTableModel searchModel;
	private HashMap<Integer, RemoteSharedFile> searchedFiles;
	private JTable searchTable;

	public SearchTab(SimpleNode s, JTextArea c) {

		//INIT
		this.sn = s;
		this.console = c;

		searchModel = new DefaultTableModel(){

			private static final long serialVersionUID = -714736532354862439L;

			@Override
			public boolean isCellEditable(int row, int column) {
				//all cells false
				return false;
			} };


			this.setLayout(new BorderLayout(0, 0));

			searchQuery = new JTextField();
			searchQuery.setColumns(25);
			searchButton = new JButton("Cerca");
			searchButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {

					String query = searchQuery.getText();

					if(!query.isEmpty()) {

						console.append("Inizio la ricerca per la query:" + query + DisplayedWindow.newline);

						try {

							List<RemoteSharedFile> tmp = sn.search(query);
							refreshSearch(tmp);


						} catch (IOException e) {
							console.append(DisplayedWindow.genericComError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (GeneralSecurityException e) {
							console.append(DisplayedWindow.genericSecError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (IllegalStateException e) {
							console.append(DisplayedWindow.notConnectstate);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}

				}
			});

			searchTable = new JTable(searchModel);
			
			searchModel.addColumn("Nome");
			searchModel.addColumn("Peers");
			searchModel.addColumn("Hash");

			searchTable.setRowSelectionAllowed(true);
			searchTable.setColumnSelectionAllowed(false);
			searchTable.setCellSelectionEnabled(false);

			searchTable.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent ev) {
					if(ev.getClickCount() == 2)
					{  int value = searchTable.getSelectedRow();
					if( value != -1) {

						try {

							RemoteSharedFile rmt = sn.searchByHash(searchedFiles.get(value).getHash());
							String name = (String)searchModel.getValueAt(value, 0);
							sn.startDownload(rmt,name,(new DownloadCallback() {

								@Override
								public void receivedChunk( IncompleteSharedFile isf, int i) {
									console.append("ho ricevuto: " + String.valueOf(i) + "\n");
								}

								@Override
								public void gotException( IncompleteSharedFile isf, Exception ex) {
									console.append(ex.getMessage() + "\n");
									console.append("Ho fatto casino!" + "\n");
								}

								@Override
								public void endOfDownload(IncompleteSharedFile isf) {
									console.append("ANDIAMO A VINCERE CAZZO!!!, download completato" + "\n");
								}

								@Override
								public void askCommunicationToNode(NodeInfo node, SharedFile sharedFile)
										throws IOException, GeneralSecurityException {
									//Do nothing
								}
							}));

						} catch (IOException e) {
							console.append(DisplayedWindow.genericComError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (GeneralSecurityException e) {
							console.append(DisplayedWindow.genericSecError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (IllegalStateException e) {
							console.append(DisplayedWindow.notConnectstate);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (ClassNotFoundException e) {

						}
					}
					}
				}

			});
			
			groupSearch = new JPanel();
			groupSearch.add(searchButton);
			groupSearch.add(searchQuery);
			searchScrollPane = new JScrollPane(searchTable);
			add(groupSearch, BorderLayout.NORTH);
			add(searchScrollPane, BorderLayout.SOUTH);



	}

	private void refreshSearch(List<RemoteSharedFile> list) {

		searchedFiles = new HashMap<Integer, RemoteSharedFile>();

		while (searchModel.getRowCount()>0){
			searchModel.removeRow(0);
		}


		int counter = 0;
		for(RemoteSharedFile rsf: list) {
			for(String name: rsf.getFileNames()) {
				searchedFiles.put(counter, rsf);
				searchModel.addRow(new Object[] { 
						name, rsf.getPeers(), rsf.getHash().toString()});

			}

		}	

	}


}