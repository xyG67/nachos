package nachos.threads;

<<<<<<< HEAD
import nachos.machine.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
=======
import java.util.Comparator;
import java.util.HashMap;

>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1


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
 * Essentially, a priority scheduler gives access in a round-robin fashion to
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
	 * @param transferPriority
	 *            <tt>true</tt> if this queue should transfer priority from
	 *            waiting threads to the owning thread.
	 * @return a new priority thread queue.
	 */
	public ThreadQueue newThreadQueue(boolean transferPriority) {
		return new PriorityQueue(transferPriority);
	}
	/** 
	 * get the priority of given thread
	 * @param the given thread
	 */
	public int getPriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getPriority();
	}
	/**
	 * get the effective priority of given thread
	 * @param the given thread
	 */
	public int getEffectivePriority(KThread thread) {
		Lib.assertTrue(Machine.interrupt().disabled());

		return getThreadState(thread).getEffectivePriority();
	}
	/**
	 * assign the priority to the given thread
	 * @param thread, given thread
	 * @param priority, the priority of given thread
	 */
	public void setPriority(KThread thread, int priority) {
		Lib.assertTrue(Machine.interrupt().disabled());

		Lib.assertTrue(priority >= priorityMinimum && priority <= priorityMaximum);
		
		ThreadState ts = getThreadState(thread);
		
	
		if (priority != ts.getPriority())
			ts.setPriority(priority);
	}

	public boolean increasePriority() {
		boolean intStatus = Machine.interrupt().disable(), returnBool = true;

		KThread thread = KThread.currentThread();
		
		int priority = getPriority(thread);
		if (priority == priorityMaximum)
			returnBool = false;
		else
			setPriority(thread, priority + 1);

		Machine.interrupt().restore(intStatus);
		return returnBool;
	}

	public boolean decreasePriority() {
		boolean intStatus = Machine.interrupt().disable(), returnBool = true;
		
		KThread thread = KThread.currentThread();

		int priority = getPriority(thread);
		if (priority == priorityMinimum)
			returnBool = false;
		else
			setPriority(thread, priority - 1);
		
		Machine.interrupt().restore(intStatus);
		return returnBool;
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
	 * @param thread
	 *            the thread whose scheduling state to return.
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
<<<<<<< HEAD
	protected class PriorityQueue extends ThreadQueue {
		
		boolean transferPriority;
		private java.util.PriorityQueue<ThreadState> waitQ = new java.util.PriorityQueue<ThreadState>(8,new ThreadStateComparator<ThreadState>(this));
		private KThread lockingThread = null;
		
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}

		public PriorityQueue() {
		}
		/**
		 * implement by threadstate
		 */
=======
	protected class PriorityQueue extends ThreadQueue {		
		PriorityQueue(boolean transferPriority) {
			this.transferPriority = transferPriority;
		}
		public PriorityQueue() {
		}
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
		public void waitForAccess(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).waitForAccess(this);
		}
		/**
		 * implement by threadstate
		 */
		public void acquire(KThread thread) {
			Lib.assertTrue(Machine.interrupt().disabled());
			getThreadState(thread).acquire(this);
		}
		/**
		 * implement by threadstate
		 */
		public KThread nextThread() {
			Lib.assertTrue(Machine.interrupt().disabled());
<<<<<<< HEAD
			if (waitQ.isEmpty()) {
				return null;
			} else {
				acquire(waitQ.poll().thread);

				return lockingThread;
			}
=======
			// implement me
			//project 1.5
			if (waitingQueue.isEmpty()) 
				return null;
			acquire(waitingQueue.poll().thread);
			return lockerThread;
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
		}

		/**
		 * Return the next thread that <tt>nextThread()</tt> would return,
		 * without modifying the state of this queue.
		 * 
		 * @return the next thread that <tt>nextThread()</tt> would return.
		 */
		protected ThreadState pickNextThread() {
<<<<<<< HEAD
			return waitQ.peek();
=======
			return waitingQueue.peek();
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
		}

		public void print() {
		}
		
<<<<<<< HEAD
		protected class ThreadStateComparator<T extends ThreadState> implements Comparator<T> {
			private nachos.threads.PriorityScheduler.PriorityQueue priorityQueue;
			protected ThreadStateComparator(nachos.threads.PriorityScheduler.PriorityQueue wq) {
				priorityQueue = wq;
			}
			/**
			 * define a comparator, sorted by priority, if they are equal, then sorted by the time of 
			 * their joining
			 */
			@Override
			public int compare(T o1, T o2) {
				int effprio1 = o1.getEffectivePriority(), effprio2 = o2.getEffectivePriority();
				if (effprio1 > effprio2) {
					return -1;
				} else if (effprio1 < effprio2) {
					return 1;
				} else {
					long wait1 = o1.waiting.get(priorityQueue), wait2 = o2.waiting.get(priorityQueue);
					return (int) (wait1-wait2);
				}
			}
			
=======
		/**
		 * <tt>true</tt> if this queue should transfer priority from waiting
		 * threads to the owning thread.
		 */
		public boolean transferPriority;
		
		private java.util.PriorityQueue<ThreadState> waitingQueue = new java.util.PriorityQueue<ThreadState>(8,new ThreadStateComparator(this));
		private KThread lockerThread = null;
		
		protected class ThreadStateComparator implements Comparator<ThreadState> {
			
			private nachos.threads.PriorityScheduler.PriorityQueue priorityQueue;
			
			protected ThreadStateComparator(nachos.threads.PriorityScheduler.PriorityQueue key) {
				priorityQueue = key;
			}

			@Override
			public int compare(ThreadState o1, ThreadState o2) {
				int effectivePriority1 = o1.getEffectivePriority();
				int effectivePriority2 = o2.getEffectivePriority();
				if (effectivePriority1 > effectivePriority2)
					return -1;
				else if (effectivePriority1 < effectivePriority2) 
					return 1;
				else 
					return Long.signum(o1.waitingMap.get(priorityQueue) - o2.waitingMap.get(priorityQueue));
			}
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
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

		/** The thread with which this object is associated. */
		protected KThread thread;
		/** The priority of the associated thread. */
		protected int priority;
		protected int effprio;
		
		private PriorityQueue acquired = new PriorityQueue();
		
		public HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long> waiting = new HashMap<nachos.threads.PriorityScheduler.PriorityQueue,Long>();
		
		/**
		 * Allocate a new <tt>ThreadState</tt> object and associate it with the
		 * specified thread.
		 * 
		 * @param thread
		 *            the thread this state belongs to.
		 */
		ThreadState(KThread thread) {
			this.thread = thread;
<<<<<<< HEAD
			effprio = priorityDefault;
=======
			effectivePriority = priorityDefault;
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
			setPriority(priorityDefault);
		}

		/**
		 * Release this priority queue from the resources this ThreadState has locked.
		 * <p>
		 * This is the only time the effective priority of a thread can go down and needs a full recalculation.
		 * <p>
		 * We can detect if this exists if the top effective priority of the queue we are release is equal to this current effective priority.
		 * If it is less than (it cannot be greater by definition), then we know that something else is contributing to the effective priority of <tt>this</tt>.
		 * @param priorityQueue
		 */
		

		/**
		 * Return the priority of the associated thread.
		 * 
		 * @return the priority of the associated thread.
		 */
		int getPriority() {
			return priority;
		}

		/**
		 * Return the effective priority of the associated thread.
		 * 
		 * @return the effective priority of the associated thread.
		 */
<<<<<<< HEAD
		int getEffectivePriority() {
			return effprio;
=======
		public int getEffectivePriority() {
			// implement me
			//project 1.5
			return effectivePriority;
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
		}

		/**
		 * Set the priority of the associated thread to the specified value. <p>
		 * This method assumes the priority has changed. Protection is from PriorityScheduler class calling this.
		 * @param priority
		 *            the new priority.
		 */
<<<<<<< HEAD
		void setPriority(int priority) {
			this.priority = priority;
			updateEffectivePriority();
		}
		/**
		 * update the effective priority, it will invoked by acquire.
		 * use the priority of the top of the priority queue
		 */
		protected void updateEffectivePriority() {
			for (PriorityQueue wq : waiting.keySet())
				wq.waitQ.remove(this);
			int temp = priority;
			// give the top priority
			if (acquired.transferPriority) {
				ThreadState topTS = acquired.waitQ.peek();
				if (topTS != null) {
					int topPQ_AP = topTS.getEffectivePriority();
			
					if (topPQ_AP > temp)
						temp = topPQ_AP;
				}
			}
			
			
			boolean ifTransfer = temp != effprio;
			
			effprio = temp;
			for (PriorityQueue wq : waiting.keySet())
				wq.waitQ.add(this);

			if (ifTransfer)
				for (PriorityQueue wq : waiting.keySet()) {
					if (wq.transferPriority && wq.lockingThread != null)
						getThreadState(wq.lockingThread).updateEffectivePriority();
				}
=======
		public void setPriority(int priority) {
			// implement me
			this.priority = priority;
			updateEffectivePriority();
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
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
		 * @param priorityQ
		 *            the queue that the associated thread is now waiting on.
		 * 
		 * @see nachos.threads.ThreadQueue#waitForAccess
		 */
<<<<<<< HEAD
		void waitForAccess(PriorityQueue waitQueue) {
			if (!waiting.containsKey(waitQueue)) {
				
				
				waiting.put(waitQueue, Machine.timer().getTime());
				
				
				waitQueue.waitQ.add(this);
				
				if (waitQueue.lockingThread != null) {
					getThreadState(waitQueue.lockingThread).updateEffectivePriority();
=======
		public void waitForAccess(PriorityQueue waitQueue) {
			// implement me
			//project 1.5
			if (!waitingMap.containsKey(waitQueue)) {
				waitingMap.put(waitQueue, Machine.timer().getTime());
				waitQueue.waitingQueue.add(this);
				if (waitQueue.lockerThread != null) {
					getThreadState(waitQueue.lockerThread).updateEffectivePriority();
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
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
<<<<<<< HEAD
		void acquire(PriorityQueue waitQueue) {
			
			waitQueue.waitQ.remove(this);
			
			waitQueue.lockingThread = this.thread;
			acquired=waitQueue;
			
			waiting.remove(waitQueue);
			
			updateEffectivePriority();
		}
	}
	/**
	 * create 4 thread.
	 * add the first two threads into scheduler.
	 * the second thread will be acquired.
	 * set the priority of the first thread as 6, and check the second thread's effect priority.
	 * create 5th thread, and set its priority as 7.
	 * add 5th thread into scheduler
	 */
	public static void selfTest1() {
		ThreadQueue tq1 = ThreadedKernel.scheduler.newThreadQueue(true);
		
		KThread kt_1 = new KThread(), kt_2 = new KThread(), kt_3 = new KThread(), kt_4 = new KThread();
		
		boolean status = Machine.interrupt().disable();
		
		tq1.waitForAccess(kt_1);
		tq1.waitForAccess(kt_2);
		tq1.waitForAccess(kt_3);
		tq1.waitForAccess(kt_4);
		tq1.acquire(kt_2);
		
		System.out.println("kt_2\t"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
		
		
		ThreadedKernel.scheduler.setPriority(kt_1, 2);
		System.out.println("kt_2\t"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_2)==2);
		
		ThreadedKernel.scheduler.setPriority(kt_3, 5);
		System.out.println("kt_2\t"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_2)==5);
		
		ThreadedKernel.scheduler.setPriority(kt_4, 3);
		System.out.println("kt_2\t"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));		
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_2)==5);
=======
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
	
	public static void selfTest1() {
		ThreadQueue tq1 = ThreadedKernel.scheduler.newThreadQueue(true);
		
		KThread t1 = new KThread(), t2 = new KThread();
		
		boolean status = Machine.interrupt().disable();
		
		tq1.waitForAccess(t1);
		tq1.waitForAccess(t2);
		//tq1.waitForAccess(kt_3);
		
		tq1.acquire(t2);
		
		System.out.println("t2\t"+ThreadedKernel.scheduler.getEffectivePriority(t2));
		ThreadedKernel.scheduler.setPriority(t1, 5);
		System.out.println("t2\t"+ThreadedKernel.scheduler.getEffectivePriority(t2));
		
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(t2)==5);
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
		
		KThread kt_5 = new KThread();
		
		ThreadedKernel.scheduler.setPriority(kt_5, 6);
		
		tq1.waitForAccess(kt_5);
		
<<<<<<< HEAD
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(kt_2)==6);
		System.out.println("kt_2\t"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
		tq1.nextThread();
		
		System.out.println("kt_2\t"+ThreadedKernel.scheduler.getEffectivePriority(kt_2));
=======
		Lib.assertTrue(ThreadedKernel.scheduler.getEffectivePriority(t2)==6);
		System.out.println("t2\t"+ThreadedKernel.scheduler.getEffectivePriority(t2));
		tq1.nextThread();
		
		System.out.println("t2\t"+ThreadedKernel.scheduler.getEffectivePriority(t2));
>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
		
		
		Machine.interrupt().restore(status);
	}
<<<<<<< HEAD
}
=======
}

>>>>>>> fbbe5ee5638b6aa46c16a8ce09987b9d30aef4a1
