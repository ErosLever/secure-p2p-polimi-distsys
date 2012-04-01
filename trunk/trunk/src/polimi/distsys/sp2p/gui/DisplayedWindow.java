package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import polimi.distsys.sp2p.SimpleNode;

public class DisplayedWindow extends JFrame {


	private static final long serialVersionUID = -39050633494230802L;

	private SimpleNode sn;

	final static String newline = "\n";
	final static String genericSecError = "Operazione non riuscita! c'è stato un problema di sicurezza!" + newline;
	final static String genericComError = "Operazione non riuscita! c'è stato un problema di comunicazione!" + newline;
	final static String notConnectstate = "Devi essere connesso per fare questa operazione!" + newline;


	private JPanel contentPane;
	private JPanel innerContainer;

	private Console consolePanel;
	private JLabel statusLabel;
	private JTextArea console;

	//TAB
	private JTabbedPane tabbedPane;
	private SearchTab tabRicerca;
	private DownloadTab tabDownload;
	private FilesTab tabListaFile;

	//MENU
	private Menu menuBar;


	/**
	 * Create the frame.
	 */
	public DisplayedWindow(SimpleNode simple) {

		//prendo il riferimento al nodo da visualizzare
		this.sn = simple;


		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 622, 478);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		//PANNELLO USATO PER ORGANIZZARE I COMPONENTI
		innerContainer = new JPanel();
		contentPane.add(innerContainer, BorderLayout.CENTER);
		innerContainer.setLayout(new BorderLayout(0, 0));

		consolePanel = new Console();
		contentPane.add(consolePanel, BorderLayout.SOUTH);
		console = consolePanel.getConsole();

		//STATUS BAR
		statusLabel = new JLabel("STATUS: DISCONNESSO" + newline);
		innerContainer.add(statusLabel, BorderLayout.SOUTH);
		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
		statusLabel.setFont(new Font("Lucida Grande", Font.BOLD, 15));

		// MENU
		menuBar = new Menu(sn,console,statusLabel);
		contentPane.add(menuBar, BorderLayout.NORTH);
	
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		innerContainer.add(tabbedPane, BorderLayout.NORTH);
		
		//Tabs
		
		tabDownload = new DownloadTab(sn,console);
		tabbedPane.addTab("Downloads", null, tabDownload, null);
		tabListaFile = new FilesTab(sn,console);
		tabbedPane.addTab("File", null, tabListaFile, null);
		tabRicerca = new SearchTab(sn,console, tabDownload);
		tabbedPane.addTab("Search", null, tabRicerca, null);
		
	}

}
