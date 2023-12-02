package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;
    private final Object lock;
    private volatile Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.lock = new Object();
    }

    public boolean propose(Bid bid) {
        if (latestBid == null) {
            synchronized (lock) {
                if (latestBid == null) {
                    latestBid = bid;
                    return true;
                }
            }
        }

        if (bid.getPrice() <= latestBid.getPrice()) {
            return false;
        }

        Bid tempLatestBid;
        boolean isBidApproved;

        synchronized (lock) {
            tempLatestBid = latestBid;
            isBidApproved = bid.getPrice() > tempLatestBid.getPrice();

            if (isBidApproved) {
                latestBid = bid;
            }
        }

        if (isBidApproved) {
            notifier.sendOutdatedMessage(tempLatestBid);
        }

        return isBidApproved;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
