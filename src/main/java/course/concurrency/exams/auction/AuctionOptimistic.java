package course.concurrency.exams.auction;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private Bid latestBid;

    public boolean propose(Bid bid) {
        if (latestBid != null) {
            if (bid.getPrice() <= latestBid.getPrice()) {
                return false;
            }

            notifier.sendOutdatedMessage(latestBid);
        }

        latestBid = bid;
        return true;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
