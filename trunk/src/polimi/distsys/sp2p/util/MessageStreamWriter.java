/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eros
 */
public class MessageStreamWriter {

    private final SocketChannel sc;

    public MessageStreamWriter(SocketChannel sc){
        this.sc = sc;
    }

    public void writeMessage(Object o) {
        try {
            sc.configureBlocking(false);
            byte[] serialized = Serializer.serialize(o);
            int size = serialized.length;
            byte[] s = new byte[Integer.SIZE/8];
            for(int pos=0;pos<Integer.SIZE/8;pos++)
                s[pos] = (byte) ((size >> 8 * pos) & 0xFF);
            ByteBuffer bb = ByteBuffer.wrap(s);
            while(bb.hasRemaining())
                sc.write(bb);
            bb = ByteBuffer.wrap(serialized);
            bb.rewind();
            while(bb.hasRemaining())
                sc.write(bb);
        } catch (IOException ex) {
            Logger.getLogger(MessageStreamWriter.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(o instanceof MessageStreamEnd){
            try {
                //System.out.println("chiuso output");
                sc.socket().shutdownOutput();
            } catch (IOException ex) {
                //Logger.getLogger(MessageStreamWriter.class.getName()).log(Level.SEVERE, null, ex);
            }
            if(sc.socket().isInputShutdown()){
                //System.out.println("chiudo il socket");
                MessageStreamEnd.closeSocketChannel(sc);
            }
        }
    }

    public void close(){
        writeMessage(new MessageStreamEnd());
    }

}
