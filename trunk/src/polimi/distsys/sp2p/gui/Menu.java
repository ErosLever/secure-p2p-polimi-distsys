package polimi.distsys.sp2p.gui;

import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;

import polimi.distsys.sp2p.SimpleNode;

public class Menu extends JMenuBar{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JFileChooser dc;
	private SimpleNode sn;
	private JTextArea console;
	private JLabel statusLabel;
	
	private JMenu menuAzioni;
	private JMenu menuImpostazioni;
	private JMenuItem joinButton;
	private JMenuItem leaveButton;
	private JMenuItem exitButton;
	private JMenuItem directoryButton;
	

	public Menu(SimpleNode s, JTextArea c, JLabel status) {
		
		this.statusLabel = status;
		this.sn = s;
		this.console = c;

		dc = new JFileChooser();
		dc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		// MENU
		
		setBackground(SystemColor.menu);
		

		menuAzioni = new JMenu("Azioni");
		add(menuAzioni);

		menuImpostazioni = new JMenu("Impostazioni");
		add(menuImpostazioni);

		joinButton = new JMenuItem("Join");
		joinButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent a)
			{
				try {


					sn.join();
					if (sn.isConnected()) {
						console.append("Connessione riuscita con successo, sei connesso al supernodo: " 
								+ sn.getSuperNode().getAddress().getHostName() + DisplayedWindow.newline);
						statusLabel.setText("STATUS: CONNESSO @ " + sn.getSuperNode().getAddress().getHostName() + DisplayedWindow.newline);
					} else {
						console.append("La connessione non è andata a buon fine." + DisplayedWindow.newline);
					}



				} catch (IllegalStateException e) {
					console.append("Sei già connesso! Non puoi effettuare questa operazione!" + DisplayedWindow.newline);


				} catch (GeneralSecurityException e) {
					console.append(DisplayedWindow.genericSecError);

				} catch (IOException e) {
					console.append(DisplayedWindow.genericComError);

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
						console.append("Disconnessione avvenuta con successo" + DisplayedWindow.newline);
						statusLabel.setText("STATUS: DISCONNESSO" + DisplayedWindow.newline);
					} else {
						console.append("Problema nella disconnessione." + DisplayedWindow.newline);
					}

				} catch (IllegalStateException e) {
					console.append("Non sei connesso! Non puoi effettuare questa operazione!" + DisplayedWindow.newline);

				} catch (GeneralSecurityException e) {
					console.append(DisplayedWindow.genericSecError);

				} catch (IOException e) {
					if(!e.getMessage().isEmpty())
						console.append(e.getMessage() + DisplayedWindow.newline);
					console.append(DisplayedWindow.genericComError);
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
				//TODO TERMINARE IN MANIERA CLEAN

			}

		});

		directoryButton = new JMenuItem("Directory...");
		menuImpostazioni.add(directoryButton);
		directoryButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				int retValue = dc.showOpenDialog(Menu.this);

				if (retValue == JFileChooser.APPROVE_OPTION) {

					File file = dc.getSelectedFile();
					sn.setDownloadDirectory(file);
					console.append("Directory per i download impostata:" + file.getPath() + DisplayedWindow.newline);
				}

			}
		});



	}
}
