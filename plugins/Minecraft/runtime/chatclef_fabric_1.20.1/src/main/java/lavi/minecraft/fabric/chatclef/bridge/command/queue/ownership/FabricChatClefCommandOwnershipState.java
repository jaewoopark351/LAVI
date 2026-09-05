package lavi.minecraft.fabric.chatclef.bridge.command.queue.ownership;

//20260905_kpopmodder: Own pending and active ordinary-command state transitions.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachResult;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefPendingDetachMutation;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class FabricChatClefCommandOwnershipState {
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
        return Optional.ofNullable(active == null ? null : active.requestId());
    }

    public synchronized boolean isActive(FabricChatClefCommandContext context) {
        return context != null && active == context;
    }

    public synchronized boolean isPending(FabricChatClefCommandContext context) {
        return context != null && pending.contains(context);
    }

    public synchronized FabricChatClefCommandOwnershipSnapshot snapshot() {
        return new FabricChatClefCommandOwnershipSnapshot(active, pending.peek());
    }

    public synchronized FabricChatClefCommandQueueCompletion complete(
            FabricChatClefCommandContext context,
            String reason
    ) {
        FabricChatClefCommandContext activeBefore = active;
        if (context == null || active != context) {
            return FabricChatClefCommandQueueCompletion.of(reason, context, activeBefore, active, false);
        }
        active = null;
        return FabricChatClefCommandQueueCompletion.of(reason, context, activeBefore, active, true);
    }

    public synchronized boolean removePending(FabricChatClefCommandContext context) {
        return pending.remove(context);
    }

    public synchronized FabricChatClefConnectionDetachResult markConnectionDetached(
            FabricChatClefConnectionDetachedEvent event
    ) {
        FabricChatClefCommandContext activeBefore = active;
        FabricChatClefPendingDetachMutation pendingMutation = removePendingForDetachedConnection(event);
        FabricChatClefCommandContext detachedActive = null;
        if (active != null && active.connectionGeneration() == event.connectionGeneration()) {
            active.markDetached(event.reason());
            detachedActive = active;
        }
        return FabricChatClefConnectionDetachResult.of(
                event,
                activeBefore,
                active,
                detachedActive,
                pendingMutation.removedCount(),
                pendingMutation.inFlightRetainedCount()
        );
    }

    public synchronized FabricChatClefCommandQueueCompletion clearDetachedActive(
            FabricChatClefCommandContext context,
            String reason
    ) {
        return complete(context, reason);
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

    private FabricChatClefPendingDetachMutation removePendingForDetachedConnection(
            FabricChatClefConnectionDetachedEvent event
    ) {
        int removed = 0;
        int retainedInFlight = 0;
        Deque<FabricChatClefCommandContext> retained = new ArrayDeque<>();
        while (!pending.isEmpty()) {
            FabricChatClefCommandContext context = pending.poll();
            if (context != null && context.connectionGeneration() == event.connectionGeneration()) {
                context.markDetached(event.reason());
                if (context.terminalSendInFlight()) {
                    retained.offer(context);
                    retainedInFlight++;
                } else {
                    removed++;
                }
            } else if (context != null) {
                retained.offer(context);
            }
        }
        pending.addAll(retained);
        return FabricChatClefPendingDetachMutation.of(removed, retainedInFlight);
    }
}
