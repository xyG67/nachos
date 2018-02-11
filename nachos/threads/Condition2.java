package nachos.threads;

import java.util.LinkedList;

import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;
		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		//Project 1.2
		boolean intStatus = Machine.interrupt().disable();//disable the interrupt
		waitQueue.add(KThread.currentThread());//put current thread inn queue
		//release Lock
		conditionLock.release();
		//
		KThread.sleep();
		//acquire Lock
		conditionLock.acquire();
		//
		Machine.interrupt().restore(intStatus);//enable the interrupt
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		//Project 1.2
		boolean intStatus = Machine.interrupt().disable();//disable the interrupt
		if (!waitQueue.isEmpty())
			waitQueue.removeFirst().ready();
		
		Machine.interrupt().restore(intStatus);//enable the interrupt
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
		//Project 1,2
		while(!waitQueue.isEmpty()) {
			wake();	//use function wake() 
		}
	}

///---------------------------
	
	 private static class Condition2Test implements Runnable {
			Condition2Test(Lock lock, Condition2 condition) {
			    this.condition = condition;
		        this.lock = lock;
			}
			
			public void run() {
		        lock.acquire();

		        System.out.print(KThread.currentThread().getName() + " acquired lock\n");	
		        condition.sleep();
		        System.out.print(KThread.currentThread().getName() + " acquired lock again\n");	

		        lock.release();
		        System.out.print(KThread.currentThread().getName() + " released lock \n");	
			}

		    private Lock lock; 
		    private Condition2 condition; 
	}
    /**
     * Test if this module is working.
     */
    public static void selfTest() {

    System.out.print("Enter Condition2.selfTest\n");	

    Lock lock = new Lock();
    Condition2 condition = new Condition2(lock); 

    KThread t[] = new KThread[10];
	for (int i=0; i<t.length; i++) {
         t[i] = new KThread(new Condition2Test(lock, condition));
         t[i].setName("Thread" + i).fork();
	}

    KThread.yield();
    
    lock.acquire();

    System.out.print("condition.wake();\n");	
    condition.wake();

    System.out.print("condition.wakeAll();\n");	
    condition.wakeAll();

    lock.release();

    System.out.print("Leave Condition2.selfTest\n");	

    t[9].join();
        
    }
	
	private Lock conditionLock;
	private LinkedList<KThread> waitQueue;
}
