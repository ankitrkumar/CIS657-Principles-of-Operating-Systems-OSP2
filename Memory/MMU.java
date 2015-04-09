package osp.Memory;

/** Authors:
	Sayali Khodke
	Yogesh Chaudhari
	Ankit Kumar
	Group 4
	CIS 657
**/
import java.util.*;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.Hardware.*;
import osp.Interrupts.*;

/**
    The MMU class contains the student code that performs the work of
    handling a memory reference.  It is responsible for calling the
    interrupt handler if a page fault is required.

    @OSPProject Memory
*/
public class MMU extends IflMMU
{
    /** 
        This method is called once before the simulation starts. 
	Can be used to initialize the frame table and other static variables.

        @OSPProject Memory
    */
    public static void init()
    {
		//initialize all the entries in the frame table
	   for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
        	setFrame(i, new FrameTableEntry(i));
        }

    }

    /**
       This method handlies memory references. The method must 
       calculate, which memory page contains the memoryAddress,
       determine, whether the page is valid, start page fault 
       by making an interrupt if the page is invalid, finally, 
       if the page is still valid, i.e., not swapped out by another 
       thread while this thread was suspended, set its frame
       as referenced and then set it as dirty if necessary.
       (After pagefault, the thread will be placed on the ready queue, 
       and it is possible that some other thread will take away the frame.)
       
       @param memoryAddress A virtual memory address
       @param referenceType The type of memory reference to perform 
       @param thread that does the memory access
       (e.g., MemoryRead or MemoryWrite).
       @return The referenced page.

       @OSPProject Memory
    */
    static public PageTableEntry do_refer(int memoryAddress,
					  int referenceType, ThreadCB thread)
    {
        // find the page to which the reference was made
		//pageNo = address/2^offset
		int pageNumber = memoryAddress / (int)Math.pow(2.0, getVirtualAddressBits() - getPageAddressBits());
		//get pageTableEntry
		PageTableEntry tempPageTableEntry = getPTBR().pages[pageNumber];
		
		if(tempPageTableEntry.isValid())
		{
			tempPageTableEntry.getFrame().setReferenced(true);
			if(referenceType == GlobalVariables.MemoryWrite)
			{
				tempPageTableEntry.getFrame().setDirty(true); // set the dirt bit for the entry if the entry found is invalid
			}
			return tempPageTableEntry;
		}
		else
		{
			if(tempPageTableEntry.getValidatingThread() == null)
			{
				//if the pageFault occurs due to the original thread, set pagefault as true and handle the pagefault
				tempPageTableEntry.pageFaulted = true;
				InterruptVector.setInterruptType(referenceType);
				InterruptVector.setPage(tempPageTableEntry);
				InterruptVector.setThread(thread);
				CPU.interrupt(PageFault);
				if(thread.getStatus() == GlobalVariables.ThreadKill)
				{
					return tempPageTableEntry;
				}
			}
			else
			{
				//if pagefault occurs due to some other thread, suspend this thread and wait
				thread.suspend(tempPageTableEntry);
				if(thread.getStatus() == GlobalVariables.ThreadKill)
				{
					return tempPageTableEntry;
				}
			}
		}
		tempPageTableEntry.getFrame().setReferenced(true);
		if(referenceType == GlobalVariables.MemoryWrite)
		{
			//set the dirty bit as true if reference type was write
			tempPageTableEntry.getFrame().setDirty(true);
		}
		return tempPageTableEntry;

    }

    /** Called by OSP after printing an error message. The student can
	insert code here to print various tables and data structures
	in their state just after the error happened.  The body can be
	left empty, if this feature is not used.
     
	@OSPProject Memory
     */
    public static void atError()
    {

    }

    /** Called by OSP after printing a warning message. The student
	can insert code here to print various tables and data
	structures in their state just after the warning happened.
	The body can be left empty, if this feature is not used.
     
      @OSPProject Memory
     */
    public static void atWarning()
    {

    }
}