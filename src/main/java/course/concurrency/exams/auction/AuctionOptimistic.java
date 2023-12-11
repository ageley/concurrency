package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

import static course.concurrency.exams.auction.Bid.DEFAULT_VALUE;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;
    private final AtomicReference<Bid> latestBid;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid = new AtomicReference<>(new Bid(DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE));
    }

    public boolean propose(Bid bid) {
        Bid tempLatestBid;

        do {
            tempLatestBid = latestBid.get();

            if (bid.getPrice() <= tempLatestBid.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(tempLatestBid, bid));

        notifier.sendOutdatedMessage(tempLatestBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
