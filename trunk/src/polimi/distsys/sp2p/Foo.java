package polimi.distsys.sp2p;

import java.io.IOException;
import java.security.GeneralSecurityException;

import polimi.distsys.sp2p.gui.DisplayedWindow;


public class Foo {


	/**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws GeneralSecurityException 
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, GeneralSecurityException {

		// controlla la correttezza dei parametri
		if( !(Integer.valueOf(args[0]) == 1 || Integer.valueOf(args[0]) == 2)) {
			System.out.print("I parametri utilizzati sono scorretti\n" +
					"utilizzare 2 per inizializzare un nodo semplice," +
					"1 per un supernodo ");
			System.exit(-1);
		}

		// INIT SUPERNODE
		if(Integer.valueOf(args[0]) == 1) {

			SuperNode.fromFile();
		}

		//INIT SIMPLENODE
		if(Integer.valueOf(args[0]) == 2) {

			DisplayedWindow frame =  new DisplayedWindow(SimpleNode.fromFile());
			frame.setVisible(true);

		}

	}

}
