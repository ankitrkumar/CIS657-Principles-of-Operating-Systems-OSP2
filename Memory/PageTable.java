package osp.Memory;

 /** Authors:
       Sayali Khodke
       Yogesh Chaudhari
       Ankit Kumar
       Group 4
       CIS 657
**/

/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
    /** 
	The page table constructor. Must call
	
	    super(ownerTask)

	as its first statement.

	@OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
      super(ownerTask);
  	/*
	page table is an array of size equal to the maximum number of pages allowed
	*/
	/*
	calculate maximal number of pages allowed
	*/
  	int numberOfPages = (int)Math.pow(2, MMU.getPageAddressBits());
    	
    	pages = new PageTableEntry[numberOfPages];
    	
    	
	/* initialize each page with page table entry */
for(int i = 0; i < numberOfPages; i++)
    	{
    		pages[i] = new PageTableEntry(this, i);
    	}

    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
        TaskCB task = getTask();
        
        for(int i = 0; i < MMU.getFrameTableSize(); i++)
        {
        	FrameTableEntry tempFrameTableEntry = MMU.getFrame(i);
        	PageTableEntry tempPageTableEntry = tempFrameTableEntry.getPage();
        	if(tempPageTableEntry != null && tempPageTableEntry.getTask() == task)
        	{
        		
		/* to nullify the page field that points to the page that occupies the frame */
		tempFrameTableEntry.setPage(null);
        
		// to clean the page	
		tempFrameTableEntry.setDirty(false);
        // to unset the reference bit
		tempFrameTableEntry.setReferenced(false);
 
		// un-reserves each frame that was reserved by that task    
   		if(tempFrameTableEntry.getReserved() == task)
        			tempFrameTableEntry.setUnreserved(task);
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
