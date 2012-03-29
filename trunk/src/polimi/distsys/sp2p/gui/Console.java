package polimi.distsys.sp2p.gui;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

public class Console  extends JPanel{
	
	private JTextArea console;
	private JScrollPane scrollPane;
	private JLabel consoleTitle;
	
	
	public Console() {
		
		this.setBorder(UIManager.getBorder("InsetBorder.aquaVariant"));
		this.setLayout(new BorderLayout(0, 0));

		console = new JTextArea();
		console.setEditable(false);
		console.setRows(8);
		
		scrollPane = new JScrollPane(console);
		add(scrollPane);


		consoleTitle = new JLabel("Console:");
		consoleTitle.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		add(consoleTitle, BorderLayout.NORTH);
		
		this.setVisible(true);
		this.validate();

		
		
	}
	
	public JTextArea getConsole() {
		return console;
	}

}
