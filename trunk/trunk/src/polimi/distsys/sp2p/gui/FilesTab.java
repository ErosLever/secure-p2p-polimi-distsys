package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.LocalSharedFile;

public class FilesTab extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JButton publishButton;
	private JButton unpublishButton;
	private JScrollPane fileScrollPane;

	private SimpleNode sn;
	private JTextArea console;

	//PUBLISH LIST
	private DefaultTableModel fileModel;
	private JTable fileVisualizationTable;
	private HashMap<Integer, LocalSharedFile> visualizedFiles;

	private JFileChooser fc;


	@SuppressWarnings("serial")
	public FilesTab(SimpleNode s, JTextArea c) {

		//INIT
		this.sn = s;
		this.console = c;
		setLayout(new BorderLayout(0, 0));

		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

		//TABLE MODEL
		fileModel = new DefaultTableModel(){

			@Override
			public boolean isCellEditable(int row, int column) {
				//all cells false
				return false;
			} };

			publishButton = new JButton("Condividi");
			publishButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					int retValue = fc.showOpenDialog(FilesTab.this);

					if (retValue == JFileChooser.APPROVE_OPTION) {

						File file = null;
						file = fc.getSelectedFile();

						try {

							sn.publish(file.getAbsoluteFile());
							if(file.isFile()) 
								console.append("ho aggiunto il file: " + file.getName() +DisplayedWindow.newline);
							if(file.isDirectory())
								console.append("ho aggiunto il contenuto della directory: " + file.getName() +DisplayedWindow.newline);
							refreshFileList();


						} catch (IOException e) {
							console.append(DisplayedWindow.genericComError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() + DisplayedWindow.newline);
						} catch (GeneralSecurityException e) {
							console.append(DisplayedWindow.genericSecError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() +DisplayedWindow.newline);
						} catch (IllegalStateException e) {
							console.append(DisplayedWindow.notConnectstate);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() +DisplayedWindow.newline);
						} catch (ClassNotFoundException e) {

						}

					}

				}
			});

			unpublishButton = new JButton("Rimuovi");
			unpublishButton.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					//recupera l'oggetto selezionato
					int selected = fileVisualizationTable.getSelectedRow();
					if (selected != -1) {

						//recupero l'oggetto dal nome
						LocalSharedFile tmp = visualizedFiles.get(selected);
						Set<LocalSharedFile> tmpSet = new HashSet<LocalSharedFile>();
						tmpSet.add(tmp);

						try {
							sn.unpublish(tmpSet);
							console.append("ho rimosso il file: " + tmp.getFile().getName() +DisplayedWindow.newline);
							refreshFileList();

						} catch (IOException e) {
							console.append(DisplayedWindow.genericComError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() +DisplayedWindow.newline);

						} catch (GeneralSecurityException e) {
							console.append(DisplayedWindow.genericSecError);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() +DisplayedWindow.newline);

						} catch (IllegalStateException e) {
							console.append(DisplayedWindow.notConnectstate);
							if(!e.getMessage().isEmpty())
								console.append(e.getMessage() +DisplayedWindow.newline);

						} catch (ClassNotFoundException e) {

						} 
					} else {

						new JPopupMenu("Devi selezionare un oggetto!");
					}
				}
			});

			//needed for layout reason
			JPanel groupButton = new JPanel();
			groupButton.add(publishButton);
			groupButton.add(unpublishButton);

			fileVisualizationTable = new JTable(fileModel);

			fileVisualizationTable.setRowSelectionAllowed(true);
			fileVisualizationTable.setColumnSelectionAllowed(false);
			fileVisualizationTable.setCellSelectionEnabled(false);

			fileModel.addColumn("Nome");
			fileModel.addColumn("Path");
			fileScrollPane = new JScrollPane(fileVisualizationTable);


			this.add(fileScrollPane, BorderLayout.CENTER);
			this.add(groupButton, BorderLayout.NORTH);


	}

	private void refreshFileList() {

		visualizedFiles = new HashMap<Integer, LocalSharedFile>();

		//cancella la tabella
		while (fileModel.getRowCount()>0){
			fileModel.removeRow(0);
		}

		int counter = 0;
		for(LocalSharedFile sf: sn.getFileList()) {
			visualizedFiles.put(counter, sf);
			fileModel.addRow(new Object[]{sf.getFile().getName(),sf.getFile().getPath()});
			counter++;
		}

	}

}
