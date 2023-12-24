package course.concurrency.exams.refactoring;

import course.concurrency.exams.refactoring.Others.MountTableManager;
import course.concurrency.exams.refactoring.Others.RouterState;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MountTableRefresherService {

    private enum RouterUpdateState {
        SUCCESS,
        FAIL,
        TIMEOUT
    }

    private final Function<String, MountTableManager> createManager =
            adminAddress -> new Others.MountTableManager(isLocalAdmin(adminAddress) ? "local" : adminAddress);

    private final Predicate<String> doRefresh = adminAddress -> {
        Thread.currentThread().setName("MountTableRefresh_" + adminAddress);
        return createManager.apply(adminAddress).refresh();
    };

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        List<Entry<String, CompletableFuture<RouterUpdateState>>> refreshers = routerStore.getCachedRecords()
                .stream()
                .map(RouterState::getAdminAddress)
                .filter(adminAddress -> adminAddress != null && !adminAddress.isEmpty())
                .map(adminAddress -> new SimpleEntry<>(
                                adminAddress,
                                CompletableFuture.supplyAsync(() -> doRefresh.test(adminAddress)
                                                ? RouterUpdateState.SUCCESS
                                                : RouterUpdateState.FAIL
                                        )
                                        .completeOnTimeout(RouterUpdateState.TIMEOUT, cacheUpdateTimeout,
                                                TimeUnit.MILLISECONDS)
                                        .exceptionally(e -> RouterUpdateState.FAIL)
                        )
                )
                .collect(Collectors.toList());

        if (!refreshers.isEmpty()) {
            invokeRefresh(refreshers);
        }
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<Entry<String, CompletableFuture<RouterUpdateState>>> refreshers) {
        AtomicBoolean isInterrupted = new AtomicBoolean(false);
        AtomicBoolean allReqCompleted = new AtomicBoolean(true);
        AtomicInteger failureCount = new AtomicInteger();

        CompletableFuture<Void> awaitAll = CompletableFuture.allOf(refreshers.stream()
                .map(Entry::getValue)
                .toArray(CompletableFuture[]::new)
        );

        try {
            awaitAll.get();
        } catch (InterruptedException | ExecutionException e) {
            isInterrupted.set(true);
        }

        for (Entry<String, CompletableFuture<RouterUpdateState>> refresher : refreshers) {
            RouterUpdateState value;

            try {
                value = refresher.getValue().get(0L, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                isInterrupted.set(true);
                value = RouterUpdateState.FAIL;
            }

            if (value != RouterUpdateState.SUCCESS) {
                if (value == RouterUpdateState.TIMEOUT) {
                    allReqCompleted.set(false);
                }

                removeFromCache(refresher.getKey());
                failureCount.incrementAndGet();
            }
        }

        logResult(refreshers.size() - failureCount.get(), failureCount.get(), allReqCompleted.get(),
                isInterrupted.get());
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(int successCount, int failureCount, boolean allReqCompleted, boolean isInterrupted) {
        if (isInterrupted) {
            log("Mount table cache refresher was interrupted.");
        }

        if (!allReqCompleted) {
            log("Not all router admins updated their cache");
        }

        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}