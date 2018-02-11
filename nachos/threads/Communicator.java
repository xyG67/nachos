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
	private Lock lock=new Lock();
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
		System.out.println(KThread.currentThread().getName()+" get message");
		return speakerQueue.poll();
	}
	
	//self test
	public static void selfTest() {
		
		class listenerThread implements Runnable{
			Communicator c;
			public listenerThread(Communicator c){
				this.c=c;
			}
			@Override
			public void run() {
				System.out.println(KThread.currentThread().getName()+" is listening");
				c.listen();
			}
			
		}
		class speakerThread implements Runnable{
			Communicator c;
			int word;
			public speakerThread(Communicator c, int word){
				this.c=c;
				this.word=word;
			}
			@Override
			public void run() {
				System.out.println(KThread.currentThread().getName()+" said "+word);
				c.speak(word);
			}
		}
		
		//VAR1: Test for one speaker, one listener, speaker waits for listener
		Communicator c1=new Communicator();
		Alarm a1=new Alarm();
		KThread s1=new KThread(new speakerThread(c1,1)).setName("s1");
		s1.fork();
		a1.waitUntil(5000);
		KThread l1=new KThread(new listenerThread(c1)).setName("l1");
		l1.fork();
		l1.join();
		//VAR2: Test for one speaker, one listener, listener waits for speaker
		Communicator c2=new Communicator();
		KThread l2=new KThread(new listenerThread(c2)).setName("l2");
		l2.fork();
		a1.waitUntil(5000);
		KThread s2=new KThread(new speakerThread(c2,2)).setName("s2");
		s2.fork();
		s2.join();
	}
		
}
