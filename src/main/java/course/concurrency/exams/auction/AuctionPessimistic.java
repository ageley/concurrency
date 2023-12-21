package course.concurrency.exams.auction;

import static course.concurrency.exams.auction.Bid.DEFAULT_VALUE;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;
    private final Object lock;
    private volatile Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.lock = new Object();
        this.latestBid = new Bid(DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE);
    }

    public boolean propose(Bid bid) {
        if (bid.getPrice() <= latestBid.getPrice()) {
            return false;
        }

        Bid tempLatestBid;

        synchronized (lock) {
            tempLatestBid = latestBid;

            if (bid.getPrice() <= tempLatestBid.getPrice()) {
                return false;
            }

            latestBid = bid;
        }

        notifier.sendOutdatedMessage(tempLatestBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
