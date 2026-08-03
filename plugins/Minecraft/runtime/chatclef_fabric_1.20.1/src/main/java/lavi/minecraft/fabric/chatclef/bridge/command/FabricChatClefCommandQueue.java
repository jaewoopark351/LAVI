package lavi.minecraft.fabric.chatclef.bridge.command;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

//20260801_kpopmodder: Enforce one Fabric ChatClef command pending or active at a time.
public final class FabricChatClefCommandQueue {
    //20260804_kpopmodder: Keep queue ownership under this object's monitor instead of mixing synchronized and atomics.
    private final Deque<FabricChatClefCommandContext> pending = new ArrayDeque<>();
    private FabricChatClefCommandContext active;

    public synchronized boolean offer(FabricChatClefCommandContext context) {
        if (context == null || active != null || !pending.isEmpty()) {
            return false;
        }
        pending.offer(context);
        return true;
    }

    public synchronized Optional<FabricChatClefCommandContext> peekPending() {
        return Optional.ofNullable(pending.peek());
    }

    public synchronized Optional<FabricChatClefCommandContext> pollForDispatch() {
        if (active != null) {
            return Optional.empty();
        }
        FabricChatClefCommandContext context = pending.poll();
        if (context == null) {
            return Optional.empty();
        }
        active = context;
        return Optional.of(context);
    }

    public synchronized boolean hasActive() {
        return active != null;
    }

    public synchronized Optional<FabricChatClefCommandContext> activeContext() {
        return Optional.ofNullable(active);
    }

    public synchronized Optional<String> activeRequestId() {
        FabricChatClefCommandContext context = active;
        return Optional.ofNullable(context == null ? null : context.requestId());
    }

    public synchronized boolean complete(FabricChatClefCommandContext context) {
        if (context == null || active != context) {
            return false;
        }
        active = null;
        return true;
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
        FabricChatClefCommandContext activeContext = active;
        if (activeContext != null && activeContext.connectionGeneration() == connectionGeneration) {
            activeContext.markDetached(reason);
            active = null;
        }
    }

    public synchronized void clear(String reason) {
        pending.forEach(context -> context.markDetached(reason));
        pending.clear();
        FabricChatClefCommandContext activeContext = active;
        active = null;
        if (activeContext != null) {
            activeContext.markDetached(reason);
        }
    }
}
