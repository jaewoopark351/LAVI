package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Own only the mutable WebSocket lifecycle and connection identity.

import java.net.http.WebSocket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class FabricChatClefConnectionLifecycleState {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private final AtomicLong connectionGenerations = new AtomicLong(0L);
    private volatile WebSocket webSocket;
    private volatile long activeConnectionGeneration;

    public boolean beginRunning() {
        return running.compareAndSet(false, true);
    }

    public boolean endRunning() {
        return running.compareAndSet(true, false);
    }

    public boolean running() {
        return running.get();
    }

    public boolean beginConnecting() {
        return connecting.compareAndSet(false, true);
    }

    public void endConnecting() {
        connecting.set(false);
    }

    public long acceptOpen(WebSocket socket) {
        long generation = connectionGenerations.incrementAndGet();
        webSocket = socket;
        activeConnectionGeneration = generation;
        connecting.set(false);
        return generation;
    }

    public long detachCurrent() {
        long generation = activeConnectionGeneration;
        webSocket = null;
        activeConnectionGeneration = 0L;
        connecting.set(false);
        return generation;
    }

    public WebSocket webSocket() {
        return webSocket;
    }

    public long activeConnectionGeneration() {
        return activeConnectionGeneration;
    }

    public boolean isCurrentSocket(WebSocket socket) {
        return socket != null && socket == webSocket;
    }
}
