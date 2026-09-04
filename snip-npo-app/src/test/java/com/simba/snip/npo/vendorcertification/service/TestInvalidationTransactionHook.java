package com.simba.snip.npo.vendorcertification.service;

import org.springframework.stereotype.Component;

@Component
public class TestInvalidationTransactionHook implements InvalidationTransactionHook {

    private volatile Runnable afterLocksAction;
    private volatile Runnable afterWritesAction;

    public void setAfterLocksAction(Runnable afterLocksAction) {
        this.afterLocksAction = afterLocksAction;
    }

    public void failAfterRequiredWrites() {
        this.afterWritesAction = () -> {
            throw new IllegalStateException("injected late invalidation failure");
        };
    }

    public void reset() {
        afterLocksAction = null;
        afterWritesAction = null;
    }

    @Override
    public void afterLocks() {
        Runnable action = afterLocksAction;
        if (action != null) {
            action.run();
        }
    }

    @Override
    public void afterRequiredWrites() {
        Runnable action = afterWritesAction;
        afterWritesAction = null;
        if (action != null) {
            action.run();
        }
    }
}
