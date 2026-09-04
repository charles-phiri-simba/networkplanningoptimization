package com.simba.snip.npo.vendorcertification.service;

/**
 * Optional same-transaction hooks for tests. Production has no bean.
 * Must never mutate vendor state.
 */
public interface InvalidationTransactionHook {

    default void afterLocks() {
    }

    default void afterRequiredWrites() {
    }
}
