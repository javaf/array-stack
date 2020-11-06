import java.nio.*;
import java.util.concurrent.locks.*;

// Array stack is a bounded lock-based stack using an
// array. It uses a common lock for both push and pop
// operations.

class ArrayStack<T> {
  Lock lock;
  T[] data;
  int top;
  // lock: common lock for push, pop
  // data: array of values in stack
  // top: top of stack (0 if empty)

  @SuppressWarnings("unchecked")
  public ArrayStack(int capacity) {
    lock = new ReentrantLock();
    data = (T[]) new Object[capacity];
    top = 0;
  }

  // 1. Create node for value.
  // 2. Try pushing node to stack.
  // 2a. If successful, return.
  // 2b. Otherwise, backoff and try again.
  public void push(T x) throws BufferOverflowException {
    try {
    lock.lock();
    if (top == data.length-1)
      throw new BufferOverflowException();
    data[top] = x;
    top++;
    } finally {
      lock.unlock();
    }
  }

  // 1. Try popping a node from stack.
  // 1a. If successful, return its value.
  // 1b. Otherwise, backoff and try again.
  public T pop() throws BufferUnderflowException {
    try {
    lock.lock();
    if (top == 0)
      throw new BufferUnderflowException();
    top--;
    return data[top];
    } finally {
      lock.unlock();
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
