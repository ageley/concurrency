package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

import static course.concurrency.exams.auction.Bid.DEFAULT_VALUE;

public class AuctionStoppableOptimistic implements AuctionStoppable {
    private final Notifier notifier;
    private final AtomicMarkableReference<Bid> latestBid;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
        this.latestBid = new AtomicMarkableReference<>(new Bid(DEFAULT_VALUE, DEFAULT_VALUE, DEFAULT_VALUE),
                IS_AUCTION_OPEN_INITIALLY);
    }

    public boolean propose(Bid bid) {
        Bid tempLatestBid;
        boolean isAuctionOpen;

        do {
            tempLatestBid = latestBid.getReference();
            isAuctionOpen = latestBid.isMarked();

            if (bid.getPrice() <= tempLatestBid.getPrice() || !isAuctionOpen) {
                return false;
            }
        } while (!latestBid.compareAndSet(tempLatestBid, bid, true, true));

        notifier.sendOutdatedMessage(tempLatestBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        Bid tempLatestBid;

        do {
            tempLatestBid = latestBid.getReference();
        } while (!latestBid.attemptMark(tempLatestBid, false));

        return tempLatestBid;
    }
}
