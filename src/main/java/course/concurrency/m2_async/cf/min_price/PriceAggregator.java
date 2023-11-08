package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PriceAggregator {
    private static final long LIMIT_MILLIS = 3000;
    private static final long GAP_MILLIS = 5;
    private static final long SLA_MILLIS = LIMIT_MILLIS - GAP_MILLIS;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        List<CompletableFuture<Double>> priceRequests = shopIds.stream()
                .map(shopId -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executor)
                        .completeOnTimeout(Double.NaN, SLA_MILLIS, TimeUnit.MILLISECONDS)
                        .exceptionally(e -> Double.NaN)
                )
                .collect(Collectors.toList());

        return priceRequests.stream()
                .map(CompletableFuture::join)
                .filter(price -> !price.isNaN())
                .min(Double::compareTo)
                .orElse(Double.NaN);
    }
}
