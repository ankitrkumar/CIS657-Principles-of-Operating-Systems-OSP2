package osp.Threads;
import java.util.Vector;
import java.util.Enumeration;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.EventEngine.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads
*/
public class ThreadCB extends IflThreadCB 
{
	/**
       The thread constructor. Must call 

       	   super();

       as its first statement.

       @OSPProject Threads
    */
    public ThreadCB()
    {
        // call to super
    	super();

    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       @OSPProject Threads
    */
    public static void init()
    {
        // initialising a array list for ready queue
    	threadlist = new ArrayList<ThreadCB>();

    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status 
        and attempt to add thread to task. If the latter fails 
        because there are already too many threads in this task, 
        so does this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

	The priority of the thread can be set using the getPriority/setPriority
	methods. However, OSP itself doesn't care what the actual value of
	the priority is. These methods are just provided in case priority
	scheduling is required.

	@return thread or null

        @OSPProject Threads
    */
    static public ThreadCB do_create(TaskCB task)
    {
        // check if max thread count has baan reached. If yes, do nothing.
    	if(task.getThreadCount()>= MaxThreadsPerTask){
    		dispatch();
    		return null;
    	}
    	ThreadCB newThread = new ThreadCB(); 			//create new thread
    	newThread.setPriority(task.getPriority()); 		//set priority of the thread to that of task
    	newThread.setStatus(ThreadReady);				//set status of the thread as ready
    	newThread.setTask(task);						//set task to the thread
    	
    	if(task.addThread(newThread) != SUCCESS){ 		//attach thread to the task
    		dispatch();
    		return null;
    	}
    		threadlist.add(newThread);					//add the thread to ready queue
    	dispatch();
    	return newThread;
    	}

    }

    /** 
	Kills the specified thread. 

	The status must be set to ThreadKill, the thread must be
	removed from the task's list of threads and its pending IORBs
	must be purged from all device queues.
        
	If some thread was on the ready queue, it must removed, if the 
	thread was running, the processor becomes idle, and dispatch() 
	must be called to resume a waiting thread.
	
	@OSPProject Threads
    */
    public void do_kill()
    {
        
    	TaskCB task = getTask();
    	
    	//if status of the thread is ready, remove it from the ready queue
    	if(getStatus() == ThreadReady){
    		if(threadlist.remove(this) == false){
    			return;
    		}
    	}
    	
    	//if thread is running, stop the thread
    	if(getStatus() == ThreadRunning){
    		MMU.getPTBR().getTask().setCurrentThread(null);
    	}
    	
    	
    	if(task.removeThread(this != SUCCESS)){
    		return;
    	}
    	
    	//set status of the thread as terminated
    	setStatus(ThreadKill);
    	
    	//cancel all the pending IO requests of the thread
    	for(int i =0; i<Device.getTableSize();i++){
    		Device.get(i).cancelPendingIO(this);
    	}
    	
    	//release the resources used by the thread
    	ResouceCB.giveupResources(this);
    	
    	dispatch();
    	
    	//if there are to threads, kill the task
    	if(task.getThreadCount() == 0){
    		task.kill();
    	}

    }

    /** Suspends the thread that is currenly on the processor on the 
        specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
	Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

	@param event - event on which to suspend this thread.

        @OSPProject Threads
    */
    public void do_suspend(Event event)
    {
        
    	int status = this.getStatus();
    	
    	
    	if( status == ThreadRunning){					//if the thread is running, updae the status to waiting
    		setStatus(ThreadWaiting);
    		this.getTask().setCurrentThread(null);		//set the current thread as null
    	}
    	else if(status > = ThreadWaiting){				//if thread is already waiting, update the status by one level
    		setStatus(status+1);
    	}
    	
    	
    	threadlist.remove(this);						//remove the thread from the ready queue
    	event.addThread(this);							//add the thread to the waiting queue
    	dispatch();
    }

    /** Resumes the thread.
        
	Only a thread with the status ThreadWaiting or higher
	can be resumed.  The status must be set to ThreadReady or
	decremented, respectively.
	A ready thread should be placed on the ready queue.
	
	@OSPProject Threads
    */
    public void do_resume()
    {
        
    	int status = getStatus();
    	
    	if(status == ThreadWaiting){					//if the thread is waiting at level one put it into the ready queue
    		setStatus(ThreadReady);
    		threadlist.add(this);
    	}
    	else{
    		setStatus(status - 1);						//if thread is waiting at higher level, decrease its wait state
    	}
    	
    	dispatch();

    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one theread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
	@return SUCCESS or FAILURE

        @OSPProject Threads
    */
    public static int do_dispatch()
    {
        TaskCB task = null;
    	ThreadCB thread = null;
    	ThreadCB newThread = null;
    	
    	try{		
    		task = MMU.getPTBR().getTask();				//get the current task
    		thread = task.getCurrentThread();			//get the current thread
    	}catch(Exception e)
    	{}
    	if(thread != null){								//free the resources and CPU from the running thread
    		task.setCurrentThread(null);				//set the currrent thread as null
    		thread.setStatus(ThreadReady);				//change the status of running thread to ready
    		MMU.setPTBR(null);
    		threadlist.add(thread);						//add the thread to ready queue
    	}
    	if(threadlist.size()>0){						//call the new thread to start execution
    		newthread = threadlist.remove(0);			//take the first thread from ready queue
    		MMU.setPTBR(newthread.getTask().getPageTable());
    		newthread.getTask.setCurrentThread(newthread);
    		newthread.setStatus(ThreadRunning);
    		
    		HTimer.set(200);							//start the timer for execution
    		
    		return SUCCESS;
    	}
    	
    	MMU.setPTBR(null);
    	return FAILURE;
    		
    	

    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
