package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.*;
import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {
		int numPhysPages = Machine.processor().getNumPhysPages();
		pageTable = new TranslationEntry[numPhysPages];
		childMap = new HashMap<Integer, UserProcess>();
		childExitStatus = new HashMap<Integer, Integer>();
		for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);
		
		freeDescriptors = new ArrayList<Integer>();
		openFiles = new HashMap<Integer, OpenFile>();
		mutex.acquire();
		for(int i=2; i < 16; i++) {
			freeDescriptors.add(i);
		}
		openFiles.put(0, UserKernel.console.openForReading());
		openFiles.put(1, UserKernel.console.openForWriting());
		
		pid = nextPid;
		nextPid++;
		mutex.release();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
		return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

		new UThread(this).setName(name).fork();

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
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;
		
		if(vaddr > numPages * pageSize) 
			return 0;
		
		if((vaddr+length) > numPages * pageSize)
			return 0;
		
		int pageNum = vaddr / pageSize;
		
		if (!pageTable[pageNum].valid)
			return 0;
		
		int physicalPageNum = pageTable[pageNum].ppn;
		
		int pageOffset = vaddr % pageSize;
		
		int physicalAddress = physicalPageNum * pageSize + pageOffset;
		
		if (physicalAddress < 0 || physicalAddress >= memory.length)
			return 0;
		
		int amount = Math.min(length, memory.length - physicalAddress);
		System.arraycopy(memory, physicalAddress, data, offset, amount);

		return amount;
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		if(vaddr > numPages * pageSize) 
			return 0;
		
		if((vaddr+length) > numPages * pageSize)
			return 0;
		
		int pageNum = vaddr / pageSize;
		
		if (!pageTable[pageNum].valid)
			return 0;
		
		if (pageTable[pageNum].readOnly)
			return 0;
		
		int physicalPageNum = pageTable[pageNum].ppn;
		
		int pageOffset = vaddr % pageSize;
		
		int physicalAddress = physicalPageNum * pageSize + pageOffset;
		
		if (physicalAddress < 0 || physicalAddress >= memory.length)
			return 0;
		
		int amount = Math.min(length, memory.length - physicalAddress);
		System.arraycopy(data, offset, memory, physicalAddress, amount);

		return amount;
	}

	/**
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
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
		for (int s = 0; s < coff.getNumSections(); s++) {
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
		for (int i = 0; i < args.length; i++) {
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
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		
		//update page table
		mutex.acquire();
		if (UserKernel.freePhysPages.size() < numPages){
			mutex.release();
			return false;
		}
		int physPageNum = 0;

		for (int i = 0; i < numPages; i++){
			if(UserKernel.freePhysPages.isEmpty()) {
				mutex.release();
				return false;
			}	
			physPageNum = UserKernel.freePhysPages.removeFirst();
			pageTable[i].ppn = physPageNum;
			pageTable[i].vpn = i;
			pageTable[i].valid = true;
		}
		mutex.release();
		// load sections
		if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, pageTable[vpn].ppn);
				pageTable[vpn].readOnly = section.isReadOnly();
				pageTable[vpn].valid = true;
			}
		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		mutex.acquire();
		for(int i = 0; i < numPages; i++){
			if(pageTable[i].valid)
				UserKernel.freePhysPages.add(pageTable[i].ppn);
		}
		mutex.release();
		
		numPages = 0;
		pageTable = null;
		
		coff.close();
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
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

		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}
	
	/**
	 * Handle the create system call.
	 */
	private int handleCreate(int a0) {
		Lib.debug(dbgProcess, "start handleCreate()");
		
		String filename = this.readVirtualMemoryString(a0, MAXLEN);
		Lib.debug(dbgProcess, "file:" + filename);
		
		if(filename == null)
			return -1;
		else {
			OpenFile openFile = ThreadedKernel.fileSystem.open(filename, true);
			if((openFile == null) || (freeDescriptors.size() < 1))
				return -1;
			else {
				Integer descriptorId = freeDescriptors.remove(0);
				openFiles.put(descriptorId, openFile);
				Lib.debug(dbgProcess, "create in " + descriptorId);
				return descriptorId;
			}		
		}
	}
	
	/**
	 * Handle the open system call.
	 */
	private int handleOpen(int a0) {
		Lib.debug(dbgProcess, "start handleOpen()");
		
		String filename = this.readVirtualMemoryString(a0, MAXLEN);
		Lib.debug(dbgProcess, "file:" + filename);
		
		if(filename == null)
			return -1;
		else {
			OpenFile openFile = ThreadedKernel.fileSystem.open(filename, false);
			if((openFile == null) || (freeDescriptors.size() < 1))
				return -1;
			else {
				Integer descriptorId = freeDescriptors.remove(0);
				openFiles.put(descriptorId, openFile);
				Lib.debug(dbgProcess, "create in " + descriptorId);
				return descriptorId;
			}		
		}
	}
	
	/**
	 * Handle the read system call.
	 */
	private int handleRead(int a0, int a1, int a2) {
		Lib.debug(dbgProcess, "start handleRead()");
		
		OpenFile openFile = openFiles.get(a0);
		
		if(openFile == null)
			return -1;
		else {
			byte[] readBuffer = new byte[Processor.pageSize];
			boolean ifFinish = false;
			int transferCount = 0;
			int count = a2;
			while (!ifFinish && count > 0) {
				int readLength = Math.min(Processor.pageSize, count);
				
				int actualReadLength = openFile.read(readBuffer, 0, readLength);
				if (actualReadLength == -1)
					return -1;
				if (actualReadLength < readLength)
					ifFinish = true;
				
				int transferredBytes = writeVirtualMemory(a1, readBuffer, 0, actualReadLength);
				if (transferredBytes != actualReadLength)
					return -1;
				
				count -= actualReadLength;
				a1 += actualReadLength;
				transferCount += actualReadLength;
			}
			Lib.debug(dbgProcess, "Transferred bytes: " + transferCount);
			return transferCount;
		}
	}
	
	/**
	 * Handle the write system call.
	 */
	private int handleWrite(int a0, int a1, int a2) {
		Lib.debug(dbgProcess, "start handleWrite()");
		
		OpenFile openFile = openFiles.get(a0);
		
		if(openFile == null)
			return -1;
		else {
			byte[] writeBuffer = new byte[Processor.pageSize];
			int transferCount = 0;
			int count = a2;
			while (count > 0) {
				int readLength = Math.min(Processor.pageSize, count);
				
				int actualReadLength = readVirtualMemory(a1, writeBuffer, 0, readLength);
				if (actualReadLength == -1)
					return -1;
				if (actualReadLength < readLength)
					return -1;
				
				int transferredBytes = openFile.write(writeBuffer, 0, actualReadLength);
				if (transferredBytes != actualReadLength)
					return -1;
				
				count -= actualReadLength;
				a1 += actualReadLength;
				transferCount += actualReadLength;
			}
			Lib.debug(dbgProcess, "Transferred bytes: " + transferCount);
			return transferCount;
		}
	}
	
	/**
	 * Handle the close system call.
	 */
	private int handleClose(int a0) {
		Lib.debug(dbgProcess, "start handleClose()");
		
		OpenFile openFile = openFiles.get(a0);
		
		if(openFile == null)
			return -1;
		else {
			openFile.close();
			openFiles.remove(a0);
			freeDescriptors.add(a0);
			Lib.debug(dbgProcess, "Close file successfully");
			return 0;
		}
	}
	
	/**
	 * Handle the unlink system call.
	 */
	private int handleUnlink(int a0) {
		Lib.debug(dbgProcess, "start handleUnlink()");
		
		String filename = this.readVirtualMemoryString(a0, MAXLEN);
		Lib.debug(dbgProcess, "file:" + filename);
		
		if(filename == null)
			return -1;
		else if(ThreadedKernel.fileSystem.remove(filename))
			return 0;
		else
			return -1;
	}
	
	/**
	 * Handle the exit system call.
	 */
	private void handleExit(int a0) {
		Lib.debug(dbgProcess, "start handleExit()");
		
		for(int descriptorId: openFiles.keySet()) {
			OpenFile openFile = openFiles.get(descriptorId);
			openFile.close();
			freeDescriptors.remove(descriptorId);
		}
		openFiles.clear();
		unloadSections();
		if(toJoinThread != null) {
			boolean intStatus = Machine.interrupt().disable();
			toJoinThread.ready();
			Machine.interrupt().restore(intStatus);
			toJoinThread = null;
			if(parent != null) {
				parent.childExitStatus.put(this.pid, a0);				
			}
			
		}
		
		if(parent != null) {
			parent.childMap.remove(pid);
			parent = null;
		}
		
		for (Map.Entry<Integer, UserProcess> entry : childMap.entrySet()) {
			entry.getValue().parent = null;
			entry.getValue().toJoinThread = null;
		}
		
		childMap.clear();
		
		if (this.pid == 1)
			Kernel.kernel.terminate();
		else {
			KThread.currentThread().finish();			
		}			
	}
	
	
	/**
	 * Handle the exec system call.
	 */
	private int handleExec(int a0, int a1, int a2) {
		
		Lib.debug(dbgProcess, "start handleExec()");
		if(a0 <= 0 || a1 < 0 || a2 < 0)
			return -1;
		
		String fileName = readVirtualMemoryString(a0, MAXLEN);

		if(fileName == null)
			return -1;
		String arguments[] = new String[] {};
		if(a1 > 0) {
			arguments = new String[a1];
			byte data[]=new byte[4];
			for(int i = 0; i < a1; i++) {
				readVirtualMemory(a2 + i * 4, data);
				int argumentAddr = Lib.bytesToInt(data, 0);
				arguments[i] = readVirtualMemoryString(argumentAddr, MAXLEN);
			}
		}

		UserProcess child = new UserProcess();
		
		if(child.execute(fileName, arguments) == false)
			return -1;
		
		child.parent = this;
		childMap.put(child.pid, child);
		childExitStatus.put(child.pid, Integer.MIN_VALUE);
		return child.pid;
	}
	
	/**
	 * Handle the join system call.
	 */
	private int handleJoin(int a0, int a1) {
		Lib.debug(dbgProcess, "start handleJoin()");
		
		if(!childMap.containsKey(a0))
			return -1;
		if(!childExitStatus.containsKey(a0))
			return -1;
		
		childMap.get(a0).toJoinThread =  (UThread) KThread.currentThread();
		
		boolean intStatus = Machine.interrupt().disable();
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
			int exitStatus = childExitStatus.get(a0);
			byte[] exitStatusBytes = new byte[4];
			exitStatusBytes = Lib.bytesFromInt(exitStatus);
			writeVirtualMemory( a1, exitStatusBytes);	
			if(exitStatus != Integer.MIN_VALUE)
				return 1;
		return 0;
	}
	

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
			
		case syscallExit:
			handleExit(a0);
			
		case syscallExec:
			return handleExec(a0, a1, a2);
			
		case syscallJoin:
			return handleJoin(a0, a1);
			
		case syscallCreate:
			return handleCreate(a0);

		case syscallOpen:
			return handleOpen(a0);
			
		case syscallRead:
			return handleRead(a0, a1, a2);
			
		case syscallWrite:
			return handleWrite(a0, a1, a2);
			
		case syscallClose:
			return handleClose(a0);
			
		case syscallUnlink:
			return handleUnlink(a0);
			
		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
	}

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);
			processor.advancePC();
			break;

		default:
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
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

	private static final int MAXLEN = 256;

	private List<Integer> freeDescriptors;
	
	private HashMap<Integer, OpenFile> openFiles;
	
	private Lock mutex = new Lock();
	
	private UserProcess parent;
	
	private HashMap<Integer,UserProcess> childMap;
	
	private int pid;
	
	private static int nextPid = 1;
	
	private HashMap<Integer,Integer> childExitStatus;
	
	private UThread toJoinThread;
	
}
