package course.concurrency.exams.auction;

public interface AuctionStoppable extends Auction {
    boolean IS_AUCTION_OPEN_INITIALLY = true;

    // stop auction and return latest bid
    Bid stopAuction();
}
