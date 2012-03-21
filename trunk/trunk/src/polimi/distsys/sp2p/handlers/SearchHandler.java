
package polimi.distsys.sp2p.handlers;

import java.util.ArrayList;

import polimi.distsys.sp2p.containers.RemoteSharedFile;

/**
 * @author Ale
 * 
 * Classe contenitore per i metodi utilizzati per effettuare le ricerche
 */
public class SearchHandler {

	/**
	 * implementazione banale guardando se le stringhe che contengono la parola cercata
	 * @param query
	 * @param list
	 * @return
	 */
	public static ArrayList<RemoteSharedFile> localSearch(String query, ArrayList<RemoteSharedFile> list) {

		ArrayList<RemoteSharedFile> search = new ArrayList<RemoteSharedFile>();

		for(RemoteSharedFile rf: list) {
			if (rf.getName().contains(query))
				search.add(rf);
		}

		return search;

	}
	
	/**
	 * Unisce due liste di file condivisi controllando eventuali duplicati
	 * 
	 * @param firstList
	 * @param secondList
	 * @return
	 */
	public static ArrayList<RemoteSharedFile> mergeLists(ArrayList<RemoteSharedFile> firstList, ArrayList<RemoteSharedFile> secondList){ 
	
		ArrayList<RemoteSharedFile> mergedList = new ArrayList<RemoteSharedFile>();
		mergedList.addAll(firstList);
		
		// controllo duplicati
		boolean found;
		
		for(RemoteSharedFile fileLista1 : secondList) {
			found = false;
			
			for (RemoteSharedFile fileLista2 : mergedList) {
				
				if (fileLista1.getName().equals(fileLista2.getName()) &&
						fileLista1.getHash().equals(fileLista2.getHash()) &&
								fileLista1.getIp().equals(fileLista2.getIp())) {
					
					found = true;
					break;
				}
				
			}
			
			if(!found) {
				mergedList.add(fileLista1);
			}
			
		}
		
		return mergedList;
	}
}
