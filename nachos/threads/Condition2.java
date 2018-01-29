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

	private Lock conditionLock;
	private LinkedList<KThread> waitQueue;
}
