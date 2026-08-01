package lavi.minecraft.fabric.chatclef.bridge.command;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

//20260801_kpopmodder: Enforce one Fabric ChatClef command pending or active at a time.
public final class FabricChatClefCommandQueue {
    private final ConcurrentLinkedQueue<FabricChatClefCommandContext> pending = new ConcurrentLinkedQueue<>();
    private final AtomicReference<FabricChatClefCommandContext> active = new AtomicReference<>();

    public synchronized boolean offer(FabricChatClefCommandContext context) {
        if (context == null || active.get() != null || !pending.isEmpty()) {
            return false;
        }
        pending.offer(context);
        return true;
    }

    public synchronized Optional<FabricChatClefCommandContext> peekPending() {
        return Optional.ofNullable(pending.peek());
    }

    public synchronized Optional<FabricChatClefCommandContext> pollForDispatch() {
        if (active.get() != null) {
            return Optional.empty();
        }
        FabricChatClefCommandContext context = pending.poll();
        if (context == null) {
            return Optional.empty();
        }
        if (!active.compareAndSet(null, context)) {
            pending.offer(context);
            return Optional.empty();
        }
        return Optional.of(context);
    }

    public synchronized boolean hasActive() {
        return active.get() != null;
    }

    public synchronized Optional<FabricChatClefCommandContext> activeContext() {
        return Optional.ofNullable(active.get());
    }

    public synchronized Optional<String> activeRequestId() {
        FabricChatClefCommandContext context = active.get();
        return Optional.ofNullable(context == null ? null : context.requestId());
    }

    public synchronized boolean complete(FabricChatClefCommandContext context) {
        return context != null && active.compareAndSet(context, null);
    }

    public synchronized boolean removePending(FabricChatClefCommandContext context) {
        return pending.remove(context);
    }

    public synchronized void detachConnection(long connectionGeneration, String reason) {
        pending.removeIf(context -> {
            boolean matches = context.connectionGeneration() == connectionGeneration;
            if (matches) {
                context.markDetached(reason);
            }
            return matches;
        });
        FabricChatClefCommandContext activeContext = active.get();
        if (activeContext != null && activeContext.connectionGeneration() == connectionGeneration) {
            activeContext.markDetached(reason);
            active.compareAndSet(activeContext, null);
        }
    }

    public synchronized void clear(String reason) {
        pending.forEach(context -> context.markDetached(reason));
        pending.clear();
        FabricChatClefCommandContext activeContext = active.getAndSet(null);
        if (activeContext != null) {
            activeContext.markDetached(reason);
        }
    }
}
