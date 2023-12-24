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

    public void enqueue(T value) {
        Node<T> node = new Node<>(value, null);

        try {
            lock.lock();

            while (size.get() == capacity) {
                notFull.awaitUninterruptibly();
            }

            if (head != null) {
                head.next = node;
            }

            head = node;

            if (tail == null) {
                tail = head;
            }

            if (size.getAndIncrement() == 0) {
                notEmpty.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public T dequeue() {
        try {
            lock.lock();

            while (size.get() == 0) {
                notEmpty.awaitUninterruptibly();
            }

            Node<T> node = tail;
            tail = tail.next;
            node.next = null;

            if (tail == null) {
                head = null;
            }

            if (size.getAndDecrement() == capacity) {
                notFull.signalAll();
            }

            return node.value;
        } finally {
            lock.unlock();
        }
    }
}
