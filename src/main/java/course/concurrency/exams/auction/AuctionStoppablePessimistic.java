package course.concurrency.exams.auction;

import static course.concurrency.exams.auction.Bid.DEFAULT_VALUE;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;
    private final Object lock;
    private volatile Bid latestBid;
    private volatile boolean isAuctionOpen;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.lock = new Object();
        this.latestBid = new Bid(DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE);
        this.isAuctionOpen = IS_AUCTION_OPEN_INITIALLY;
    }

    public boolean propose(Bid bid) {
        if (bid.getPrice() <= latestBid.getPrice() || !isAuctionOpen) {
            return false;
        }

        Bid tempLatestBid;
        boolean tempIsAuctionOpen;

        synchronized (lock) {
            tempLatestBid = latestBid;
            tempIsAuctionOpen = isAuctionOpen;

            if (bid.getPrice() <= tempLatestBid.getPrice() || !tempIsAuctionOpen) {
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

    public Bid stopAuction() {
        synchronized (lock) {
            isAuctionOpen = false;
        }

        return latestBid;
    }
}
