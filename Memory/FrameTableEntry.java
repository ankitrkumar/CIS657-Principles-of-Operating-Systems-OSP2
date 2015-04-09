package osp.Memory;

 /** Authors:
       Sayali Khodke
       Yogesh Chaudhari
       Ankit Kumar
       Group 4
       CIS 657
**/

/**
    The FrameTableEntry class contains information about a specific page
    frame of memory.

    @OSPProject Memory
*/
import osp.Tasks.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.IflFrameTableEntry;

public class FrameTableEntry extends IflFrameTableEntry
{
    /**
       The frame constructor. Must have

       	   super(frameID)
	   
       as its first statement.

       @OSPProject Memory
    */
    public FrameTableEntry(int frameID)
    {
        /* calling IflFrameTableEntry Constructor for 					initialization 
	   */
		super(frameID);
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
