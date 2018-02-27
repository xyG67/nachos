package nachos.threads;

import java.util.Random;

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
	private int speakerNum;
	private int listenerNum;
	private Integer word;
	private Condition2 speaker;
	private Condition2 listener;
	private Condition2 transc;					//add on 02/25
	private boolean isReady;

	public Communicator() {
		this.lock = new Lock();
		this.speakerNum = 0;
		this.listenerNum = 0;
		this.word = null;
		this.speaker = new Condition2(lock);
		this.listener = new Condition2(lock);
		this.transc = new Condition2(lock);		//add on 02/25
		this.isReady = false;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 * 
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 * 
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int message) {
		// project 1.4
		//acquire lock and increase the speaker number
		lock.acquire();
		speakerNum++;
		//speaker sleep while there is word ready or no listener is active
		while(word != null) {				//add on 02/25
			speaker.sleep();
		}
		//say the word and indicate word is ready
		this.word = message;
		isReady = true;
		//wake the listener
		listener.wake();
		transc.sleep();						//add on 02/25
		//decrease the speaker number and release the lock
		speakerNum--;
		lock.release();
		//return;
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return the
	 * <i>word</i> that thread passed to <tt>speak()</tt>.
	 * 
	 * @return the integer transferred.
	 */
	public int listen() {
		//acquire lock and increase the listener number
		lock.acquire();
		listenerNum++;
		//listener sleep while there is no ready word, and try to wake up all the speaker
		while(word == null) {				//add on 02/25
			listener.sleep();
		}
		//listen to the word and set the ready word is used now
		int recWord = word;
		isReady = false;
		word = null;						//add on 02/25
		transc.wake();						//add on 02/25
		speaker.wake();						//add on 02/25
		//decrease the listener number and release the lock
		listenerNum--;
		lock.release();
		//return the received word
		return recWord;
	}
}
