package course.concurrency.m5_streams.queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class CustomBlockingQueue<T> {
    private static class Node<T> {
        private final T value;
        private Node<T> next;

        private Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    private final int capacity;
    private final AtomicInteger size;
    private final ReentrantLock lock;
    private final Condition notFull;
    private final Condition notEmpty;

    public CustomBlockingQueue(int capacity) {
        this.capacity = capacity;
        size = new AtomicInteger();
        lock = new ReentrantLock();
        notFull = lock.newCondition();
        notEmpty = lock.newCondition();
    }

    public int size() {
        return size.get();
    }

    private volatile Node<T> head;
    private volatile Node<T> tail;

    public void enqueue(T value) throws InterruptedException {
        int prevSize = -1;
        Node<T> node = new Node<>(value, null);

        try {
            lock.lockInterruptibly();

            while (size.get() == capacity) {
                notFull.await();
            }

            if (head != null) {
                head.next = node;
            }

            head = node;

            if (tail == null) {
                tail = head;
            }

            prevSize = size.getAndIncrement();
        } finally {
            if (prevSize == 0) {
                notEmpty.signal();
            }

            lock.unlock();
        }
    }

    public T dequeue() throws InterruptedException {
        int prevSize = -1;

        try {
            lock.lockInterruptibly();

            while (tail == null) {
                notEmpty.await();
            }

            Node<T> node = tail;
            tail = tail.next;
            node.next = null;

            if (tail == null) {
                head = null;
            }

            prevSize = size.getAndDecrement();
            return node.value;
        } finally {
            if (prevSize == capacity) {
                notFull.signalAll();
            }

            lock.unlock();
        }
    }
}
