package nachos.threads;

import java.util.Comparator;
import java.util.HashMap;


import nachos.machine.Lib;
import nachos.machine.Machine;

/**
 * A scheduler that chooses threads based on their priorities.
 * 
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the thread
 * that has been waiting longest.
 * 
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fassion to
 * all the highest-priority threads, and ignores all other threads. This has the
 * potential to starve a thread if there's always a thread waiting with higher
 * priority.
 * 
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
	
	
	/**
	 * Allocate a new priority scheduler.
	 */
	public PriorityScheduler() {
	}

	/**
	 * Allocate a new priority thread queue.
	 * 
	 * @param transferPriority <tt>true</tt> if this queue should transfer
	 * priority from waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}

	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}

	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}

	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum
				&& priority <= priorityMaximum);

		getThreadState(thread).setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			ret = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable();
		boolean ret = true;

		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			ret = false;
		else
			setPriority(thread, priority - 1);

		Machine.interrupt().restore(intStatus);
		return ret;
	}

	/**
	 * The default priority for a new thread. Do not change this value.
	 */
	public static final int priorityDefault = 1;

	/**
	 * The minimum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMinimum = 0;

	/**
	 * The maximum priority that a thread can have. Do not change this value.
	 */
	public static final int priorityMaximum = 7;

	/**
	 * Return the scheduling state of the specified thread.
	 * 
	 * @param thread the thread whose scheduling state to return.
	 * @return the scheduling state of the specified thread.
	 */
	protected ThreadState getThreadState(KThread thread) {
		if (thread.schedulingState == null)
			thread.schedulingState = new ThreadState(thread);

		return (ThreadState) thread.schedulingState;
	}

	/**
	 * A <tt>ThreadQueue</tt> that sorts threads by priority.
	 */
	protected class PriorityQueue extends ThreadQueue {		
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}
		public PriorityQueue() {
		}
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}

		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}

		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me
			//project 1.5
			if (waitingQueue.isEmpty()) 
				return null;
			acquire(waitingQueue.poll().thread);
			return lockerThread;
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
			return waitingQueue.peek();
		}

		public void print() {
			Lib.assertTrue(Machine.interrupt().disabled());
			// implement me (if you want)
		}
		
		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		private java.util.PriorityQueue<ThreadState> waitingQueue = new java.util.PriorityQueue<ThreadState>(8,new ThreadStateComparator<ThreadState>(this));
		private KThread lockerThread = null;
		
		protected class ThreadStateComparator<T extends ThreadState> implements Comparator<T> {
			
			private nachos.threads.PriorityScheduler.PriorityQueue priorityQueue;
			
			protected ThreadStateComparator(nachos.threads.PriorityScheduler.PriorityQueue key) {
				priorityQueue = key;
			}

			@Override
			public int compare(T o1, T o2) {
				int effectivePriority1 = o1.getEffectivePriority();
				int effectivePriority2 = o2.getEffectivePriority();
				if (effectivePriority1 > effectivePriority2)
					return -1;
				else if (effectivePriority1 < effectivePriority2) 
					return 1;
				else 
					return Long.signum(o1.waitingMap.get(priorityQueue) - o2.waitingMap.get(priorityQueue));
			}
		}
	}

	/**
	 * The scheduling state of a thread. This should include the thread's
	 * priority, its effective priority, any objects it owns, and the queue it's
	 * waiting for, if any.
	 * 
	 * @see nachos.threads.KThread#schedulingState
	 */
	protected class ThreadState {
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread the thread this state belongs to.
		 */
		public ThreadState(KThread thread) {
			this.thread = thread;
			effectivePriority = priorityDefault;
			setPriority(priorityDefault);
		}

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		public int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
		public int getEffectivePriority() {
			// implement me
			//project 1.5
			return effectivePriority;
		}

		/**
		 * Set the priority of the associated thread to the specified value.
		 * 
		 * @param priority the new priority.
		 */
		public void setPriority(int priority) {
			// implement me
			this.priority = priority;
			updateEffectivePriority();
		}
		protected void updateEffectivePriority() {
			for (PriorityQueue key : waitingMap.keySet())
				key.waitingQueue.remove(this);
			
			int currPriority = priority;

			if (acquired.transferPriority) {
				ThreadState highestThread = acquired.waitingQueue.peek();
				if (highestThread != null) {
					int highestPriority = highestThread.getEffectivePriority();
					if (highestPriority > currPriority)
						currPriority = highestPriority;
				}
			}

			boolean ifInheritance = currPriority != effectivePriority;

			effectivePriority = currPriority;
			
			for (PriorityQueue key : waitingMap.keySet())
				key.waitingQueue.add(this);

			if (ifInheritance) {
				for (PriorityQueue key : waitingMap.keySet()) {
					if (key.transferPriority && key.lockerThread != null)
						getThreadState(key.lockerThread).updateEffectivePriority();
				}
			}
		}
		/**
		 * Called when <tt>waitForAccess(thread)</tt> (where <tt>thread</tt> is
		 * the associated thread) is invoked on the specified priority queue.
		 * The associated thread is therefore waiting for access to the resource
		 * guarded by <tt>waitQueue</tt>. This method is only called if the
		 * associated thread cannot immediately obtain access.
		 * 
		 * @param waitQueue the queue that the associated thread is now waiting
		 * on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			//project 1.5
			if (!waitingMap.containsKey(waitQueue)) {
				waitingMap.put(waitQueue, Machine.timer().getTime());
				waitQueue.waitingQueue.add(this);
				if (waitQueue.lockerThread != null) {
					getThreadState(waitQueue.lockerThread).updateEffectivePriority();
				}
			}
		}

		/**
		 * Called when the associated thread has acquired access to whatever is
		 * guarded by <tt>waitQueue</tt>. This can occur either as a result of
		 * <tt>acquire(thread)</tt> being invoked on <tt>waitQueue</tt> (where
		 * <tt>thread</tt> is the associated thread), or as a result of
		 * <tt>nextThread()</tt> being invoked on <tt>waitQueue</tt>.
		 * 
		 * @see nachos.threads.ThreadQueue#acquire
		 * @see nachos.threads.ThreadQueue#nextThread
		 */
		public void acquire(PriorityQueue waitQueue) {
			// implement me
			//project 1.5
			waitQueue.waitingQueue.remove(this);
			waitQueue.lockerThread = this.thread;
			acquired = waitQueue;
			waitingMap.remove(waitQueue);
			updateEffectivePriority();
		}
		
		/** The thread with which this object is associated. */
		protected KThread thread;

		/** The priority of the associated thread. */
		protected int priority;		

		private PriorityQueue acquired = new PriorityQueue();
		protected int effectivePriority;
		
		public HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long> waitingMap = new HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long>();
	}
}
