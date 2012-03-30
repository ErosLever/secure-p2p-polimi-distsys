package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.IncompleteSharedFile;



public class DownloadTab extends JPanel {
	
	private static final long serialVersionUID = 642389033105767922L;
	
	@SuppressWarnings("unused")
	private SimpleNode sn;
	@SuppressWarnings("unused")
	private JTextArea console;
	
	private JButton pauseButton;
	private JButton stopButton;
	private JPanel groupDownload;
	private JScrollPane downloadScrollPane;

	// DOWNLOAD TABLE
	private DefaultTableModel downloadModel;
	@SuppressWarnings("unused")
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
			stopButton = new JButton("Cancella");

			groupDownload = new JPanel();
			groupDownload.add(pauseButton);
			groupDownload.add(stopButton);

			downloadTable = new JTable(downloadModel);
			downloadScrollPane = new JScrollPane(downloadTable);
			add(groupDownload,BorderLayout.NORTH);
			add(downloadScrollPane, BorderLayout.CENTER);

		
	}

}
