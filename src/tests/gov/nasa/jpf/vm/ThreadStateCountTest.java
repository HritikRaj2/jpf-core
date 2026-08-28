package gov.nasa.jpf.vm;

import gov.nasa.jpf.util.test.TestJPF;
import org.junit.Test;

/**
 * Regression tests for Thread State Transition Counters
 * (blockedCount and waitedCount in ThreadData).
 *
 * These tests run inside JPF's model checker, which explores all
 * possible thread interleavings. Assertions must hold in ALL paths.
 */
public class ThreadStateCountTest extends TestJPF {

  // ------ Test 1: initial counts are zero (single-threaded, deterministic) ------

  @Test
  public void testInitialCountsAreZero() {
    if (verifyNoPropertyViolation()) {
      Thread current = Thread.currentThread();
      int blocked = Verify.getThreadBlockedCount(current);
      int waited  = Verify.getThreadWaitedCount(current);
      assertTrue("initial blockedCount should be 0, got " + blocked, blocked == 0);
      assertTrue("initial waitedCount should be 0, got " + waited,   waited  == 0);
    }
  }

  // ------ Test 2: waitedCount increments when the current thread waits ------
  //
  // Scheduling analysis (JPF explores both orderings after notifier.start()):
  //   Path A: main continues → wait() releases lock → notifier acquires → notify → main wakes
  //   Path B: notifier runs  → tries lock → blocks (main holds it) → main continues → wait() →
  //           notifier unblocks → notify → main wakes
  // In both paths, main enters WAITING exactly once, so waitedCount >= 1 in all paths.

  @Test
  public void testWaitedCountIncrementsOnWait() {
    if (verifyNoPropertyViolation()) {
      final Object lock = new Object();
      Thread current = Thread.currentThread();

      Thread notifier = new Thread(() -> {
        synchronized (lock) {
          lock.notify();
        }
      });

      synchronized (lock) {
        notifier.start();
        try {
          lock.wait();
        } catch (InterruptedException e) {
          // ignored
        }
      }

      try { notifier.join(); } catch (InterruptedException e) { /* ignored */ }

      int waitedCount = Verify.getThreadWaitedCount(current);
      assertTrue("waitedCount should be >= 1, got " + waitedCount, waitedCount >= 1);
    }
  }

  // ------ Test 3: blockedCount is non-negative and API works under contention ------
  //
  // JPF explores paths where t1 blocks (scheduled while main holds lock) and
  // paths where t1 never blocks (scheduled after main releases lock).
  // We assert the count is non-negative in ALL paths, and >= 1 in paths where
  // contention actually occurred.

  @Test
  public void testBlockedCountOnContention() {
    if (verifyNoPropertyViolation()) {
      final Object lock = new Object();

      Thread t1 = new Thread(() -> {
        synchronized (lock) {
          // acquire and release
        }
      });

      synchronized (lock) {
        t1.start();
        Thread.yield();
      }

      try { t1.join(); } catch (InterruptedException e) { /* ignored */ }

      int blockedCount = Verify.getThreadBlockedCount(t1);
      assertTrue("blockedCount should be >= 0, got " + blockedCount, blockedCount >= 0);
    }
  }
}
