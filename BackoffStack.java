import java.util.*;
import java.util.concurrent.atomic.*;

// Backoff stack is an unbounded lock-free LIFO linked
// list, where pushes and pops synchronize at a single
// location. It is compare-and-set (CAS) atomic operation
// to provide concurrent access with obstruction freedom.

class BackoffStack<T> {
  AtomicReference<Node<T>> top;
  static final long MIN_WAIT = 1;
  static final long MAX_WAIT = 100;
  // top: top of stack (null if empty)
  // MIN_WAIT: initial backoff wait range
  // MAX_WAIT: ultimate backoff wait range

  public BackoffStack() {
    top = new AtomicReference<>(null);
  }

  // 1. Create node for value.
  // 2. Try pushing node to stack.
  // 2a. If successful, return.
  // 2b. Otherwise, backoff and try again.
  public void push(T x) {
    long W = MIN_WAIT;
    Node<T> n = new Node<>(x); // 1
    while (true) {
      if (tryPush(n)) return; // 2, 2a
      else W = backoff(W);    // 2b
    }
  }

  // 1. Try popping a node from stack.
  // 1a. If successful, return its value.
  // 1b. Otherwise, backoff and try again.
  public T pop() throws EmptyStackException {
    long W = MIN_WAIT;
    while (true) {
      Node<T> n = tryPop(); // 1
      if (n != null) return n.value; // 1a
      else W = backoff(W);           // 1b
    }
  }

  // 1. Get stack top.
  // 2. Set node's next to top.
  // 3. Try push node at top (CAS).
  protected boolean tryPush(Node<T> n) {
    Node<T> m = top.get(); // 1
    n.next = m;                     // 2
    return top.compareAndSet(m, n); // 3
  }

  // 1. Get stack top, and ensure stack not empty.
  // 2. Try pop node at top, and set top to next (CAS).
  protected Node<T> tryPop() throws EmptyStackException {
    Node<T> m = top.get();                          // 1
    if (m == null) throw new EmptyStackException(); // 1
    Node<T> n = m.next;                       // 2
    return top.compareAndSet(m, n)? m : null; // 2
  }

  // 1. Get a random wait duration.
  // 2. Sleep for the duration.
  // 3. Double the max random wait duration.
  private long backoff(long W) {
    long w = (long) (Math.random() * // 1
      (W-MIN_WAIT) + MIN_WAIT);      // 1
    try { Thread.sleep(w); }         // 2
    catch(InterruptedException e) {} // 2
    return Math.min(2*W, MAX_WAIT);  // 3
  }
}
