package course.concurrency.exams.auction;

import java.util.concurrent.CompletableFuture;

public class Notifier {

    public void sendOutdatedMessage(Bid bid) {
        if (bid.getId() != Bid.DEFAULT_VALUE) {
            CompletableFuture.runAsync(this::imitateSending);
        }
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
    }

    public void shutdown() {
    }
}
