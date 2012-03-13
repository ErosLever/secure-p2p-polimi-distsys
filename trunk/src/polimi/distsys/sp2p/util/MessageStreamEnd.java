/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.SocketChannel;

/**
 *
 * @author eros
 */
public class MessageStreamEnd implements Serializable {

	private static final long serialVersionUID = -7844681686975835504L;

	public static void closeSocketChannel(SocketChannel sc){
        try{
            sc.socket().shutdownInput();
        }catch(IOException e){}
        try{
            sc.socket().getInputStream().close();
        }catch(IOException e){}
        try{
            sc.socket().getOutputStream().close();
        }catch(IOException e){}
        try{
            sc.socket().shutdownOutput();
        }catch(IOException e){}
        try{
            sc.socket().close();
        }catch(IOException e){}
        try{
            sc.close();
        }catch(IOException e){}
    }
}
