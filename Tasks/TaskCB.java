package osp.Tasks;

import java.util.ArrayList;
import osp.IFLModules.*;
import osp.Threads.*;
import osp.Ports.*;
import osp.Memory.*;
import osp.FileSys.*;
import osp.Utilities.*;
import osp.Hardware.*;

/**
    The student module dealing with the creation and killing of
    tasks.  A task acts primarily as a container for threads and as
    a holder of resources.  Execution is associated entirely with
    threads.  The primary methods that the student will implement
    are do_create(TaskCB) and do_kill(TaskCB).  The student can choose
    how to keep track of which threads are part of a task.  In this
    implementation, an array is used.

    @OSPProject Tasks
*/
public class TaskCB extends IflTaskCB
{

    private ArrayList<ThreadCB> listOfThreads;
    private ArrayList<PortCB> listOfPorts;
    private ArrayList<OpenFile> listOfOpenFiles;
  
    /**
    Done
       The task constructor. Must have

           super();

       as its first statement.

       @OSPProject Tasks
    */
    public TaskCB()
    {
        // your code goes here
      super();
      listOfOpenFiles = new ArrayList<OpenFile>();
      listOfPorts = new ArrayList<PortCB>();
      listOfThreads = new ArrayList<ThreadCB>();

    }

    /**
    Done
       This method is called once at the beginning of the
       simulation. Can be used to initialize static variables.

       @OSPProject Tasks
    */
    public static void init()
    {
        // your code goes here

    }

    /** 
    Done
        Sets the properties of a new task, passed as an argument. 
        
        Creates a new thread list, sets TaskLive status and creation time,
        creates and opens the task's swap file of the size equal to the size
  (in bytes) of the addressable virtual memory.

  @return task or null

        @OSPProject Tasks
    */
    static public TaskCB do_create()
    {
        // your code goes here
      String swapFileAdd;
      
      TaskCB task = new TaskCB();

      PageTable pageTab = new PageTable(task);

      task.setPageTable(pageTab);

      task.setCreationTime(HClock.get());

      task.setStatus(TaskLive);

      task.setPriority(5);

      int swapFileSize = (int)Math.pow(2, MMU.getVirtualAddressBits());

      swapFileAdd = SwapDeviceMountPoint + task.getID();

      FileSys.create(swapFileAdd, swapFileSize);
      
      OpenFile swapFile =OpenFile.open(swapFileAdd, task);

      if (swapFile != null)
      {
        task.setSwapFile(swapFile);
        ThreadCB.create(task);
        return task;
      }
      else 
      {
        ThreadCB.dispatch();
        return null;
      }
    }

    /**
       Kills the specified task and all of it threads. 

       Sets the status TaskTerm, frees all memory frames 
       (reserved frames may not be unreserved, but must be marked 
       free), deletes the task's swap file.
  
       @OSPProject Tasks
    */
    public void do_kill()
    {
        // your code goes here
      String swapFileAdd = SwapDeviceMountPoint + this.getID();
      while(listOfThreads.size() > 0)
      {
        listOfThreads.get(0).kill();
      }
      
      while(listOfPorts.size() > 0)
      {
        listOfPorts.get(0).destroy();
      }

      this.setStatus(TaskTerm);

      this.getPageTable().deallocateMemory();

      int i;
      for (i = listOfOpenFiles.size() - 1; i >= 0; i--) 
      {
        if (listOfOpenFiles.get(i) != null) 
          listOfOpenFiles.get(i).close();
      }
    

      FileSys.delete(swapFileAdd);

    }

    /** 
  Done
  Returns a count of the number of threads in this task. 
  
  @OSPProject Tasks
    */
    public int do_getThreadCount()
    {
        // your code goes here
        return listOfThreads.size();
    }

    /**
    Done
       Adds the specified thread to this task. 
       @return FAILURE, if the number of threads exceeds MaxThreadsPerTask;
       SUCCESS otherwise.
       
       @OSPProject Tasks
    */
    public int do_addThread(ThreadCB thread)
    {
        // your code goes here
      if(do_getThreadCount() >= ThreadCB.MaxThreadsPerTask)
        return FAILURE;
      listOfThreads.add(thread);
      return SUCCESS;

    }

    /**
    Done
       Removes the specified thread from this task.     

       @OSPProject Tasks
    */
    public int do_removeThread(ThreadCB thread)
    {
        // your code goes here
      if (listOfThreads.size() == 0)
        return FAILURE;
      else if (listOfThreads.contains(thread)) {
        listOfThreads.remove(thread);
        return SUCCESS;
      }
      return FAILURE;
    }

    /**
    Done
       Return number of ports currently owned by this task. 

       @OSPProject Tasks
    */
    public int do_getPortCount()
    {
        // your code goes here
        return listOfPorts.size();
    }

    /**
    Done
       Add the port to the list of ports owned by this task.
  
       @OSPProject Tasks 
    */ 
    public int do_addPort(PortCB newPort)
    {
        // your code goes here
      if(do_getPortCount() >= PortCB.MaxPortsPerTask)
        return FAILURE;
      else
      {
      listOfPorts.add(newPort);
      return SUCCESS;
      }
    }

    /**
    Done
       Remove the port from the list of ports owned by this task.

       @OSPProject Tasks 
    */ 
    public int do_removePort(PortCB oldPort)
    {
        // your code goes here
       if (listOfPorts.size() == 0)
        return FAILURE;
      else if (listOfPorts.contains(oldPort)) 
      {
        listOfPorts.remove(oldPort);
        return SUCCESS;
      }
      return FAILURE;
    }

    /**
    Done
       Insert file into the open files table of the task.

       @OSPProject Tasks
    */
    public void do_addFile(OpenFile file)
    {
        // your code goes here
        listOfOpenFiles.add(file);
    }

    /** 
    Done
  Remove file from the task's open files table.

  @OSPProject Tasks
    */
    public int do_removeFile(OpenFile file)
    {
        // your code goes here
      if (listOfOpenFiles.size() == 0)
        return FAILURE;
      else if (listOfOpenFiles.contains(file)) {
        listOfOpenFiles.remove(file);
        return SUCCESS;
      }
      return FAILURE;
    }

    /**
    Done
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures
       in their state just after the error happened.  The body can be
       left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atError()
    {
        // your code goes here
      System.out.println("Error invoked");
    }

    /**
    Done
       Called by OSP after printing a warning message. The student
       can insert code here to print various tables and data
       structures in their state just after the warning happened.
       The body can be left empty, if this feature is not used.
       
       @OSPProject Tasks
    */
    public static void atWarning()
    {
        // your code goes here
      System.out.println("Warning invoked");

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
