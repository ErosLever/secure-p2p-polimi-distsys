package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.LocalSharedFile;
import polimi.distsys.sp2p.containers.SharedFile;

public class DisplayedWindow extends JFrame {


	private SimpleNode sn;
	private final static String newline = "\n";
	private final static String genericSecError = "Operazione non riuscita! c'è stato un problema di sicurezza!" + newline;
	private final static String genericComError = "Operazione non riuscita! c'è stato un problema di comunicazione!" + newline;
	private final static String notConnectstate = "Devi essere connesso per fare questa operazione!" + newline;
	private JPanel contentPane;
	private JLabel statusLabel;

	//TAB
	private JTabbedPane tabbedPane;
	private JPanel tabRicerca;
	private JPanel tabDownload;

	//TAB PUBLISH
	private JPanel tabListaFile;
	private JButton publishButton;
	private JButton unpublishButton;
	private JScrollPane fileScrollPane;
	
	//PUBLISH LIST
	private DefaultListModel model;
	private JList fileVisualizationList;
	private HashMap<String, LocalSharedFile> visualizedFiles;

	//MENU
	private JMenuBar menuBar;
	private JMenu menuAzioni;
	private JMenuItem joinButton;
	private JMenuItem leaveButton;
	private JMenuItem exitButton;

	//CONSOLE
	private JPanel panel;
	private JTextArea console;
	private JScrollPane scrollPane;
	private JLabel consoleTitle;

	// File chooser
	private final JFileChooser fc;
	private JPanel innerContainer;


	/**
	 * Create the frame.
	 */
	public DisplayedWindow(SimpleNode simple) {

		//prendo il riferimento al nodo da visualizzare
		this.sn = simple;

		//init file chooser
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);



		//GENERAL CONTAINER
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 622, 478);
		contentPane = new JPanel();

		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		model = new DefaultListModel();





		// MENU
		menuBar = new JMenuBar();
		menuBar.setBackground(SystemColor.menu);
		contentPane.add(menuBar, BorderLayout.NORTH);

		menuAzioni = new JMenu("Azioni");
		menuBar.add(menuAzioni);

		joinButton = new JMenuItem("Join");
		joinButton.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent a)
            {
				try {
					
					
					sn.join();
					if (sn.isConnected()) {
						console.append("Connessione riuscita con successo, sei connesso al supernodo: " 
								+ sn.getSuperNode().getAddress().getHostName() + newline);
						statusLabel.setText("STATUS: CONNESSO @ " + sn.getSuperNode().getAddress().getHostName() + newline);
					} else {
						console.append("La connessione non è andata a buon fine." + newline);
					}



				} catch (IllegalStateException e) {
					console.append("Sei già connesso! Non puoi effettuare questa operazione!" + newline);


				} catch (GeneralSecurityException e) {
					console.append(genericSecError);

				} catch (IOException e) {
					console.append(genericComError);

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		});

		menuAzioni.add(joinButton);

		leaveButton = new JMenuItem("Leave");
		menuAzioni.add(leaveButton);
		leaveButton.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent a)
            {

				try {
					sn.leave();
					if (!sn.isConnected()) {
						console.append("Disconnessione avvenuta con successo" + newline);
						statusLabel.setText("STATUS: DISCONNESSO" + newline);
					} else {
						console.append("Problema nella disconnessione." + newline);
					}

				} catch (IllegalStateException e) {
					console.append("Non sei connesso! Non puoi effettuare questa operazione!" + newline);

				} catch (GeneralSecurityException e) {
					console.append(genericSecError);

				} catch (IOException e) {
					if(!e.getMessage().isEmpty())
						console.append(e.getMessage() + newline);
					console.append(genericComError);
				} catch (ClassNotFoundException e) {
				
				} 
			}
		});

		exitButton = new JMenuItem("Close");
		menuAzioni.add(exitButton);

		exitButton.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent e)
            {

				System.exit(0);

			}

		});



		// Pannello Console

		panel = new JPanel();
		panel.setBorder(UIManager.getBorder("InsetBorder.aquaVariant"));
		contentPane.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));

		console = new JTextArea();
		console.setEditable(false);
		console.setRows(8);
		
		scrollPane = new JScrollPane(console);
		panel.add(scrollPane);


		consoleTitle = new JLabel("Console:");
		consoleTitle.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		panel.add(consoleTitle, BorderLayout.NORTH);

		innerContainer = new JPanel();
		contentPane.add(innerContainer, BorderLayout.CENTER);
		innerContainer.setLayout(new BorderLayout(0, 0));



		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		innerContainer.add(tabbedPane, BorderLayout.NORTH);

		tabRicerca = new JPanel();
		tabbedPane.addTab("Search", null, tabRicerca, null);

		tabDownload = new JPanel();
		tabbedPane.addTab("Downloads", null, tabDownload, null);


		// TAB PUBLISH
		tabListaFile = new JPanel();
		tabbedPane.addTab("File", null, tabListaFile, null);

		tabListaFile.setLayout(new BorderLayout(0, 0));

		publishButton = new JButton("Condividi");
		publishButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				int retValue = fc.showOpenDialog(tabListaFile);

				if (retValue == JFileChooser.APPROVE_OPTION) {

					File file = null;
					file = fc.getSelectedFile();

					try {
						sn.publish(file.getAbsoluteFile());
						console.append("ho aggiunto il file: " + file.getName() + newline);
						refreshFileList();
						
						

					} catch (IOException e) {
						console.append(genericComError);
					} catch (GeneralSecurityException e) {
						console.append(genericSecError);
					} catch (IllegalStateException e) {
						console.append(notConnectstate);
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
				Object selected = fileVisualizationList.getSelectedValue();
				if (selected != null) {
					
					//recupero l'oggetto dal nome
					LocalSharedFile tmp = visualizedFiles.get(selected);
					Set<LocalSharedFile> tmpSet = new HashSet<LocalSharedFile>();
					tmpSet.add(tmp);
					
					try {
						sn.unpublish(tmpSet);
						console.append("ho rimosso il file: " + selected + newline);
						refreshFileList();
						
					} catch (IOException e) {
						console.append(genericComError);
						
					} catch (GeneralSecurityException e) {
						console.append(genericSecError);
						
					} catch (IllegalStateException e) {
						console.append(notConnectstate);
						
					} catch (ClassNotFoundException e) {
						
					} 
				} else {
					
					new JPopupMenu("Devi selezionare un oggetto!");
				}
			}
			});

		JPanel groupButton = new JPanel();
		groupButton.add(publishButton);
		groupButton.add(unpublishButton);
		fileVisualizationList = new JList(model);
		fileScrollPane = new JScrollPane(fileVisualizationList);


		tabListaFile.add(fileScrollPane, BorderLayout.CENTER);
		tabListaFile.add(groupButton, BorderLayout.NORTH);

		//STATUS BAR
		statusLabel = new JLabel("STATUS: DISCONNESSO" + newline);
		innerContainer.add(statusLabel, BorderLayout.SOUTH);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setFont(new Font("Lucida Grande", Font.BOLD, 15));

	}

	private void refreshFileList() {
		
		visualizedFiles = new HashMap<String, LocalSharedFile>();
		model.removeAllElements();
		
		for(LocalSharedFile sf: sn.getFileList()) {
			visualizedFiles.put(sf.getFile().getName(), sf);
			model.addElement(sf.getFile().getName());
		}
		
		

	}
}
