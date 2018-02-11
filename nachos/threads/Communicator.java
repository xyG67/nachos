package nachos.threads;

import java.util.LinkedList;
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
	private Lock lock = new Lock();
	private int speakerNum = 0;// original "static" is deleted as communicator
								// should be different objects
	private int listenerNum = 0;// same as above
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
	 * @param word
	 *            the integer to transfer.
	 */
	public void speak(int word) {
		// project 1.4
		boolean intStatus = Machine.interrupt().disable();
		lock.acquire();
		if (listenerNum == 0) {
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
		if (speakerNum != 0) {
			speaker.wake();
			listener.sleep();
		} else {
			listenerNum++;
			listener.sleep();
			listenerNum--;
		}
		lock.release();
		Machine.interrupt().restore(intStatus);
		System.out.println(KThread.currentThread().getName() + " get message " + speakerQueue.peek());
		return speakerQueue.poll();
	}

	// self test
	public static void selfTest() {

		class listenerThread implements Runnable {
			Communicator c;

			public listenerThread(Communicator c) {
				this.c = c;
			}

			@Override
			public void run() {
				System.out.println(KThread.currentThread().getName() + " is listening");
				c.listen();
			}

		}
		class speakerThread implements Runnable {
			Communicator c;
			int word;

			public speakerThread(Communicator c, int word) {
				this.c = c;
				this.word = word;
			}

			@Override
			public void run() {
				System.out.println(KThread.currentThread().getName() + " said " + word);
				c.speak(word);
			}
		}

		System.out.println("\n Begin Communicator Test");

		// VAR1: Test for one speaker, one listener, speaker waits for listener
		System.out.println("\n Test1");
		Communicator c1 = new Communicator();
		Alarm a1 = new Alarm();
		KThread s1 = new KThread(new speakerThread(c1, 1)).setName("s1");
		s1.fork();
		a1.waitUntil(5000);
		KThread l1 = new KThread(new listenerThread(c1)).setName("l1");
		l1.fork();
		l1.join();

		// VAR2: Test for one speaker, one listener, listener waits for speaker
		System.out.println("\n Test2");
		Communicator c2 = new Communicator();
		KThread l2 = new KThread(new listenerThread(c2)).setName("l2");
		l2.fork();
		a1.waitUntil(5000);
		KThread s2 = new KThread(new speakerThread(c2, 2)).setName("s2");
		s2.fork();
		s2.join();

		// VAR3: Test for one speaker, more listeners, listener waits for
		// speaker
		System.out.println("\n Test3");
		Communicator c3 = new Communicator();
		KThread[] l3 = new KThread[5];
		for (int i = 0; i < 5; i++) {
			l3[i] = new KThread(new listenerThread(c3)).setName("l3-" + i);
			l3[i].fork();
		}
		a1.waitUntil(5000);
		KThread s3 = new KThread(new speakerThread(c3, 3)).setName("s3");
		s3.fork();
		s3.join();

		// VAR4: Test for one speaker, more listeners, speaker waits for
		// listener
		System.out.println("\n Test4");
		Communicator c4 = new Communicator();
		KThread s4 = new KThread(new speakerThread(c4, 4)).setName("s4");
		s4.fork();
		a1.waitUntil(5000);
		KThread[] l4 = new KThread[5];
		for (int i = 0; i < 5; i++) {
			l4[i] = new KThread(new listenerThread(c4)).setName("l4-" + i);
			l4[i].fork();
		}
		a1.waitUntil(5000);

		// VAR5: Test for one speaker, more listeners, listeners waits for
		// speaker, and then create more listeners
		System.out.println("\n Test5");
		Communicator c5 = new Communicator();
		KThread[] l5 = new KThread[10];
		for (int i = 0; i < 5; i++) {
			l5[i] = new KThread(new listenerThread(c5)).setName("l5-" + i);
			l5[i].fork();
		}

		KThread s5 = new KThread(new speakerThread(c5, 5)).setName("s5");
		s5.fork();
		// s5.join();
		for (int i = 5; i < 10; i++) {
			l5[i] = new KThread(new listenerThread(c5)).setName("l5-" + i);
			l5[i].fork();
		}
		a1.waitUntil(5000);

		// VAR6: Test for more speakers, one listener, listener waits for
		// speaker
		System.out.println("\n Test6");
		Communicator c6 = new Communicator();
		KThread l6 = new KThread(new listenerThread(c6)).setName("l6");
		l6.fork();
		a1.waitUntil(5000);
		// KThread[] s6 = new KThread[5];
		for (int i = 0; i < 5; i++) {
			new KThread(new speakerThread(c6, 6 * 10 + i)).setName("s6-" + i).fork();
		}
		a1.waitUntil(5000);

		// VAR7: Test for more speakers, one listener, speaker waits for
		// listener
		System.out.println("\n Test7");
		Communicator c7 = new Communicator();
		for (int i = 0; i < 5; i++) {
			new KThread(new speakerThread(c7, 7 * 10 + i)).setName("s7-" + i).fork();
		}
		a1.waitUntil(5000);
		new KThread(new listenerThread(c7)).setName("l7").fork();
		a1.waitUntil(5000);

		// VAR8: Test for one listener, more speakers, speakers wait for
		// listener, and then create more speakers
		System.out.println("\n Test8");
		Communicator c8 = new Communicator();
		for (int i = 0; i < 5; i++) {
			new KThread(new speakerThread(c8, 8 * 10 + i)).setName("s8-" + i).fork();
		}

		new KThread(new listenerThread(c8)).setName("l8").fork();

		for (int i = 5; i < 10; i++) {
			new KThread(new speakerThread(c8, 8 * 10 + i)).setName("s8-" + i).fork();
		}
		a1.waitUntil(5000);

		// VAR9: Test for more speakers, more listeners, listeners waits for
		// speaker
		System.out.println("\n Test9");
		Communicator c9 = new Communicator();
		for (int i = 0; i < 5; i++) {
			new KThread(new listenerThread(c9)).setName("l9-" + i).fork();
		}
		a1.waitUntil(5000);
		for (int i = 0; i < 5; i++) {
			new KThread(new speakerThread(c9, 9 * 10 + i)).setName("s9-" + i).fork();
		}
		a1.waitUntil(5000);

		// VAR10: Test for more speakers, more listeners, listeners waits for
		// speaker
		System.out.println("\n Test10");
		Communicator c10 = new Communicator();
		for (int i = 0; i < 50; i++) {
			new KThread(new listenerThread(c10)).setName("l10-" + i).fork();
		}
		a1.waitUntil(5000);
		for (int i = 0; i < 50; i++) {
			new KThread(new speakerThread(c10, 10 * 10 + i)).setName("s10-" + i).fork();
		}
		a1.waitUntil(5000);

		// VAR11: Test for more speakers, more listeners, speakers and listeners
		// have the same number but created with random order.
		Communicator c11=new Communicator();
		int N=100;
		boolean[] flip=new boolean[N];
		for(int i=N/2;i<N;i++){
			flip[i]=true;
		}
		
		//random shullfle
		Random rand=new Random();
		for (int i = flip.length - 1; i > 0; i--)
	    {
	      int index = rand.nextInt(i + 1);
	      // Simple swap
	      boolean a = flip[index];
	      flip[index] = flip[i];
	      flip[i] = a;
	    }
		int idx1=0;
		int idx2=0;
		for(int i=0;i<N;i++){
			if(flip[i]){
				new KThread(new speakerThread(c11, 1000 * 10 + idx1)).setName("s20-" + idx1++).fork();
			}else{
				new KThread(new listenerThread(c11)).setName("l20-" + idx2++).fork();
			}
				
		}
		a1.waitUntil(20000);
		
		// VAR12: Run above test cases in batch for more than two hours, make
		// sure no exception occurs.
	}

}
