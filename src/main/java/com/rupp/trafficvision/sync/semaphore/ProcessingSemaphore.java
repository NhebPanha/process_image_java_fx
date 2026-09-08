package com.rupp.trafficvision.sync.semaphore;

import com.rupp.trafficvision.sync.SynchronizationStrategy;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Synchronization strategy using {@link Semaphore} to regulate concurrency and throttle
 * the maximum number of simultaneous image-processing tasks active in the thread pool.
 */
public class ProcessingSemaphore implements SynchronizationStrategy {

    private final int totalPermits;
    private final Semaphore semaphore;

    public ProcessingSemaphore(int permits) {
        this.totalPermits = Math.max(1, permits);
        this.semaphore = new Semaphore(this.totalPermits, true);
    }

    public ProcessingSemaphore() {
        this(2); // Default to 2 concurrent processing tasks
    }

    @Override
    public void acquireAccess() throws InterruptedException {
        semaphore.acquire();
    }

    @Override
    public void releaseAccess() {
        semaphore.release();
    }

    @Override
    public <T> T executeWithProtection(Supplier<T> action) throws InterruptedException {
        acquireAccess();
        try {
            return action.get();
        } finally {
            releaseAccess();
        }
    }

    @Override
    public void executeWithProtection(Runnable action) throws InterruptedException {
        acquireAccess();
        try {
            action.run();
        } finally {
            releaseAccess();
        }
    }

    @Override
    public String getStrategyName() {
        return "Semaphore (Concurrency Limit: " + totalPermits + ")";
    }

    @Override
    public String getStatusDescription() {
        return String.format("Semaphore [AvailablePermits: %d/%d, QueuedThreads: %d]",
                semaphore.availablePermits(), totalPermits, semaphore.getQueueLength());
    }

    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    public int getTotalPermits() {
        return totalPermits;
    }
}
