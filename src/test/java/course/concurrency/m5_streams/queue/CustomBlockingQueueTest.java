package course.concurrency.m5_streams.queue;

import org.junit.jupiter.api.Test;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomBlockingQueueTest {
    private static final List<String> EXPECTED_VALUES = List.of("a", "b", "c", "d");
    private static final int NUMBER_OF_WRITES = 100000;
    private static final int NUMBER_OF_LOOPS = 4;
    private static final int SIZE_OF_STARVING_QUEUE = NUMBER_OF_WRITES * EXPECTED_VALUES.size();
    private static final int SIZE_OF_RESULTS = SIZE_OF_STARVING_QUEUE * NUMBER_OF_LOOPS;

    @Test
    void singleThreadEnqueueDequeue() {
        //given
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(1);

        //when
        queue.enqueue("a");

        //then
        assertEquals(1, queue.size());
        assertEquals("a", queue.dequeue());
        assertEquals(0, queue.size());
    }

    @Test
    void capacityExceeded() throws InterruptedException {
        //given
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(1);

        //when
        queue.enqueue("a");
        Thread thread = new Thread(() -> queue.enqueue("b"));
        thread.start();

        //then
        assertEquals(State.RUNNABLE, thread.getState());
        assertEquals(1, queue.size());
        assertEquals("a", queue.dequeue());

        //when
        thread.join();

        assertEquals(1, queue.size());
        assertEquals("b", queue.dequeue());
        assertEquals(0, queue.size());
        assertEquals(State.TERMINATED, thread.getState());
    }

    @Test
    void queueIsEmpty() throws InterruptedException {
        //given
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(1);

        //when
        queue.enqueue("a");
        queue.dequeue();
        Thread thread = new Thread(queue::dequeue);
        thread.start();

        //then
        assertEquals(State.RUNNABLE, thread.getState());
        assertEquals(0, queue.size());

        //when
        queue.enqueue("b");
        thread.join();

        //then
        assertEquals(0, queue.size());
        assertEquals(State.TERMINATED, thread.getState());
    }

    @Test
    void multiThreadEnqueueDequeue() throws ExecutionException, InterruptedException {
        //given
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(SIZE_OF_STARVING_QUEUE);
        List<String> factualValues = Collections.synchronizedList(new ArrayList<>());

        Runnable doEnqueue = () -> {
            for (int i = 0; i < NUMBER_OF_WRITES; i++) {
                for (String value : EXPECTED_VALUES) {
                    queue.enqueue(value);
                }
            }
        };

        Runnable doDequeue = () -> {
            while (factualValues.size() < SIZE_OF_RESULTS) {
                factualValues.add(queue.dequeue());
            }
        };

        List<CompletableFuture<Void>> writers = IntStream.range(0, NUMBER_OF_LOOPS)
                .mapToObj(i -> CompletableFuture.runAsync(doEnqueue))
                .collect(Collectors.toList());

        List<CompletableFuture<Void>> readers = IntStream.range(0, NUMBER_OF_LOOPS)
                .mapToObj(i -> CompletableFuture.runAsync(doDequeue))
                .collect(Collectors.toList());

        CompletableFuture<Void> allWriters = CompletableFuture.allOf(writers.toArray(CompletableFuture[]::new));
        CompletableFuture<Object> allReaders = CompletableFuture.anyOf(readers.toArray(CompletableFuture[]::new));
        CompletableFuture<Void> allWorkers = CompletableFuture.allOf(allWriters, allReaders);

        //when
        allWorkers.get();

        //then
        assertEquals(0, queue.size());
    }
}