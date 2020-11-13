package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
	pageTable = new TranslationEntry[numPhysPages];
	/*for (int i=0; i<numPhysPages; i++)
	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);*/

		//fileDescriptors = new OpenFile[16];
		lock = new Lock();
		boolean initStatus = Machine.interrupt().disable();
		lock.acquire();
		counter++;
		processID = counter;
		running++;
		lock.release();
		/*fileDescriptors[0] = UserKernel.console.openForReading();
		fileDescriptors[1] = UserKernel.console.openForWriting();*/
		stdin = UserKernel.console.openForReading();
		stdout = UserKernel.console.openForWriting();
		Machine.interrupt().restore(initStatus);
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	myThread = (UThread) new UThread(this).setName(name);
	myThread.fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++)
	{
	    if (bytes[length] == 0) {
//	    	String s = new String(bytes,0,length);
//			System.out.println("s: "+s + " s end");
	    	return new String(bytes, 0, length);
		}
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0 && offset + length <= data.length);

		//System.out.println("after vaddr : " + vaddr);

		if (numPages == 0 || pageTable == null) {
//			System.out.println("returning 1");
			return -1;
		}

		byte[] memory = Machine.processor().getMemory();

		int amount = 0;
		int readAmount = 0;
		int startPage = Processor.pageFromAddress(vaddr);
		int endPage = Processor.pageFromAddress(vaddr + length - 1);
		int endVaddr = vaddr + length - 1;

		// for now, just assume that virtual addresses equal physical addresses
	/*if (vaddr < 0 || vaddr >= memory.length)
	    return 0;*/
		if (vaddr < 0 || endVaddr > Processor.makeAddress(numPages - 1, pageSize - 1)) {
//			System.out.println("returning 2");
			return -1;
		}

	/*int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(memory, vaddr, data, offset, amount);*/

		for (int i = startPage; i <= endPage; i++) {
			if (i > pageTable.length || !pageTable[i].valid)
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
			int ppn = pageTable[i].ppn;
			int physicalAddress = Processor.makeAddress(ppn, addressOffset);
			System.arraycopy(memory, physicalAddress, data, amount + offset, readAmount);
			amount += readAmount;
		}

		return amount;
	}

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {

	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

		//System.out.println("vaddr in wvm " + vaddr);
		//Lib.debug(dbgProcess, "\n vaddr " + vaddr);
	if(numPages > pageTable.length){
		return 0;
	}

	byte[] memory = Machine.processor().getMemory();

		int amount = 0;
		int writtenAmount = 0;
		int startPage = Processor.pageFromAddress(vaddr);
		int endPage = Processor.pageFromAddress(vaddr + length - 1);
		int endVaddr = vaddr + length - 1;


	// for now, just assume that virtual addresses equal physical addresses
		if(vaddr < 0 || endVaddr > Processor.makeAddress(numPages-1, pageSize-1)) {
			return -1;
		}

	/*int amount = Math.min(length, memory.length-vaddr);
	System.arraycopy(data, offset, memory, vaddr, amount);*/

		for (int i= startPage; i<= endPage; i++){
			if(i>pageTable.length || pageTable[i].readOnly || !pageTable[i].valid)
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
			int ppn = pageTable[i].ppn;
			int physicalAddress = Processor.makeAddress(ppn, addressOffset);
			System.arraycopy(data, amount + offset, memory, physicalAddress, writtenAmount);
			amount += writtenAmount;
		}
	return amount;
    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
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
		for (int i = 0; i < stackPages; ++i) {
			int vpn = numPages + i;
			int ppn = UserKernel.assign();
			if(ppn == -1)
			{
				for(int pt=0;pt<pageTable.length;pt++)
				{
					UserKernel.freePage(pageTable[pt].ppn);
					pageTable[pt] = new TranslationEntry(pageTable[pt].vpn,0,false,false,false,false);
				}
				numPages =0;
				return false;
			}
			pageTable[vpn] = new TranslationEntry(vpn, ppn, true, false, false, false);
		}


	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
		int argPPN = UserKernel.assign();
		if(argPPN == -1)
		{
			for(int pt=0;pt<pageTable.length;pt++)
			{
				UserKernel.freePage(pageTable[pt].ppn);
				pageTable[pt] = new TranslationEntry(pageTable[pt].vpn,0,false,false,false,false);
			}
			numPages = 0;
			return false;
		}
		pageTable[numPages] = new TranslationEntry(numPages, argPPN, true, false, false, false);
	numPages++;

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
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
	}


	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;
		int ppn = UserKernel.assign();
		if(ppn == -1)
		{
			for(int pt=0;pt<pageTable.length;pt++)
			{
				UserKernel.freePage(pageTable[pt].ppn);
				pageTable[pt] = new TranslationEntry(pageTable[pt].vpn,0,false,false,false,false);
			}
			numPages =0;
			return false;
		}
		pageTable[vpn] = new TranslationEntry(vpn, ppn, true, section.isReadOnly(), false, false);

		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, ppn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
		//System.out.println("Unloading happned");
    	deallocatePageTable();
		numPages = 0;
		stdin.close();
		stdout.close();
		stdin = null;
		stdout = null;
		coff.close();
    }    

    public void deallocatePageTable(){
		//System.out.println("printing pt length before deallocation" + pageTable.length);
		for (int i=0; i<numPages; i++){
			int ppn = pageTable[i].ppn;
			UserKernel.freePage(ppn);
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		}
	}
    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

    	if(processID != 1) {
			return -1;
		}

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

    private int handleRead(int fileDescriptor, int bufferAddress, int size){
		//System.out.println(fileDescriptor);
		//System.out.println("badd" + bufferAddress);
		//Lib.debug(dbgProcess,"\nbadd" + bufferAddress);
    	OpenFile file;


//
		if(fileDescriptor != 0 || size < 0 || stdin == null|| bufferAddress < 0 ){
			//Lib.debug(dbgProcess,"\nin if1 retunring");
			return -1;
		}
		file = stdin;

		int length, count;
		byte[] buffer = new byte[size];
		length = file.read(buffer, 0, size);

		if(length == -1){
			//Lib.debug(dbgProcess,"\nin if2 read len " + length);
			return -1;
		}
		count = writeVirtualMemory(bufferAddress, buffer, 0, size);
    	return count;
	}

	private int handleWrite(int fileDescriptor, int bufferAddress, int size){
		//System.out.println(fileDescriptor);
		OpenFile file;


		if(fileDescriptor != 1 || size < 0 || stdout == null){
			return -1;
		}

		file = stdout;

		int length, count;
		byte[] buffer = new byte[size];
		length = readVirtualMemory(bufferAddress, buffer, 0, size);
		//Lib.debug(dbgProcess," write len " + length);
		if(length == -1){
			return -1;
		}

		count = file.write(buffer, 0, size);
		return count;
	}

	private int handleExec(int fileNameAddress, int argc, int argvAddress){

    	if(fileNameAddress < 0 || argc < 0 || argvAddress < 0)
    		return -1;

    	String fileName = readVirtualMemoryString(fileNameAddress,256);

    	if(fileName == null || !fileName.endsWith(".coff"))
    		return -1;

		String[] argv = new String[argc];
		for (int i=0; i<argc; i++){
			byte[] argBuffer = new byte[4];
			//System.out.println("into loop i: " + i);
			if(readVirtualMemory(argvAddress + i * 4,argBuffer) != 4)
				return -1;
			int argAddress = Lib.bytesToInt(argBuffer,0);
			//System.out.println("arg Address: " + argAddress);

			String arg = readVirtualMemoryString(argAddress,256);

			//System.out.println("loop i: "+i+" arg: "+arg);
			if(arg == null) {
				//.out.println("returning form here");
				return -1;
			}
			argv[i] = arg;
		}


		UserProcess child = new UserProcess();

		if(!child.execute(fileName, argv)){
			return -1;
		}

		child.parent = this;
		children.add(child);
    	return child.processID;
	}

	private int handleJoin(int processID, int exitStatusVirtualAddress){
    	if(processID < 0 || exitStatusVirtualAddress < 0){
    		return -1;
		}
		UserProcess child = null;
    	for (int i=0; i<children.size(); i++){
    		if(children.get(i).processID == processID){
    			child = children.get(i);
    			break;
			}
		}

    	if(child == null)
    		return -1;

		child.myThread.join();

		statudLock.acquire();
		Integer status = childrenStatusMap.get(child.processID);
		statudLock.release();

		if(status == null){
			child.deallocatePageTable();
			return -1;
		}
		else {
			byte[] buffer = new byte[4];
			buffer = Lib.bytesFromInt(status);
			if (writeVirtualMemory(exitStatusVirtualAddress,buffer) == 4){
				return 1;
			}
			else {
				child.deallocatePageTable();
				return 0;
			}
		}

	}

	private void handleExit(int status){
		//System.out.println("Normal exit");
		if(parent != null){
			parent.statudLock.acquire();
			parent.childrenStatusMap.put(this.processID, status);
			parent.statudLock.release();
		}

		unloadSections();

		for (UserProcess child: children) {
			child.parent = null;
		}

		children.clear();
		UserProcess.running--;
		if (processID == 1 || running == 0){
			Kernel.kernel.terminate();
		}
		else {
			UThread.finish();
		}

	}


    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();

		case syscallRead:
			return handleRead(a0,a1,a2);

		case syscallWrite:
			return handleWrite(a0, a1, a2);

		case syscallExec:
			return handleExec(a0, a1, a2);

		case syscallJoin:
			return handleJoin(a0, a1);

		case syscallExit:
			handleExit(a0);
			return 0;

	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
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
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;				       
				       
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    private int initialPC, initialSP;
    private int argc, argv;
	
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';

    //private OpenFile[] fileDescriptors ;
	private OpenFile stdin;
	private OpenFile stdout;
    private Lock lock;
    private static int counter = 0;
    private static int running = 0;
    private int processID = 0;
    private UThread myThread;

    private UserProcess parent = null;
    private ArrayList<UserProcess> children = new ArrayList<>();

	private Lock statudLock = new Lock();
	private HashMap<Integer, Integer> childrenStatusMap = new HashMap<>();
    private final int stackPageSize = 8;
    private boolean alreadyUnloaded = false;
}
