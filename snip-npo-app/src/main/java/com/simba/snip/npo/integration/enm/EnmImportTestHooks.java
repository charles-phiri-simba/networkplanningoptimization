package com.simba.snip.npo.integration.enm;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class EnmImportTestHooks {

    private final AtomicReference<ConnectorCancellationToken> token = new AtomicReference<>();
    private final AtomicReference<Runnable> afterFirstPage = new AtomicReference<>();
    private final AtomicReference<Runnable> beforeReconcile = new AtomicReference<>();
    private final AtomicReference<Runnable> afterReconcileBeforeCheckpoint = new AtomicReference<>();
    private final AtomicReference<Consumer<ConnectorCancellationToken>> onBind = new AtomicReference<>();

    public void bind(ConnectorCancellationToken cancellationToken) {
        token.set(cancellationToken);
        Consumer<ConnectorCancellationToken> bound = onBind.getAndSet(null);
        if (bound != null) {
            bound.accept(cancellationToken);
        }
    }

    public void runAfterFirstPage() {
        Runnable action = afterFirstPage.getAndSet(null);
        if (action != null) {
            action.run();
        }
    }

    public void runBeforeReconcile() {
        Runnable action = beforeReconcile.getAndSet(null);
        if (action != null) {
            action.run();
        }
    }

    public void runAfterReconcileBeforeCheckpoint() {
        Runnable action = afterReconcileBeforeCheckpoint.getAndSet(null);
        if (action != null) {
            action.run();
        }
    }

    public ConnectorCancellationToken token() {
        return token.get();
    }

    public void onBind(Consumer<ConnectorCancellationToken> consumer) {
        onBind.set(consumer);
    }

    public void afterFirstPage(Runnable action) {
        afterFirstPage.set(action);
    }

    public void beforeReconcile(Runnable action) {
        beforeReconcile.set(action);
    }

    public void afterReconcileBeforeCheckpoint(Runnable action) {
        afterReconcileBeforeCheckpoint.set(action);
    }

    public void clear() {
        token.set(null);
        afterFirstPage.set(null);
        beforeReconcile.set(null);
        afterReconcileBeforeCheckpoint.set(null);
        onBind.set(null);
    }
}
