package course.concurrency.m3_shared.collections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class RestaurantService {

    private final Map<String, Restaurant> restaurantMap;
    private final Map<String, AtomicInteger> stat;
    private final ReadWriteLock lock;

    public RestaurantService() {
        this.restaurantMap = new ConcurrentHashMap() {{
            put("A", new Restaurant("A"));
            put("B", new Restaurant("B"));
            put("C", new Restaurant("C"));
        }};
        this.stat = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public void addToStat(String restaurantName) {
        Lock operationalLock = lock.readLock();

        try {
            operationalLock.lock();
            stat.computeIfAbsent(restaurantName, name -> new AtomicInteger()).incrementAndGet();
        } finally {
            operationalLock.unlock();
        }
    }

    public Set<String> printStat() {
        Lock reportingLock = lock.writeLock();

        try {
            reportingLock.lock();
            return stat.entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + " - " + entry.getValue())
                    .collect(Collectors.toSet());
        } finally {
            reportingLock.unlock();
        }
    }
}
