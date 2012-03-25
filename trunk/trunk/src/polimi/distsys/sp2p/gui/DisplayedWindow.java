package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import polimi.distsys.sp2p.SimpleNode;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class DisplayedWindow extends JFrame {

	
	private SimpleNode sn;
	private final static String newline = "\n";
	
	private JPanel contentPane;
	private JLabel statusLabel;
	
	//TAB
	private JTabbedPane tabbedPane;
	private JScrollPane tabRicerca;
	private JScrollPane tabDownload;
	private JScrollPane tabListaFile;
	
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
	
	/*
	 * Codice da mettere nel main
	 * DisplayedWindow frame = new DisplayedWindow(SIMPLENODE);
	 	frame.setVisible(true);
	 * 
	 */

	/**
	 * Create the frame.
	 */
	public DisplayedWindow(SimpleNode simple) {
		
		//prendo il riferimento al nodo da visualizzare
		this.sn = simple;
		
		

		//GENERAL CONTAINER
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 622, 478);
		contentPane = new JPanel();
		
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		//STATUS BAR
		statusLabel = new JLabel("STATUS: DISCONNESSO" + newline);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setFont(new Font("Lucida Grande", Font.BOLD, 15));
		contentPane.add(statusLabel, BorderLayout.NORTH);
		
		
		// TABBED WINDOWS
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		
		tabRicerca = new JScrollPane();
		tabbedPane.addTab("Search", null, tabRicerca, null);
		
		tabDownload = new JScrollPane();
		tabbedPane.addTab("Downloads", null, tabDownload, null);
		
		tabListaFile = new JScrollPane();
		tabbedPane.addTab("Files", null, tabListaFile, null);
		
		
		
		// MENU
		menuBar = new JMenuBar();
		contentPane.add(menuBar, BorderLayout.NORTH);
		
		menuAzioni = new JMenu("Azioni");
		menuBar.add(menuAzioni);
		
		joinButton = new JMenuItem("Join");
		joinButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
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
					console.append("Operazione non riuscita! c'è stato un problema di sicurezza!" + newline);
					
				} catch (IOException e) {
					console.append("Operazione non riuscita! c'è stato un problema di comunicazione!" + newline);

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		});
		
		menuAzioni.add(joinButton);
		
		leaveButton = new JMenuItem("Leave");
		menuAzioni.add(leaveButton);
		leaveButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
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
					console.append("Operazione non riuscita! c'è stato un problema di sicurezza!" + newline);
					
				} catch (IOException e) {
					console.append("Operazione non riuscita! c'è stato un problema di comunicazione!" + newline);
				} 
			}
		});
		
		exitButton = new JMenuItem("Close");
		menuAzioni.add(exitButton);
		
		exitButton.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent arg0) {
				
				System.exit(0);
				
			}
			
		});
		
		
		
		// Pannello Console
		
		panel = new JPanel();
		panel.setBorder(UIManager.getBorder("InsetBorder.aquaVariant"));
		contentPane.add(panel, BorderLayout.SOUTH);
		panel.setLayout(new BorderLayout(0, 0));
		
		console = new JTextArea();
		console.setRows(8);
		
		scrollPane = new JScrollPane(console);
		panel.add(scrollPane);
		
		
		consoleTitle = new JLabel("Console:");
		consoleTitle.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		panel.add(consoleTitle, BorderLayout.NORTH);
		
	}
}
