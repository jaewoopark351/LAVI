package lavi.minecraft.fabric.chatclef.bridge.command;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

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
        FabricChatClefCommandContext activeBefore = active;
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
        if (context == null || active != context) {
            FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                    "stale_terminal_attempt",
                    context,
                    activeBefore,
                    active,
                    false,
                    ownershipBefore,
                    FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot()
            );
            return false;
        }
        active = null;
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                "terminal_result",
                context,
                activeBefore,
                active,
                true,
                ownershipBefore,
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot()
        );
        return true;
    }

    public synchronized boolean removePending(FabricChatClefCommandContext context) {
        return pending.remove(context);
    }

    public synchronized void detachConnection(long connectionGeneration, String reason) {
        FabricChatClefCommandContext activeBefore = active;
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
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
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                "connection_detached",
                activeContext,
                activeBefore,
                active,
                activeBefore != active,
                ownershipBefore,
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot()
        );
    }

    public synchronized void clear(String reason) {
        FabricChatClefCommandContext activeBefore = active;
        FabricChatClefTaskOwnershipSnapshot ownershipBefore =
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot();
        pending.forEach(context -> context.markDetached(reason));
        pending.clear();
        FabricChatClefCommandContext activeContext = active;
        active = null;
        if (activeContext != null) {
            activeContext.markDetached(reason);
        }
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                "queue_cleared",
                activeContext,
                activeBefore,
                active,
                activeBefore != active,
                ownershipBefore,
                FabricChatClefCommandContextUnbindDiagnostics.captureOwnershipSnapshot()
        );
    }
}
