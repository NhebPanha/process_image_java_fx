package com.rupp.trafficvision.sync.lock;

import com.rupp.trafficvision.sync.SynchronizationStrategy;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Synchronization strategy using {@link ReentrantLock} to enforce strict mutual exclusion (Mutex)
 * over shared processing statistics and critical metrics.
 */
public class ProcessingLock implements SynchronizationStrategy {

    private final ReentrantLock lock;

    public ProcessingLock() {
        // Fair locking policy to prevent worker thread starvation
        this.lock = new ReentrantLock(true);
    }

    @Override
    public void acquireAccess() throws InterruptedException {
        lock.lockInterruptibly();
    }

    @Override
    public void releaseAccess() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
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
        return "ReentrantLock (Mutual Exclusion)";
    }

    @Override
    public String getStatusDescription() {
        return String.format("ReentrantLock [Held: %b, HoldCount: %d, QueuedThreads: %d]",
                lock.isLocked(), lock.getHoldCount(), lock.getQueueLength());
    }

    public ReentrantLock getUnderlyingLock() {
        return lock;
    }
}
