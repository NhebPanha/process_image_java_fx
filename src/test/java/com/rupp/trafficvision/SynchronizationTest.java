package com.rupp.trafficvision;

import com.rupp.trafficvision.model.DetectionResult;
import com.rupp.trafficvision.model.VehicleType;
import com.rupp.trafficvision.statistics.StatisticsManager;
import com.rupp.trafficvision.sync.lock.ProcessingLock;
import com.rupp.trafficvision.sync.semaphore.ProcessingSemaphore;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SynchronizationTest {

    @Test
    public void testProcessingLockMutualExclusion() throws InterruptedException {
        ProcessingLock lock = new ProcessingLock();
        AtomicInteger counter = new AtomicInteger(0);
        int threads = 10;
        int incrementsPerThread = 1000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < incrementsPerThread; j++) {
                        lock.executeWithProtection(counter::incrementAndGet);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(threads * incrementsPerThread, counter.get());
    }

    @Test
    public void testProcessingSemaphorePermits() throws InterruptedException {
        ProcessingSemaphore semaphore = new ProcessingSemaphore(2);
        assertEquals(2, semaphore.getAvailablePermits());

        semaphore.acquireAccess();
        assertEquals(1, semaphore.getAvailablePermits());

        semaphore.acquireAccess();
        assertEquals(0, semaphore.getAvailablePermits());

        semaphore.releaseAccess();
        assertEquals(1, semaphore.getAvailablePermits());

        semaphore.releaseAccess();
        assertEquals(2, semaphore.getAvailablePermits());
    }

    @Test
    public void testStatisticsManagerConcurrentAggregation() throws InterruptedException {
        StatisticsManager stats = new StatisticsManager(new ProcessingLock());
        int numTasks = 50;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(numTasks);

        for (int i = 0; i < numTasks; i++) {
            final int index = i;
            pool.submit(() -> {
                try {
                    stats.recordImageSubmitted();
                    VehicleType type = VehicleType.values()[index % 5];
                    DetectionResult res = new DetectionResult(
                            "ID-" + index,
                            type,
                            0.92,
                            50 + (index % 30),
                            null,
                            "DETECTED",
                            "Valid"
                    );
                    stats.recordResult(res);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        pool.shutdown();

        assertEquals(numTasks, stats.getTotalImages());
        assertEquals(numTasks, stats.getProcessedImages());
        assertEquals(numTasks, stats.getSuccessfulDetections());
        assertEquals(0, stats.getFailedDetections());
        assertTrue(stats.getAverageProcessingTime() >= 50.0);
    }
}
