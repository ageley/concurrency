package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {
    private final ExecutorService executor;

    public Notifier() {
        executor = Executors.newWorkStealingPool();
    }

    public void sendOutdatedMessage(Bid bid) {
        if (bid.getId() != Bid.DEFAULT_VALUE) {
            executor.submit(this::imitateSending);
        }
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
