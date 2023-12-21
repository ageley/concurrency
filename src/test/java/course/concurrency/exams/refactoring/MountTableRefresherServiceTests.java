package course.concurrency.exams.refactoring;

import course.concurrency.exams.refactoring.Others.RouterState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MountTableRefresherServiceTests {
    private static final List<String> ADDRESSES = List.of("123", "local6", "789", "local");
    private static final long CACHE_UPDATE_TIMEOUT = 1000L;

    private MountTableRefresherService service;
    private Others.RouterStore routerStore;
    private Others.LoadingCache routerClientsCache;
    private MountTableRefresherService mockedService;
    private List<Others.RouterState> states;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(CACHE_UPDATE_TIMEOUT);
        routerStore = mock(Others.RouterStore.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        mockedService = Mockito.spy(service);
        states = ADDRESSES.stream()
                .map(RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
        service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    void allDone() {
        // when
        try (MockedConstruction<Others.MountTableManager> manager = Mockito.mockConstructionWithAnswer(
                Others.MountTableManager.class,
                invocation -> true
        )) {
            mockedService.refresh();
        }

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    void noSuccessfulTasks() {
        // when
        try (MockedConstruction<Others.MountTableManager> manager = Mockito.mockConstructionWithAnswer(
                Others.MountTableManager.class,
                invocation -> false
        )) {
            mockedService.refresh();
        }

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    void halfSuccessedTasks() {
        // when
        try (MockedConstruction<Others.MountTableManager> manager = Mockito.mockConstructionWithAnswer(
                Others.MountTableManager.class,
                invocation -> true,
                invocation -> true,
                invocation -> false,
                invocation -> false
        )) {
            mockedService.refresh();
        }

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    void exceptionInOneTask() {
        // when
        try (MockedConstruction<Others.MountTableManager> manager = Mockito.mockConstructionWithAnswer(
                Others.MountTableManager.class,
                invocation -> {
                    throw new RuntimeException();
                },
                invocation -> true
        )) {
            mockedService.refresh();
        }

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    void oneTaskExceedTimeout() {
        // when
        try (MockedConstruction<Others.MountTableManager> manager = Mockito.mockConstructionWithAnswer(
                Others.MountTableManager.class,
                invocation -> {
                    Thread.sleep(CACHE_UPDATE_TIMEOUT + 5);
                    return true;
                },
                invocation -> true
        )) {
            mockedService.refresh();
        }

        // then
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

}
