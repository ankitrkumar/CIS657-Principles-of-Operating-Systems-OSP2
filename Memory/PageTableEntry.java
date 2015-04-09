package osp.Memory;

 /** Authors:
       Sayali Khodke
       Yogesh Chaudhari
       Ankit Kumar
       Group 4
       CIS 657
**/

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.
   
   @OSPProject Memory

*/

public class PageTableEntry extends IflPageTableEntry
{
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);
	   
       as its first statement.

       @OSPProject Memory
    */
	boolean pageFaulted = false;
	
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
        super(ownerPageTable, pageNumber);

    }

    /**
       This method increases the lock count on the page by one. 

	The method must FIRST increment lockCount, THEN  
	check if the page is valid, and if it is not and no 
	page validation event is present for the page, start page fault 
	by calling PageFaultHandler.handlePageFault().

	@return SUCCESS or FAILURE
	FAILURE happens when the pagefault due to locking fails or the 
	that created the IORB thread gets killed.

	@OSPProject Memory
     */
    public int do_lock(IORB iorb)
    {
       ThreadCB thread = iorb.getThread();
    	
    	// testing page validity
	
    	if(!isValid())
	    {
		// if page is invalid

		/* 
			to identify pages that are involved in pagefault 			*/
		
	    	if(getValidatingThread() == null)
	    	{
			/*
			call static pageFaultHandler() to initiate page 				fault
			*/ 

	    		PageFaultHandler.handlePageFault(thread, GlobalVariables.MemoryLock, this);
	    	}
	    	else
	    	{
			// if page is valid

			/* 
			to identify pages that are involved in pagefault 			*/			

	    		if(getValidatingThread() != thread)
	            {
				
	            	thread.suspend(this);
	            	if(thread.getStatus() == GlobalVariables.ThreadKill)
	                {
	                	return GlobalVariables.FAILURE;
	                }
	            }
	    	}
    	}
	/*
        threads waiting on the page will be unblocked
	by the pagefault handler
	*/

	/*
	increment the lock count of the frame associated with the page
	*/	

    	getFrame().incrementLockCount();
    	return GlobalVariables.SUCCESS;
    	

    }

    /** This method decreases the lock count on the page by one. 

	This method must decrement lockCount, but not below zero.

	@OSPProject Memory
    */
    public void do_unlock()
    {
	// decrement the lock count
        getFrame().decrementLockCount();

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
