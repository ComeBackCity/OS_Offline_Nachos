package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
        super();
        //swapfile = ThreadedKernel.fileSystem.open(swapFileName,true);
        pageTable = new Hashtable<iptKey, TranslationEntry>();
        TLB = new TranslationEntry[Machine.processor().getTLBSize()];
        for (int i=0; i<TLB.length; i++){
            TLB[i] = new TranslationEntry(0,0,false,false,false,false);
        }
        swapSpace = new Hashtable<>();
        processHashMap = new HashMap<>();
        //lock = new Lock();
    }

    public static void addToSwapSpace(TranslationEntry toSwap) {
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
	super.selfTest();
    }

    /**
     * Start running user programs.
     */
    public void run() {
	super.run();
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /*public static int newPage() {
        int pageNo = assign();

        if (pageNo == -1){

        }
        return pageNo;
    }*/

    /*public pair<TranslationEntry, Integer> randomSelect(){
        iptKey[] keys = pageTable.keySet().toArray(new iptKey[0]);
        TranslationEntry te = null;
        iptKey key = null;
        do{
            int index = Lib.random(keys.length);
            key = keys[index];
            te = pageTable.get(key);
        }
        while (te == null || !te.valid);
        return new pair<TranslationEntry, Integer>(te, key.getProcessID());
    }*/


    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';

    public static Hashtable<iptKey, TranslationEntry> pageTable;
    TranslationEntry[] TLB;
    static Lock lock;

    String swapFileName = "swpfile";
    OpenFile swapfile;
    static Hashtable<Integer, pair<Integer, Integer>> swapSpace;
    static HashMap<Integer, VMProcess> processHashMap;
}
