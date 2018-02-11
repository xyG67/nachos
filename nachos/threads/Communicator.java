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
	private  int speakerNum = 0;//original "static" is deleted as communicator should be different objects
	private  int listenerNum = 0;// same as above
	private int word = 0;
	LinkedList<Integer> speakerQueue = new LinkedList<Integer>();
//	Condition2 speaker = new Condition2(lock);
//	Condition2 listener = new Condition2(lock);
	Condition speaker = new Condition(lock);
	Condition listener = new Condition(lock);
	
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
		System.out.println(KThread.currentThread().getName()+" get message "+speakerQueue.peek());
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
		System.out.println("\n Begin Communicator Test");
		System.out.println("\n Test1");
		//VAR1: Test for one speaker, one listener, speaker waits for listener
		Communicator c1=new Communicator();
		Alarm a1=new Alarm();
		KThread s1=new KThread(new speakerThread(c1,1)).setName("s1");
		s1.fork();
		a1.waitUntil(5000);
		KThread l1=new KThread(new listenerThread(c1)).setName("l1");
		l1.fork();
		l1.join();
		System.out.println("\n Test2");
		//VAR2: Test for one speaker, one listener, listener waits for speaker
		Communicator c2=new Communicator();
		KThread l2=new KThread(new listenerThread(c2)).setName("l2");
		l2.fork();
		a1.waitUntil(5000);
		KThread s2=new KThread(new speakerThread(c2,2)).setName("s2");
		s2.fork();
		s2.join();
		
		System.out.println("\n Test3");
//		VAR3: Test for one speaker, more listeners, listener waits for speaker
		Communicator c3=new Communicator();
		KThread[] l3=new KThread[5];
		for(int i=0;i<5;i++){
			l3[i]=new KThread(new listenerThread(c3)).setName("l3-"+i);
			l3[i].fork();
		}
		a1.waitUntil(5000);
		KThread s3=new KThread(new speakerThread(c3,3)).setName("s3");
		s3.fork();
		s3.join();
		
		System.out.println("\n Test4");
//		VAR4: Test for one speaker, more listeners, speaker waits for listener
		Communicator c4=new Communicator();
		KThread s4=new KThread(new speakerThread(c4,4)).setName("s4");
		s4.fork();
		a1.waitUntil(5000);
		KThread[] l4=new KThread[5];
		for(int i=0;i<5;i++){
			l4[i]=new KThread(new listenerThread(c4)).setName("l4-"+i);
			l4[i].fork();
		}
		a1.waitUntil(5000);
		
//		VAR5: Test for one speaker, more listeners, listeners waits for speaker, and then create more listeners
//		VAR6: Test for more speakers, one listener, listener waits for speaker
//		VAR7: Test for more speakers, one listener, speaker waits for listener
//		VAR8: Test for one listener, more speakers, speakers wait for listener, and then create more speakers
//		VAR9: Test for more speakers, more listeners, listeners waits for speaker
//		VAR10: Test for more speakers, more listeners, listeners waits for speaker
//		VAR11: Test for more speakers, more listeners, speakers and listeners have the same number but created with random order.
//		VAR12: Run above test cases in batch for more than two hours, make sure no exception occurs.
	}
		
}
