package osp.Memory;

/** Authors:
Sayali Khodke
Yogesh Chaudhari
Ankit Kumar
Group 4
CIS 657
**/
import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
    The page fault handler is responsible for handling a page
    fault.  If a swap in or swap out operation is required, the page fault
    handler must request the operation.

    @OSPProject Memory
*/
public class PageFaultHandler extends IflPageFaultHandler
{
    /**
        This method handles a page fault. 

        It must check and return if the page is valid, 

        It must check if the page is already being brought in by some other
	thread, i.e., if the page's has already pagefaulted
	(for instance, using getValidatingThread()).
        If that is the case, the thread must be suspended on that page.
        
        If none of the above is true, a new frame must be chosen 
        and reserved until the swap in of the requested 
        page into this frame is complete. 

	Note that you have to make sure that the validating thread of
	a page is set correctly. To this end, you must set the page's
	validating thread using setValidatingThread() when a pagefault
	happens and you must set it back to null when the pagefault is over.

        If a swap-out is necessary (because the chosen frame is
        dirty), the victim page must be dissasociated 
        from the frame and marked invalid. After the swap-in, the 
        frame must be marked clean. The swap-ins and swap-outs 
        must are preformed using regular calls read() and write().

        The student implementation should define additional methods, e.g, 
        a method to search for an available frame.

	Note: multiple threads might be waiting for completion of the
	page fault. The thread that initiated the pagefault would be
	waiting on the IORBs that are tasked to bring the page in (and
	to free the frame during the swapout). However, while
	pagefault is in progress, other threads might request the same
	page. Those threads won't cause another pagefault, of course,
	but they would enqueue themselves on the page (a page is also
	an Event!), waiting for the completion of the original
	pagefault. It is thus important to call notifyThreads() on the
	page at the end -- regardless of whether the pagefault
	succeeded in bringing the page in or not.

        @param thread the thread that requested a page fault
        @param referenceType whether it is memory read or write
        @param page the memory page 

	@return SUCCESS is everything is fine; FAILURE if the thread
	dies while waiting for swap in or swap out or if the page is
	already in memory and no page fault was necessary (well, this
	shouldn't happen, but...). In addition, if there is no frame
	that can be allocated to satisfy the page fault, then it
	should return NotEnoughMemory

        @OSPProject Memory
    */
    public static int do_handlePageFault(ThreadCB thread, 
					 int referenceType,
					 PageTableEntry page)
    {
        TaskCB Task = thread.getTask();
		//check if page is valid
    	if(page.isValid())
    	{
    		return FAILURE;
    	}
    	    	
    	FrameTableEntry newFrame = null;
    	
    	newFrame = GetNewFrame();
    	//check if memory is available
    	if(newFrame == null)
    	{
    		return NotEnoughMemory;
    	}
    	
    	Event event = new SystemEvent("PageFaultHappened");
    	thread.suspend(event);
		
    	 //set the thread and the reserve the frame for swapping   	
    	page.setValidatingThread(thread);
    	newFrame.setReserved(thread.getTask());
		
    	if(newFrame.getPage() != null)
    	{
    		PageTableEntry newPage = newFrame.getPage();
    		if(newFrame.isDirty())
    		{
    			SwapOut(thread, newFrame);
    			
    			if(thread.getStatus() == GlobalVariables.ThreadKill)
    			{
    				page.notifyThreads();
    				event.notifyThreads();
    				ThreadCB.dispatch();
    				return FAILURE;
    			}
    			newFrame.setDirty(false);
    		}
    		newFrame.setReferenced(false);
    		newFrame.setPage(null);
    		newPage.setValid(false);
    		newPage.setFrame(null);
    	}
    	
    	page.setFrame(newFrame);
    	SwapIn(thread, page);
    	//if the thread is terminated, set the page as null and notify all the threads
    	if(thread.getStatus() == ThreadKill)
    	{
    		if(newFrame.getPage() != null)
    		{
    			if(newFrame.getPage().getTask() == thread.getTask())
    			{
	    			newFrame.setPage(null);
	    		}
    		}
    		page.notifyThreads();
    		page.setValidatingThread(null);
    		page.setFrame(null);
    		event.notifyThreads();
    		ThreadCB.dispatch();
    		return FAILURE;
    	}

    	newFrame.setPage(page);
    	page.setValid(true);
    	    
    	//if the new frame is reserved unreserve it
    	if(newFrame.getReserved() == Task)
    	{
    		newFrame.setUnreserved(Task);    		
    	}
    	

    	page.setValidatingThread(null);
    	page.notifyThreads();
    	event.notifyThreads();
    	ThreadCB.dispatch();
    	return SUCCESS;
	}
	
	//get a new frame for the page fault handler
    private static FrameTableEntry GetNewFrame()
    {
    	FrameTableEntry newFrame = null;
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((newFrame.getPage() == null) && (!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame;
    		}
    	}
    	
    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((!newFrame.isDirty()) && (!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; 
    		}
    	}

    	for(int i = 0; i < MMU.getFrameTableSize(); i++)
    	{
    		newFrame = MMU.getFrame(i);
    		if((!newFrame.isReserved()) && (newFrame.getLockCount() == 0))
    		{
    			return newFrame; 
    		}
    	}
    	return MMU.getFrame(MMU.getFrameTableSize() - 1);
    }
    
	//Swap in the page
    public static void SwapIn(ThreadCB thread, PageTableEntry page)
    {
    	TaskCB newTask = page.getTask();
    	newTask.getSwapFile().read(page.getID(), page, thread);
    }
	
    //Swap out the page
    public static void SwapOut(ThreadCB thread, FrameTableEntry frame)
    {
    	PageTableEntry newPage = frame.getPage();
    	TaskCB newTask = newPage.getTask();
    	newTask.getSwapFile().write(newPage.getID(), newPage, thread);
    }

	public static void init() {

	}
	
	static void StarvationAvoid(ThreadCB thread)
	{
		FrameTableEntry frame = GetNewFrame();
		if(frame == null)
		{
			return;
		}
		else
		{
			if(frame.getPage() != null)
			{
				TaskCB task = frame.getPage().getTask();
				
				task.getSwapFile().write(frame.getPage().getID(), frame.getPage(), thread);
				frame.setDirty(false);
			}
		}
	}



    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
