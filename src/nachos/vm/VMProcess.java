package nachos.vm;

import nachos.machine.*;
import nachos.userprog.*;

import java.util.Hashtable;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
        super();
        vpnToCoffMap = new Hashtable<>();
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
	super.saveState();
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	//super.restoreState();
    }

    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        //return super.loadSections();
        //System.out.println("Entering loadSectionVM");
        if (numPages > Machine.processor().getNumPhysPages()){
            coff.close();
            return false;
        }
        for (int i = 0; i<coff.getNumSections(); i++){
            CoffSection section = coff.getSection(i);
            for (int j = 0; j<section.getLength(); j++){
                int vpn = section.getFirstVPN() + j;
                pair<Integer, Integer> secOffPair = new pair<>(i, j);
                vpnToCoffMap.put(vpn, secOffPair);
            }
        }
        //System.out.println("Exiting loadSectionVM");
        return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	    vpnToCoffMap.clear();
        //super.unloadSections();
    }    

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
        case Processor.exceptionTLBMiss:
            int tlbMissAddress = processor.readRegister(Processor.regBadVAddr);
            int vpn = Processor.pageFromAddress(tlbMissAddress);
            //VMKernel.lock.acquire();
            boolean success = handleTLBMiss(vpn);
            if(!success){
                UThread.finish();
            }
            //VMKernel.lock.release();
            break;
	default:
	    super.handleException(cause);
	    break;
	}
    }

    @Override
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        //System.out.println("Entering readVirtualMemory");
        //System.out.println(vaddr);
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        int amount = 0;
        int readAmount = 0;
        int startPage = Processor.pageFromAddress(vaddr);
        int endPage = Processor.pageFromAddress(vaddr + length - 1);
        int endVaddr = vaddr + length - 1;


        for (int i = startPage; i <= endPage; i++) {
            iptKey key = new iptKey(i,this.processID);
            TranslationEntry entry = PageTable.getInstance().getEntry(key);
            if (i > PageTable.getInstance().getLength() || entry == null || !entry.valid)
                break;

            int startAddress = Processor.makeAddress(i, 0);
            int endAddress = Processor.makeAddress(i, pageSize - 1);
            readAmount = 0;
            int addressOffset = 0;
            if (vaddr >= startAddress && endVaddr <= endAddress) {
                addressOffset = vaddr - startAddress;
                readAmount = endVaddr - vaddr + 1;
            } else if (vaddr < startAddress && endVaddr <= endAddress) {
                addressOffset = 0;
                readAmount = endVaddr - startAddress + 1;
            } else if (vaddr >= startAddress && endVaddr > endAddress) {
                addressOffset = vaddr - startAddress;
                readAmount = endAddress - vaddr + 1;
            } else if (vaddr < startAddress && endVaddr > endAddress) {
                addressOffset = 0;
                readAmount = endAddress - startAddress + 1;
            }
            int ppn = entry.ppn;
            //System.out.println(i  + " " + ppn);
            int physicalAddress = Processor.makeAddress(ppn, addressOffset);
            System.arraycopy(memory, physicalAddress, data, amount + offset, readAmount);
            amount += readAmount;
        }
        //System.out.println("Exiting readVM");
        return amount;
    }

    @Override
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        //System.out.println("Entering writeVirtualMemory");
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        int amount = 0;
        int writtenAmount = 0;
        int startPage = Processor.pageFromAddress(vaddr);
        int endPage = Processor.pageFromAddress(vaddr + length - 1);
        int endVaddr = vaddr + length - 1;


        for (int i= startPage; i<= endPage; i++){
            iptKey key = new iptKey(i,this.processID);
            TranslationEntry entry = PageTable.getInstance().getEntry(key);
            if(i>PageTable.getInstance().getLength() || entry == null || entry.readOnly || !entry.valid)
                break;

            int startAddress = Processor.makeAddress(i,0);
            int endAddress = Processor.makeAddress(i,pageSize-1);
            writtenAmount = 0;
            int addressOffset = 0;
            if (vaddr >= startAddress && endVaddr <= endAddress){
                addressOffset = vaddr - startAddress;
                writtenAmount = endVaddr - vaddr + 1;
            }
            else if (vaddr < startAddress && endVaddr <= endAddress){
                addressOffset = 0;
                writtenAmount = endVaddr - startAddress + 1;
            }
            else if(vaddr >= startAddress && endVaddr > endAddress){
                addressOffset = vaddr - startAddress;
                writtenAmount = endAddress - vaddr + 1;
            }
            else if(vaddr < startAddress && endVaddr > endAddress){
                addressOffset = 0;
                writtenAmount = endAddress - startAddress + 1;
            }
            int ppn = entry.ppn;
            int physicalAddress = Processor.makeAddress(ppn, addressOffset);
            System.arraycopy(data, amount + offset, memory, physicalAddress, writtenAmount);
            amount += writtenAmount;
        }
        //System.out.println("Exiting writeVirtualMemory");
        return amount;
    }

    public boolean handleTLBMiss(int vpn){
//        System.out.println("Vpn in handleTLBmiss  = " + vpn);
//        System.out.println("Entering handleTLBmiss");
        int TLBsize = Machine.processor().getTLBSize();
        int TLBindex = -1;
        TranslationEntry toSwap = null, toAdd = null;
//        System.out.println("Entering loop for invalid finding");
        for (int i=0; i<TLBsize; i++){
            TranslationEntry entry = Machine.processor().readTLBEntry(i);
            if (!entry.valid){
                TLBindex = i;
                toSwap = entry;
                break;
            }
        }
        // all TLB entries are valid
        if (TLBindex == -1){
//            System.out.println("All entries are valid");
            // random evict
            TLBindex = Lib.random(TLBsize);
            toSwap = Machine.processor().readTLBEntry(TLBindex);
            if (toSwap.dirty){
                /*TranslationEntry iptEntry = PageTable.getInstance().getEntry(new iptKey(toSwap.vpn, this.processID));
                iptEntry = toSwap;*/
//                System.out.println("toSwap.dirty is on");
                PageTable.getInstance().updateEntry(new iptKey(toSwap.vpn, this.processID), toSwap);
            }
        }
        while (true) {
            toAdd = PageTable.getInstance().getEntry(new iptKey(vpn, this.processID));
            if (toAdd == null || !toAdd.valid) {
//                System.out.println("Before entering pageFault");
                handlePageFault(vpn);
            }
            else
                break;
        }
        Machine.processor().writeTLBEntry(TLBindex, toAdd);
//        System.out.println("Exiting handleTLBMiss");
        return true;
    }

    private void handlePageFault(int vpn) {
//        System.out.println("Entering handlePageFault");
        iptKey key = new iptKey(vpn, this.processID);
        TranslationEntry entry = PageTable.getInstance().getEntry(key);
        int ppn = (entry == null) ? VMKernel.assign() : entry.ppn;
        boolean used = entry != null && entry.used;
        boolean dirty = entry != null && entry.dirty;
//        System.out.println("ppn = " + ppn);
//        System.out.println("vpn = " + vpn);
        if (entry != null && entry.dirty){
//            System.out.println("Ignoring entry.dirty");
        }
        else {
            if (vpn <= codeSectionSize){
                pair<Integer, Integer> p = vpnToCoffMap.get(vpn);
                CoffSection section = coff.getSection(p.getElement1());
                section.loadPage(p.getElement2(), ppn);
            }
            else {
                /*byte[] memory = Machine.processor().getMemory();
                byte[] buffer = new byte[pageSize];
                System.arraycopy(buffer, 0, memory, ppn*pageSize, pageSize);*/
            }
            TranslationEntry newEntry = new TranslationEntry(vpn, ppn,true, false, used, dirty);
            if(entry == null)
                PageTable.getInstance().addEntry(key, newEntry);
            else
                PageTable.getInstance().updateEntry(key, newEntry);

        }

//        System.out.println("Exiting handlePageFault");
    }



    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    protected Hashtable<Integer, pair<Integer, Integer>> vpnToCoffMap;

}
