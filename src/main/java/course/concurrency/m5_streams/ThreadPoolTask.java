package course.concurrency.m5_streams;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;

public class ThreadPoolTask {
    private static final long DEFAULT_KEEP_ALIVE_SECONDS = 60L;
    private static final int DEFAULT_CAPACITY = Runtime.getRuntime().availableProcessors();

    // Task #1
    public ThreadPoolExecutor getLifoExecutor() {
        return new ThreadPoolExecutor(0, DEFAULT_CAPACITY, DEFAULT_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>() {
                    @Override
                    public boolean add(Runnable e) {
                        addFirst(e);
                        return true;
                    }

                    @Override
                    public boolean offer(Runnable e) {
                        return offerFirst(e);
                    }

                    @Override
                    public void put(Runnable e) throws InterruptedException {
                        putFirst(e);
                    }

                    @Override
                    public boolean offer(Runnable e, long timeout, TimeUnit unit) throws InterruptedException {
                        return offerFirst(e, timeout, unit);
                    }
                }
        );
    }

    // Task #2
    public ThreadPoolExecutor getRejectExecutor() {
        return new ThreadPoolExecutor(0, DEFAULT_CAPACITY, DEFAULT_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new SynchronousQueue<>(), new DiscardPolicy()
        );
    }
}
