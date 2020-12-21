package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import javax.crypto.Mac;
import java.io.EOFException;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();
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
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	super.unloadSections();
    }

    @Override
    public boolean execute(String name, String[] args) {
        return super.execute(name, args);
    }

    @Override
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

        if (numPages == 0 || VMKernel.ipt == null) {
            return -1;
        }

        byte[] memory = Machine.processor().getMemory();

        int amount = 0;
        int readAmount = 0;
        int startPage = Processor.pageFromAddress(vaddr);
        int endPage = Processor.pageFromAddress(vaddr + length - 1);
        int endVaddr = vaddr + length - 1;


        if (vaddr < 0 || endVaddr > Processor.makeAddress(numPages - 1, pageSize - 1)) {
            return -1;
        }


        for (int i = startPage; i <= endPage; i++) {
            iptKey key = new iptKey(i, this.processID);
            TranslationEntry entry = VMKernel.ipt.get(key);
            if (i > pageTable.length || !entry.valid)
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
            int physicalAddress = Processor.makeAddress(ppn, addressOffset);
            System.arraycopy(memory, physicalAddress, data, amount + offset, readAmount);
            amount += readAmount;
        }

        return amount;
    }

    @Override
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

        byte[] memory = Machine.processor().getMemory();

        int amount = 0;
        int writtenAmount = 0;
        int startPage = Processor.pageFromAddress(vaddr);
        int endPage = Processor.pageFromAddress(vaddr + length - 1);
        int endVaddr = vaddr + length - 1;


        // for now, just assume that virtual addresses equal physical addresses
        /*if(vaddr < 0 || endVaddr > Processor.makeAddress(numPages-1, pageSize-1)) {
            return -1;
        }*/

        for (int i= startPage; i<= endPage; i++){
            iptKey key = new iptKey(i, this.processID);
            TranslationEntry entry = VMKernel.ipt.get(key);
            if(i>pageTable.length || entry.readOnly || !entry.valid)
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
        return amount;
    }

    @Override
    public boolean load(String name, String[] args) {
        Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

        OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
        if (executable == null) {
            Lib.debug(dbgProcess, "\topen failed");
            return false;
        }

        try {
            coff = new Coff(executable);
        }
        catch (EOFException e) {
            executable.close();
            Lib.debug(dbgProcess, "\tcoff load failed");
            return false;
        }

        // make sure the sections are contiguous and start at page 0
        numPages = 0;
        for (int s=0; s<coff.getNumSections(); s++) {
            CoffSection section = coff.getSection(s);
            if (section.getFirstVPN() != numPages) {
                coff.close();
                Lib.debug(dbgProcess, "\tfragmented executable");
                return false;
            }
            numPages += section.getLength();
        }
        codeSectionSize = numPages;
        // make sure the argv array will fit in one page
        byte[][] argv = new byte[args.length][];
        int argsSize = 0;
        for (int i=0; i<args.length; i++) {
            argv[i] = args[i].getBytes();
            // 4 bytes for argv[] pointer; then string plus one for null byte
            argsSize += 4 + argv[i].length + 1;
        }
        if (argsSize > pageSize) {
            coff.close();
            Lib.debug(dbgProcess, "\targuments too long");
            return false;
        }

        // program counter initially points at the program entry point
        initialPC = coff.getEntryPoint();

        // next comes the stack; stack pointer initially points to top of it
//        for (int i = 0; i < stackPages; ++i) {
//            int vpn = numPages + i;
//            int ppn = UserKernel.assign();
//            if(ppn == -1)
//            {
//                for(int pt=0;pt<pageTable.length;pt++)
//                {
//                    UserKernel.freePage(pageTable[pt].ppn);
//                    pageTable[pt] = new TranslationEntry(pageTable[pt].vpn,0,false,false,false,false);
//                }
//                numPages =0;
//                return false;
//            }
//            pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
//        }


        numPages += stackPages;
        initialSP = numPages*pageSize;

        // and finally reserve 1 page for arguments
//        int argPPN = UserKernel.assign();
//        if(argPPN == -1)
//        {
//            for(int pt=0;pt<pageTable.length;pt++)
//            {
//                UserKernel.freePage(pageTable[pt].ppn);
//                pageTable[pt] = new TranslationEntry(pageTable[pt].vpn,0,false,false,false,false);
//            }
//            numPages = 0;
//            return false;
//        }
//        pageTable[numPages] = new TranslationEntry(numPages, argPPN, true, false, false, false);
//        numPages++;

//        if (!loadSections())
//            return false;

        // store arguments in last page
        /*int entryOffset = (numPages-1)*pageSize;
        int stringOffset = entryOffset + args.length*4;

        this.argc = args.length;
        this.argv = entryOffset;

        for (int i=0; i<argv.length; i++) {
            byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
            Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
            entryOffset += 4;
            Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
                    argv[i].length);
            stringOffset += argv[i].length;
            Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
            stringOffset += 1;
        }*/


        return true;
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
            int vaddr = Machine.processor().readRegister(Processor.regBadVAddr);
            int vpn = Processor.pageFromAddress(vaddr);
            handleTLBmiss(vpn);
            break;
	default:
	    super.handleException(cause);
	    break;
	}
    }

    private void handleTLBmiss(int vpn) {
        int TLBsize = Machine.processor().getTLBSize();
        int TLBindex = -1;
        TranslationEntry toSwap = null, toAdd = null;
        for (int i=0; i<TLBsize; i++){
            TranslationEntry entry = Machine.processor().readTLBEntry(i);
            if (!entry.valid){
                TLBindex = i;
                break;
            }
        }

        if (TLBindex == -1){
            TLBindex = Lib.random(TLBsize);
            toSwap = Machine.processor().readTLBEntry(TLBindex);
            if (toSwap.dirty){
                VMKernel.ipt.replace(new iptKey(toSwap.vpn, this.processID),toSwap);
            }
        }

        while (true){
            toAdd = VMKernel.ipt.get(new iptKey(vpn, this.processID));
            if (toAdd == null || !toAdd.valid) {
                handlePageFault(vpn);
            }
            else {
                break;
            }
        }
        Machine.processor().writeTLBEntry(TLBindex, toAdd);
    }

    private void handlePageFault(int vpn) {
        boolean loopBreaker = false;
        iptKey key = new iptKey(vpn, this.processID);
        TranslationEntry entry = VMKernel.ipt.get(key);
        int ppn = (entry == null) ? VMKernel.assign() : entry.ppn;
        boolean used = entry != null && entry.used;
        boolean dirty = entry != null && entry.dirty;
        boolean readOnly = false;
        if (entry != null && entry.dirty){
            System.out.println("Ignoring entry.dirty");
        }
        else {
            if (vpn <= codeSectionSize){
//                System.out.println("In code section");
                for (int s=0; s<coff.getNumSections(); s++) {
                    CoffSection section = coff.getSection(s);

                    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
                            + " section (" + section.getLength() + " pages)");

                    for (int i=0; i<section.getLength(); i++) {
                        int vpn_here = section.getFirstVPN()+i;
                        if (vpn_here == vpn) {
                            if (section.isReadOnly())
                                readOnly = true;
                            section.loadPage(i, ppn);
                            loopBreaker = true;
                            break;
                        }
                    }
                    if (loopBreaker)
                        break;
                }
            }
            else {
//                System.out.println("in data section");
                byte[] memory = Machine.processor().getMemory();
                byte[] buffer = new byte[pageSize];
//                for (int i=0; i<pageSize; i++){
//                    System.out.println(buffer[0]);
//                }
                System.arraycopy(buffer, 0, memory, ppn*pageSize, pageSize);
            }
            TranslationEntry newEntry = new TranslationEntry(vpn, ppn,true, readOnly, used, dirty);
            if(entry == null)
                VMKernel.ipt.put(key, newEntry);
            else
                VMKernel.ipt.replace(key,newEntry);

        }
    }

    private int initialPC, initialSP;
    private int argc, argv;
    private int codeSectionSize;
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';
    private static int debugCounter = 0;
}
