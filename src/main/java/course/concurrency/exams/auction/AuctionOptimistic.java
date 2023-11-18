package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;
    private final AtomicReference<Bid> latestBid;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid = new AtomicReference<>();
    }

    public boolean propose(Bid bid) {
        Bid tempLatestBid;

        do {
            tempLatestBid = latestBid.get();

            if (tempLatestBid != null) {
                if (bid.getPrice() <= tempLatestBid.getPrice()) {
                    return false;
                }

                notifier.sendOutdatedMessage(tempLatestBid);
            }

        } while (!latestBid.compareAndSet(tempLatestBid, bid));

        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
