/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eros
 */
public class MessageInputStream {

    private final InputStream wrapped;

    public MessageInputStream(InputStream wrapped) throws IOException {
        this.wrapped = wrapped;
    }

    public synchronized Object readMessage(){
        try {
            //if (wrapped.available() >= Integer.SIZE/8) {
            if(true){
                // Get the size of the serialized object
                int size = 0;
                for(int pos=0;pos<Integer.SIZE/8;pos++)
                    size += wrapped.read() << 8*pos;
                // read from input stream
                byte[] buf = new byte[size];
                int start = 0;
                while(start < size)
                    start += wrapped.read(buf, start, size-start);
                ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                ObjectInputStream ois = new ObjectInputStream(bais);
                Object o = ois.readObject();
                return o;
            }
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(MessageInputStream.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(MessageInputStream.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static void main(String[] args) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageOutputStream mos = new MessageOutputStream(baos);
        mos.writeMessage("Test");
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        MessageInputStream mis = new MessageInputStream(bais);
        System.out.println( mis.readMessage() );
    }

}
