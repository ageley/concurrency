package course.concurrency.m5_streams.queue;

import java.util.concurrent.atomic.AtomicInteger;
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

    public CustomBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.size = new AtomicInteger();
        this.lock = new ReentrantLock();
    }

    public int size() {
        return size.get();
    }

    private Node<T> head;
    private Node<T> tail;

    public void enqueue(T value) {
        if (size.get() == capacity) {
            throw new IndexOutOfBoundsException("Queue reached capacity = " + capacity);
        }

        Node<T> node = new Node<>(value, null);

        try {
            lock.lock();

            if (head != null) {
                head.next = node;
            }

            head = node;

            if (tail == null) {
                tail = head;
            }

            size.incrementAndGet();
        } finally {
            lock.unlock();
        }
    }

    public T dequeue() {
        if (tail == null) {
            return null;
        }

        try {
            lock.lock();

            if (tail == null) {
                return null;
            }

            Node<T> node = tail;
            tail = tail.next;
            node.next = null;

            if (tail == null) {
                head = null;
            }

            size.decrementAndGet();
            return node.value;
        } finally {
            lock.unlock();
        }
    }
}
