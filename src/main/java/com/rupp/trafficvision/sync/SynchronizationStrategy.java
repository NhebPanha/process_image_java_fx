package com.rupp.trafficvision.sync;

import java.util.function.Supplier;

/**
 * Strategy interface defining concurrent access synchronization contracts.
 * Implementations provide mutual exclusion (e.g. ReentrantLock) or concurrency throttling (e.g. Semaphore).
 */
public interface SynchronizationStrategy {

    /**
     * Acquires exclusive or permitted access according to this strategy.
     *
     * @throws InterruptedException if current thread is interrupted while waiting
     */
    void acquireAccess() throws InterruptedException;

    /**
     * Releases previously acquired access or permit.
     */
    void releaseAccess();

    /**
     * Executes a supplier action guarded by this synchronization strategy.
     *
     * @param action supplier to execute inside protected critical section
     * @param <T>    return type
     * @return action result
     * @throws InterruptedException if interrupted while waiting for lock/permit
     */
    <T> T executeWithProtection(Supplier<T> action) throws InterruptedException;

    /**
     * Executes a runnable action guarded by this synchronization strategy.
     *
     * @param action runnable to execute inside protected critical section
     * @throws InterruptedException if interrupted while waiting for lock/permit
     */
    void executeWithProtection(Runnable action) throws InterruptedException;

    /**
     * Returns the human-readable identifier of this synchronization mechanism.
     *
     * @return strategy name
     */
    String getStrategyName();

    /**
     * Returns detailed diagnostic information regarding lock/permit state.
     *
     * @return status description
     */
    String getStatusDescription();
}
