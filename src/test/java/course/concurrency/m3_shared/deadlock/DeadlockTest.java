package course.concurrency.m3_shared.deadlock;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DeadlockTest {
    @Test
    void emulateDeadlock() throws InterruptedException {
        Map<String, String> map = new ConcurrentHashMap<>();

        BiFunction<String, String, String> remappingFunction1 = (key, value) -> {
            System.out.println(Thread.currentThread().getName() + " inside compute");

            try {
                Thread.sleep(2L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String newValue = map.compute("key2", (key2, value2) -> value + value2);
            System.out.println("Never happen");
            return newValue;
        };

        BiFunction<String, String, String> remappingFunction2 = (key, value) -> {
            System.out.println(Thread.currentThread().getName() + " inside compute");

            try {
                Thread.sleep(2L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            String newValue = map.compute("key1", (key1, value1) -> value + value1);
            System.out.println("Never happen");
            return newValue;
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> map.compute("key1", remappingFunction1));
        executor.submit(() -> map.compute("key2", remappingFunction2));

        assertFalse(executor.awaitTermination(3L, TimeUnit.SECONDS));
    }
}
