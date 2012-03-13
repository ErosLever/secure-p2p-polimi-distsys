/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eros
 */
public class Sender {

    public static int DEFAULT_INTERVAL = 10*1000;
    public static int DEFAULT_DELAY = 500;
    public static int SOCKET_TIMEOUT = 5*1000;
    private Timer t;
    private MyTask mt;
    private TargetGenerator targetGenerator;
    private MessageGenerator messageGenerator;
    private ConnectionFailedCallback connFailedCB;

    public Sender(TargetGenerator tg, MessageGenerator mg){
        this(tg, mg, null, DEFAULT_INTERVAL);
    }

    public Sender(TargetGenerator tg, MessageGenerator mg, ConnectionFailedCallback cfcb){
        this(tg, mg, cfcb, DEFAULT_INTERVAL);
    }

    public Sender(TargetGenerator tg, MessageGenerator mg, ConnectionFailedCallback cfcb, int interval){
        t = new Timer();
        mt = new MyTask();
        this.targetGenerator = tg;
        this.messageGenerator = mg;
        this.connFailedCB = cfcb;
        if(interval >= 0){
            this.privateStart(interval);
        }
    }

    private void privateStart(int interval){
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                Sender.this.stop();
            }
        });
        t.scheduleAtFixedRate(mt, DEFAULT_DELAY, interval);
    }

    public void start(int interval){
        this.privateStart(interval);
    }

    class MyTask extends TimerTask {

        @Override
        public void run() {
            byte[] msg;
            try {
                msg = messageGenerator.generateMessage();
                Set<InetSocketAddress> targets = targetGenerator.getTargetsList();
                for(InetSocketAddress isa : targets){
                    try{
                        //System.out.println("Messaggio con "+isa);
                        Socket s = new Socket();
                        //s.setSoTimeout(SOCKET_TIMEOUT);
                        s.connect(isa,SOCKET_TIMEOUT);
                        s.getOutputStream().write(msg);
                        s.getOutputStream().close();
                        try{
                            s.getInputStream().close();
                        }catch(IOException e){}
                        try{
                            s.close();
                        }catch(IOException e){}
                    }catch(IOException e){
                        if(connFailedCB != null)
                            connFailedCB.connectionFailed(isa);
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void stop(){
        mt.cancel();
        t.cancel();
        t.purge();
    }
    
    public interface MessageGenerator {
        public byte[] generateMessage(Object... args);
    }

    public interface TargetGenerator {
        public Set<InetSocketAddress> getTargetsList(Object... args);
    }

    public interface ConnectionFailedCallback {
        public void connectionFailed(InetSocketAddress addr);
    }

}
