/**
 * 
 */
package polimi.distsys.sp2p.gui;

import java.util.ArrayList;
import java.util.Scanner;

import polimi.distsys.sp2p.SimpleNode;
import polimi.distsys.sp2p.containers.SharedFile;

/**
 * classe che uso per 
 * @author Ale
 *
 */
public class VisualizationHandler {
	
	private ArrayList<String> offlineOpt;
	private ArrayList<String> onlineOpt;
	private Scanner scan;
	private String command;
	
	private final SimpleNode node;
	
	public VisualizationHandler(SimpleNode sn) {
		
		//init
		scan = new Scanner(System.in);
		offlineOpt = new ArrayList<String>();		
		offlineOpt.add("Connettiti alla rete"); 
		
		onlineOpt = new ArrayList<String>();
		onlineOpt.add("Metti un file in condivisione");
		onlineOpt.add("Disconnettiti dalla rete");
		
		this.node = sn;
		
		run();
	}
	
	
	private void run() {
		
		System.out.print("Benvenuto/a " + node.getUserID()+ "\n\n");
		
		while(true) {
			
			System.out.print("Opzioni disponibili: \n" + 
					"-------------------------------\n");
			
			if(node.isConnected()) {
				
				for(String s: onlineOpt) {
					System.out.println(Integer.valueOf(onlineOpt.indexOf(s))+ ") "+ s);
				}
			}
			else {
				for(String s: offlineOpt) {
					System.out.println(Integer.valueOf(onlineOpt.indexOf(s))+ ") "+ s);
					
				}
			}
			
			System.out.print("-------------------------------\n\n");
			command = scan.nextLine();
			
			try {
				
				commandChooser(Integer.valueOf(command)); 
				
			} catch(NumberFormatException e) {
				
				System.out.print("Inserire il numero del comando!!\n\n\n");
			}
			
		}
		
	}
		
		/**
		 * visualizza la lista dei file condivisi
		 */
		@SuppressWarnings("unused")
		private void visualizeSharedFile() {

			for( SharedFile sf: node.getFileList()) {

				System.out.println("Nome file: " + sf.getName() + ", path: " + sf.getPath());
			}
		}
		
		private void commandChooser(int choice) {
			
			//non posso fare uno switch su una stringa
			switch(choice) {
			case 0: {
				System.out.println("Tento di fare la join");
				//Chiamero il metodo per la join
				break;
			}
			
			default:
				System.out.print("Comando non disponibile!!\n\n\n");
			}
			
		}

}
