
package polimi.distsys.sp2p.handlers;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import polimi.distsys.sp2p.containers.RemoteSharedFile;
import polimi.distsys.sp2p.containers.SharedFile;

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
	public static List<RemoteSharedFile> localSearch(String query, List<RemoteSharedFile> list) {

		List<RemoteSharedFile> result = new Vector<RemoteSharedFile>();
		
		for( RemoteSharedFile f : list ){
			if( matchQuery(f, query) ){
				if( ! result.contains( f ) ){
					result.add( f );
					
				}else{
					RemoteSharedFile mine = result.get( result.indexOf( f ) );
					mine.merge( f );
				}
			}
		}
		
		return result;

	}
	
	/**
	 * Unisce due liste di file condivisi controllando eventuali duplicati
	 * 
	 * @param list
	 * @param set
	 * @return
	 */
	public static List<RemoteSharedFile> mergeLists(List<RemoteSharedFile> list, Set<RemoteSharedFile> set){ 

		//Aggiunge i file alla lista se non ci sono
		for(RemoteSharedFile f : set) {
			if(!list.contains(f))
				list.add(f);
			else {
				// se il file è già presente allora lo aggiunge al numero dei peers
				RemoteSharedFile tmp = list.get(list.indexOf(f));
				tmp.merge(f);
			}
		}
		return list;

	}
	
	/**
	 * 
	 * @param sf
	 * @param query
	 * @return
	 */
	private static boolean matchQuery( SharedFile sf, String query ){
		
		//più criptico nn lo potevi fare? xD
		List<String> tokens = Arrays.asList( query.split(" ") );
		for( String name : sf.getFileNames() ){
			List<String> pieces = Arrays.asList( name.split(" " ) );
			pieces.retainAll( tokens );
			if( pieces.size() > 0 ){
				return true;
			}
		}
		return false;
	}
}
