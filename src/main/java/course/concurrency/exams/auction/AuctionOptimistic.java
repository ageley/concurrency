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
        Bid tempLatestBid = latestBid.get();

        if (tempLatestBid == null && latestBid.compareAndSet(tempLatestBid, bid)) {
            return true;
        }

        boolean isBidApproved;

        do {
            tempLatestBid = latestBid.get();
            isBidApproved = bid.getPrice() > tempLatestBid.getPrice();
        } while (isBidApproved && !latestBid.compareAndSet(tempLatestBid, bid));

        if (isBidApproved) {
            notifier.sendOutdatedMessage(tempLatestBid);
        }

        return isBidApproved;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
