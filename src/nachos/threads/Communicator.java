package nachos.threads;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {

    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    public void speak(int word) {
        //System.out.println(KThread.currentThread().getName() + " entered with message " + word);
        lock.acquire();
        if (!noSpeaker) {
            //System.out.println(KThread.currentThread().getName() + " waiting 1");
            multiSpeaker.sleep();
            //System.out.println(KThread.currentThread().getName() + " done waiting 1");
        }
        noSpeaker = false;
        message = word;
        System.out.println(KThread.currentThread().getName() + " spoke " + message);
        if(noListener) {
            //System.out.println(KThread.currentThread().getName() + " waiting for a listener");
            cond.sleep();
            noListener = true;
            //System.out.println(KThread.currentThread().getName() + " got a listener");
            pairDone.wake();
        }
        else {
            //System.out.println(KThread.currentThread().getName() + " Waking up a listener");
            cond.wake();
            pairDone.sleep();
            noListener = true;
        }
        //System.out.println(KThread.currentThread().getName() + " stuck here");
        multiSpeaker.wake();
        lock.release();
        /*if(speakerCount > listenerCount || (speakerCount == 0 && listenerCount == 0)) {
            System.out.println("Entered speaker if");
            upCounter.acquire();
            speakerCount++;
            upCounter.release();
            multiSpeaker.acquire();
            message = word;
            cond.sleep();
            multiSpeaker.release();
            lock.release();
            downCounter.acquire();
            speakerCount--;
            downCounter.release();
        }
        else {
            System.out.println("Entered speaker else");
            cond.wake();
            message = word;
            lock.release();
        }*/
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
        int toReturn = 0;
        lock.acquire();
        if (!noListener) {
            //System.out.println(KThread.currentThread().getName() + " waiting 1");
            multiListener.sleep();
            //System.out.println(KThread.currentThread().getName() + " done waiting 1");
        }
        noListener = false;
        if(noSpeaker){
            //System.out.println(KThread.currentThread().getName() + " waiting for a speaker");
            cond.sleep();
            //System.out.println(KThread.currentThread().getName() + " got a speaker");
            pairDone.wake();
            noSpeaker = true;
        }
        else {
            //System.out.println(KThread.currentThread().getName() + " Waking up a speaker");
            cond.wake();
            noSpeaker = true;
            pairDone.sleep();
            /*multiListener.wake();
            multiSpeaker.wake();*/
        }
        toReturn = message;
        System.out.println(KThread.currentThread().getName() + " listened " + toReturn);
        message = 0;
        //System.out.println(KThread.currentThread().getName() + " stuck here");
        multiListener.wake();
        lock.release();
        /*if(listenerCount > speakerCount || (speakerCount == 0 && listenerCount == 0)) {
            System.out.println("Entered listener if");
            upCounter.acquire();
            listenerCount++;
            upCounter.release();
            multiListener.acquire();
            cond.sleep();
            toReturn = message;
            multiListener.release();
            lock.release();
            downCounter.acquire();
            listenerCount--;
            downCounter.release();
        }
        else {
            System.out.println("Entered listener else");
            cond.wake();
            toReturn = message;
            lock.release();
        }*/
        return toReturn;
    }

    private final Lock lock = new Lock();
    private final Condition2 cond = new Condition2(lock);
    private final Condition2 multiSpeaker = new Condition2(lock);
    private final Condition2 multiListener = new Condition2(lock);
    private final Condition2 pairDone = new Condition2(lock);
    private int message;
    private boolean ongoing;
    private boolean noSpeaker = true;
    private boolean noListener = true;
}
