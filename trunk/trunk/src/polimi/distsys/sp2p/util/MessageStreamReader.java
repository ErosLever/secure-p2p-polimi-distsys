/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eros
 */
public class MessageStreamReader implements Runnable {

    public static int TIMEOUT = 20000;

    private final SocketChannel sc;
    private final Selector s;
    private final MessageReceivedCallback mrc;
    private boolean stopped = true;
    private final int timeout;

    public MessageStreamReader(SocketChannel sc, MessageReceivedCallback mrc) throws IOException{
        this(sc, mrc, TIMEOUT);
    }

    public MessageStreamReader(SocketChannel sc, MessageReceivedCallback mrc, int timeout) throws IOException{
        this.sc = sc;
        sc.configureBlocking(false);
        s = Selector.open();
        sc.register(s, SelectionKey.OP_READ);
        this.mrc = mrc;
        this.timeout = timeout;
    }

    @Override
    public void run(){
        stopped = false;
        boolean newMessage = true;
        boolean readingSize = false;
        boolean readingSerialization = false;
        ByteBuffer bb = null;
        byte[] buf = null;
        int lastRead = 1;
        while(!stopped && sc.isOpen() && lastRead != -1){
            try {
                s.select(timeout);
                if( s.selectedKeys().isEmpty())
                    break;
                Iterator<SelectionKey> it = s.selectedKeys().iterator();
                it.next();
                it.remove();
                if(newMessage){
                    newMessage = false;
                    readingSize = true;
                    buf = new byte[Integer.SIZE/8];
                    bb = ByteBuffer.wrap(buf);
                }
                if(readingSize){
                    while(bb.remaining()>0 && sc.isOpen()){
                        if((lastRead = sc.read(bb)) <= 0)
                            break;
                    }
                    if(!bb.hasRemaining()){
                        readingSize = false;
                        bb.rewind();
                        int size = 0;
                        for(int i=0;i<Integer.SIZE/8;i++)
                            size += (buf[i]&0xFF) << 8*i;
                        buf = new byte[size];
                        bb = ByteBuffer.wrap(buf);
                        readingSerialization = true;
                    }
                }
                if(readingSerialization){
                    while(bb.remaining()>0 && sc.isOpen()){
                        if((lastRead = sc.read(bb)) <= 0)
                            break;
                    }
                    if(!bb.hasRemaining()){
                        readingSerialization = false;
                        newMessage = true;
                        bb.rewind();
                        Object o = Serializer.deserialize(buf, Object.class);
                        if(o instanceof MessageStreamEnd)
                            break;
                        System.out.println("Received: "+o);
                        mrc.messageReceived(o);
                    }
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(MessageStreamReader.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(MessageStreamReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            //System.out.println("chiuso input");
            sc.socket().shutdownInput();
        } catch (IOException ex) {
            //Logger.getLogger(MessageStreamReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            s.close();
        } catch (IOException ex) {
            Logger.getLogger(MessageStreamReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        if(sc.socket().isOutputShutdown()){
            //System.out.println("chiudo il socket");
            MessageStreamEnd.closeSocketChannel(sc);
        }
        stopped = true;
    }

    public void stop(){
        stopped = true;
    }

    public interface MessageReceivedCallback {

        public void messageReceived(Object o);

    }

}
