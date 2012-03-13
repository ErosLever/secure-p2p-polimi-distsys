/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eros
 */
public class MessageOutputStream {

    private OutputStream wrapped;

    public MessageOutputStream(OutputStream wrap){
        wrapped = wrap;
    }

    public synchronized void writeMessage(Object o){
        try {
            byte[] serialized = Serializer.serialize(o);
            int size = serialized.length;
            byte[] s = new byte[Integer.SIZE/8];
            for(int pos=0;pos<Integer.SIZE/8;pos++)
                s[pos] = (byte) ((size >> 8 * pos) % 0xFF);
            wrapped.write(s);
            wrapped.write(serialized);
            wrapped.flush();
        } catch (IOException ex) {
            Logger.getLogger(MessageOutputStream.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
