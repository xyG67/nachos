package nachos.threads;

import java.util.LinkedList;

import nachos.machine.Machine;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	private Lock lock;
	private static int speakerNum = 0;
	private static int listenerNum = 0;
	private int word = 0;
	LinkedList<Integer> speakerQueue = new LinkedList<Integer>();
	Condition2 speaker = new Condition2(lock);
	Condition2 listener = new Condition2(lock);
	
	public Communicator() {
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word the integer to transfer.
	 */
	public void speak(int word) {
		//project 1.4
		boolean intStatus = Machine.interrupt().disable();
		lock.acquire();
		if(listenerNum == 0) {
			speakerNum++;
			speakerQueue.offer(word);
			speaker.sleep();
			listener.wake();
			speakerNum--;
		} else {
			speakerQueue.offer(word);
			listener.wake();
		}
		lock.release();		
		Machine.interrupt().restore(intStatus);
		return;
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		boolean intStatus = Machine.interrupt().disable();
		lock.acquire();
		if(speakerNum != 0) {
			speaker.wake();
			listener.sleep();
		}else {
			listenerNum++;
			listener.sleep();
			listenerNum--;
		}
		lock.release();		
		Machine.interrupt().restore(intStatus);
		return speakerQueue.poll();
	}
}
