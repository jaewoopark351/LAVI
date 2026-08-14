package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachResult;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

//20260801_kpopmodder: Enforce one Fabric ChatClef command pending or active at a time.
public final class FabricChatClefCommandQueue {
    //20260804_kpopmodder: Keep queue ownership under this object's monitor instead of mixing synchronized and atomics.
    private final Deque<FabricChatClefCommandContext> pending = new ArrayDeque<>();
    private final Deque<FabricChatClefConnectionDetachedEvent> connectionDetachedEvents = new ArrayDeque<>();
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

    public synchronized boolean isActive(FabricChatClefCommandContext context) {
        return context != null && active == context;
    }

    public synchronized FabricChatClefCommandQueueCompletion complete(
            FabricChatClefCommandContext context,
            String reason
    ) {
        FabricChatClefCommandContext activeBefore = active;
        if (context == null || active != context) {
            return FabricChatClefCommandQueueCompletion.of(
                    reason,
                    context,
                    activeBefore,
                    active,
                    false
            );
        }
        active = null;
        return FabricChatClefCommandQueueCompletion.of(
                reason,
                context,
                activeBefore,
                active,
                true
        );
    }

    public synchronized boolean removePending(FabricChatClefCommandContext context) {
        return pending.remove(context);
    }

    public synchronized void enqueueConnectionDetached(long connectionGeneration, String reason) {
        connectionDetachedEvents.offer(FabricChatClefConnectionDetachedEvent.of(connectionGeneration, reason));
    }

    public synchronized Optional<FabricChatClefConnectionDetachedEvent> pollConnectionDetached() {
        return Optional.ofNullable(connectionDetachedEvents.poll());
    }

    public synchronized FabricChatClefConnectionDetachResult markConnectionDetached(
            FabricChatClefConnectionDetachedEvent event
    ) {
        FabricChatClefCommandContext activeBefore = active;
        int pendingRemovedCount = removePendingForDetachedConnection(event);
        FabricChatClefCommandContext detachedActive = null;
        FabricChatClefCommandContext activeContext = active;
        if (activeContext != null && activeContext.connectionGeneration() == event.connectionGeneration()) {
            activeContext.markDetached(event.reason());
            detachedActive = activeContext;
        }
        return FabricChatClefConnectionDetachResult.of(
                event,
                activeBefore,
                active,
                detachedActive,
                pendingRemovedCount
        );
    }

    public synchronized FabricChatClefCommandQueueCompletion clearDetachedActive(
            FabricChatClefCommandContext context,
            String reason
    ) {
        FabricChatClefCommandContext activeBefore = active;
        if (context == null || active != context) {
            return FabricChatClefCommandQueueCompletion.of(
                    reason,
                    context,
                    activeBefore,
                    active,
                    false
            );
        }
        active = null;
        return FabricChatClefCommandQueueCompletion.of(
                reason,
                context,
                activeBefore,
                active,
                true
        );
    }

    public synchronized void clear(String reason) {
        pending.forEach(context -> context.markDetached(reason));
        pending.clear();
        FabricChatClefCommandContext activeContext = active;
        active = null;
        if (activeContext != null) {
            activeContext.markDetached(reason);
        }
        connectionDetachedEvents.clear();
    }

    private int removePendingForDetachedConnection(FabricChatClefConnectionDetachedEvent event) {
        int removed = 0;
        Deque<FabricChatClefCommandContext> retained = new ArrayDeque<>();
        while (!pending.isEmpty()) {
            FabricChatClefCommandContext context = pending.poll();
            if (context != null && context.connectionGeneration() == event.connectionGeneration()) {
                context.markDetached(event.reason());
                removed++;
            } else if (context != null) {
                retained.offer(context);
            }
        }
        pending.addAll(retained);
        return removed;
    }
}
