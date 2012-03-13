/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package polimi.distsys.sp2p.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author eros
 */
public class Scheduler {

    public static int DEFAULT_DELAY = 500;

    private final Timer t;
    private final TimerTask tt;

    public Scheduler(final Callback cb, final int interval){
        this(cb, DEFAULT_DELAY, interval);
    }

    public Scheduler(final Callback cb, final int delay, final int interval){
        t = new Timer();
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run(){
                cancel();
            }
        });
        tt = new TimerTask(){
            @Override
            public void run() {
                cb.callback();
            }
        };
        t.scheduleAtFixedRate(tt, delay, interval);
    }

    public void cancel(){
        tt.cancel();
        t.cancel();
    }


    public interface Callback {
        public void callback();
    }
}
