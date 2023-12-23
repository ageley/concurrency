package course.concurrency.m5_streams.queue;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomBlockingQueueTest {
    private static final List<String> EXPECTED_VALUES = List.of("a", "b", "c", "d");
    private static final int NUMBER_OF_WRITES = 100000;
    private static final int NUMBER_OF_LOOPS = 4;
    private static final int SIZE_OF_QUEUE = NUMBER_OF_WRITES * EXPECTED_VALUES.size() * NUMBER_OF_LOOPS;

    @Test
    void singleThreadEnqueueDequeue() {
        //given
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(EXPECTED_VALUES.size());
        List<String> factualValues = new ArrayList<>();

        //when
        queue.enqueue(EXPECTED_VALUES.get(0));
        queue.enqueue(EXPECTED_VALUES.get(1));
        factualValues.add(queue.dequeue());
        factualValues.add(queue.dequeue());
        queue.enqueue(EXPECTED_VALUES.get(2));
        queue.enqueue(EXPECTED_VALUES.get(3));
        factualValues.add(queue.dequeue());
        factualValues.add(queue.dequeue());

        //then
        assertEquals(0, queue.size());
        assertTrue(factualValues.containsAll(EXPECTED_VALUES));
    }

    @Test
    void capacityExceeded() {
        //given
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(1);

        //when
        queue.enqueue("a");

        //then
        assertEquals(1, queue.size());
        assertThrows(IndexOutOfBoundsException.class, () -> queue.enqueue("b"));
    }

    @Test
    void queueIsEmpty() {
        //given
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(1);

        //when
        queue.enqueue("a");
        queue.dequeue();

        //then
        assertEquals(0, queue.size());
        assertNull(queue.dequeue());
    }

    @Test
    void multiThreadEnqueueDequeue() {
        //given
        CustomBlockingQueue<String> queue =
                new CustomBlockingQueue<>(SIZE_OF_QUEUE);
        List<String> factualValues = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);

        Runnable doEnqueue = () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
            }

            for (int i = 0; i < NUMBER_OF_WRITES; i++) {
                EXPECTED_VALUES.forEach(queue::enqueue);
            }
        };

        Runnable doDequeue = () -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
            }

            while (factualValues.size() != SIZE_OF_QUEUE) {
                String value = queue.dequeue();

                if (value != null) {
                    factualValues.add(value);
                }
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_LOOPS * 2);

        List<CompletableFuture<Void>> writers = IntStream.range(0, NUMBER_OF_LOOPS)
                .mapToObj(i -> CompletableFuture.runAsync(doEnqueue, executor))
                .collect(Collectors.toList());

        List<CompletableFuture<Void>> readers = IntStream.range(0, NUMBER_OF_LOOPS)
                .mapToObj(i -> CompletableFuture.runAsync(doDequeue, executor))
                .collect(Collectors.toList());

        //when
        latch.countDown();
        writers.forEach(CompletableFuture::join);
        readers.forEach(CompletableFuture::join);

        //then
        assertEquals(SIZE_OF_QUEUE, factualValues.size());
    }
}