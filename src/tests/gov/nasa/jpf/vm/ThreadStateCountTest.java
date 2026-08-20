package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

public class ThreadStateCountTest extends TestJPF {
  
  static class BlockingThread extends Thread {
    private Object lock;
    public BlockingThread(Object lock) { this.lock = lock; }
    @Override
    public void run() {
      synchronized(lock) {
        // Just blocking and then finishing
      }
    }
  }

  static class WaitingThread extends Thread {
    private Object lock;
    public WaitingThread(Object lock) { this.lock = lock; }
    @Override
    public void run() {
      synchronized(lock) {
        try {
          lock.wait();
        } catch (InterruptedException e) {}
      }
    }
  }

  @Test
  public void testBlockedCount() {
    if (verifyNoPropertyViolation()) {
      Object lock = new Object();
      BlockingThread t1 = new BlockingThread(lock);
      
      synchronized(lock) {
        t1.start();
        Thread.yield(); // Let t1 try to get the lock and block
      }
      
      try { t1.join(); } catch (InterruptedException e) {}
      
      int blockedCount = Verify.getThreadBlockedCount(t1);
      assertTrue("Expected 1 blocked transition, got " + blockedCount, blockedCount == 1);
    }
  }

  @Test
  public void testWaitedCount() {
    if (verifyNoPropertyViolation()) {
      Object lock = new Object();
      WaitingThread t1 = new WaitingThread(lock);
      
      t1.start();
      Thread.yield(); // Let t1 enter synchronized and call wait
      
      synchronized(lock) {
        lock.notify();
      }
      
      try { t1.join(); } catch (InterruptedException e) {}
      
      int waitedCount = Verify.getThreadWaitedCount(t1);
      assertTrue("Expected 1 waited transition, got " + waitedCount, waitedCount == 1);
    }
  }
}
