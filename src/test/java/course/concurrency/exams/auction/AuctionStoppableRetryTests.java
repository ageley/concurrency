package course.concurrency.exams.auction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionStoppableRetryTests {

    @Suite
    @IncludeTags("pessimistic_stoppable")
    @SelectPackages("course.concurrency.exams.auction")
    public static class PessimisticSuite {
    }

    @Suite
    @IncludeTags("optimistic_stoppable")
    @SelectPackages("course.concurrency.exams.auction")
    public static class OptimisticSuite {
    }

    private Notifier notifier;

    private AuctionStoppable pessimistic;
    private AuctionStoppable optimistic;

    // for stopAuction test
    private Supplier<AuctionStoppable> pessimisticSupplier;
    private Supplier<AuctionStoppable> optimisticSupplier;

    @BeforeEach
    public void setup() {
        notifier = new Notifier();

        pessimisticSupplier = () -> new AuctionStoppablePessimistic(notifier);
        pessimistic = pessimisticSupplier.get();

        optimisticSupplier = () -> new AuctionStoppableOptimistic(notifier);
        optimistic = optimisticSupplier.get();
    }

    @AfterEach
    public void tearDown() {
        notifier.shutdown();
    }

    @Test
    @DisplayName("Pessimistic: stopAuction works with data races")
    @Tag("pessimistic_stoppable")
    @Timeout(60)
    public void stopWithRetryPessimistic() throws InterruptedException {
        stopAuctionWithRetry(() -> pessimistic);
    }

    @Test
    @DisplayName("Optimistic: stopAuction works with data races")
    @Tag("optimistic_stoppable")
    @Timeout(60)
    public void stopWithRetryOptimistic() throws InterruptedException {
        stopAuctionWithRetry(() -> optimistic);
    }

    public void stopAuctionWithRetry(Supplier<AuctionStoppable> auctionSuppler) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(8);

        final AtomicReference<Bid> stoppedBid = new AtomicReference<>();
        Bid firstBid = new Bid(1L, 1L, 1L);

        Bid slow = new Bid(2l, 2l, 2l) {
            public Long getPrice() {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return super.getPrice();
            }
        };

        for (int i = 2; i < 100; i++) {
            AuctionStoppable auction = auctionSuppler.get();
            auction.propose(firstBid);
            stoppedBid.set(null);
            Bid raceBid = slow;

            CountDownLatch startAuctionTasksLatch = new CountDownLatch(1);
            CountDownLatch auctionTasksDoneLatch = new CountDownLatch(8);

            Runnable doPropose = () -> {
                try {
                    startAuctionTasksLatch.await();
                } catch (InterruptedException ignored) {
                }
                auction.propose(raceBid);
                auctionTasksDoneLatch.countDown();
            };
            Runnable doStopAuction = () -> {
                try {
                    startAuctionTasksLatch.await();
                } catch (InterruptedException ignored) {
                }
                Bid stopped = auction.stopAuction();
                stoppedBid.set(stopped);
                auctionTasksDoneLatch.countDown();
            };

            for (int j = 0; j < 4; j++) {
                executor.submit(doPropose);
                executor.submit(doStopAuction);
            }

            startAuctionTasksLatch.countDown();
            auctionTasksDoneLatch.await();

            Long latestPrice = auction.getLatestBid().getPrice();
            assertEquals(stoppedBid.get().getPrice(), latestPrice, "Bid was updated after stop");
        }

        executor.shutdownNow();
    }
}
