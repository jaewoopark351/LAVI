package lavi.minecraft.fabric.chatclef.bridge.command;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

//20260801_kpopmodder: Enforce one Fabric ChatClef command pending or active at a time.
public final class FabricChatClefCommandQueue {
    private final ConcurrentLinkedQueue<FabricChatClefCommandRequest> pending = new ConcurrentLinkedQueue<>();
    private final AtomicReference<FabricChatClefCommandRequest> active = new AtomicReference<>();

    public synchronized boolean offer(FabricChatClefCommandRequest request) {
        if (request == null || active.get() != null || !pending.isEmpty()) {
            return false;
        }
        pending.offer(request);
        return true;
    }

    public synchronized Optional<FabricChatClefCommandRequest> peekPending() {
        return Optional.ofNullable(pending.peek());
    }

    public synchronized Optional<FabricChatClefCommandRequest> pollForDispatch() {
        if (active.get() != null) {
            return Optional.empty();
        }
        FabricChatClefCommandRequest request = pending.poll();
        if (request == null) {
            return Optional.empty();
        }
        if (!active.compareAndSet(null, request)) {
            pending.offer(request);
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public synchronized boolean hasActive() {
        return active.get() != null;
    }

    public synchronized Optional<String> activeRequestId() {
        FabricChatClefCommandRequest request = active.get();
        return Optional.ofNullable(request == null ? null : request.requestId);
    }

    public synchronized void complete(String requestId) {
        FabricChatClefCommandRequest request = active.get();
        if (request != null && request.requestId.equals(requestId)) {
            active.compareAndSet(request, null);
        }
    }

    public synchronized void removePending(String requestId) {
        pending.removeIf(request -> request.requestId.equals(requestId));
    }

    public synchronized void clear() {
        pending.clear();
        active.set(null);
    }
}
