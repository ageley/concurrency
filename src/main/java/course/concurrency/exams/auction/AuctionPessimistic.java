package course.concurrency.exams.auction;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;
    private final ReadWriteLock lock;
    private Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
        this.lock = new ReentrantReadWriteLock();
    }

    public boolean propose(Bid bid) {
        Lock writeLock = lock.writeLock();

        try {
            writeLock.lock();

            if (latestBid != null) {
                if (bid.getPrice() <= latestBid.getPrice()) {
                    return false;
                }

                notifier.sendOutdatedMessage(latestBid);
            }

            latestBid = bid;
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    public Bid getLatestBid() {
        Lock readLock = lock.readLock();

        try {
            readLock.lock();
            return latestBid;
        } finally {
            readLock.unlock();
        }
    }
}
