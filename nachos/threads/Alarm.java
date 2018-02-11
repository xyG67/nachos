package nachos.threads;

import java.util.LinkedList;
import java.util.TreeSet;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		waiting = new TreeSet<waiter>();
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {
		//KThread.currentThread().yield();
		//Project 1.3
		waiter waitThread;
		
		if(waiting.isEmpty()) {
			return;
		}
		if(waiting.first().wakeTime > Machine.timer().getTime()){
			return;
		}
		Lib.debug(dbgInt, "Invoking Alarm.timerInterrupt at time = " + Machine.timer().getTime());
		
		while(!waiting.isEmpty()&&waiting.first().wakeTime <= Machine.timer().getTime()) {
			waiter next = waiting.first();
			next.thread.ready();
			waiting.remove(next);
			Lib.assertTrue(next.wakeTime <= Machine.timer().getTime());
			
			Lib.debug(dbgInt, "  " + next.thread.getName());
		}
		Lib.debug(dbgInt, "  (end of Alarm.timerInterrupt)");
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		boolean intStatus = Machine.interrupt().disable();//disable interrupt
		long wakeTime = Machine.timer().getTime() + x;
		
		waiter waitThread = new waiter(wakeTime, KThread.currentThread());
		waiting.add(waitThread);
		
		
		System.out.println(KThread.currentThread().getName() + "sleep at "+ Machine.timer().getTime() + " should wake at "+wakeTime);
		
		
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
		while (wakeTime > Machine.timer().getTime())
			KThread.yield();
		
		
	}
	
	class waiter implements Comparable{
		long wakeTime;
		private KThread thread;
		waiter(long wakeTime, KThread thread){
			this.wakeTime = wakeTime;
			this.thread = thread;
		}
		@Override
		public int compareTo(Object o) {
			waiter curr = (waiter) o;
			if(wakeTime < curr.wakeTime)
				return -1;
			else if(wakeTime < curr.wakeTime)
				return 1;
			else 
				return thread.compareTo(curr.thread);
		}
	}
	
	//self test
	static void selfTest() {
		
		class alarmTest implements Runnable{
			String label;
			Alarm a=new Alarm();
			long time;
			alarmTest(String s,long time){
				label=s;
				this.time=time;
			}
			@Override
			public void run() {
				System.out.println(label);
				a.waitUntil(time);
				System.out.println(time+" ms passed");
			}
			
		}

		KThread Athread = new KThread(new alarmTest("A",3000)).setName("A");
		KThread Bthread = new KThread(new alarmTest("B",1000)).setName("B");
		
		System.out.println("beginning: Alarm Test");
		Athread.fork();
		Bthread.fork();
		Athread.join();
		Bthread.join();
	}
	
	
//	private ThreadQueue waitlist  = ThreadedKernel.scheduler.newThreadQueue(true); 
//	private LinkedList<waiter> waitlist= new LinkedList<>();
	private static final char dbgInt = 'i';
	private TreeSet<waiter> waiting;
}
