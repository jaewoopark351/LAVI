package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Preserve the connection-state API over focused lifecycle and diagnostic state owners.

import java.net.http.WebSocket;

public final class FabricChatClefWebSocketConnectionState {
    private final FabricChatClefConnectionLifecycleState lifecycleState =
            new FabricChatClefConnectionLifecycleState();
    private final FabricChatClefConnectFailureLogGate connectFailureLogGate =
            new FabricChatClefConnectFailureLogGate();

    public boolean beginRunning() {
        return lifecycleState.beginRunning();
    }

    public boolean endRunning() {
        return lifecycleState.endRunning();
    }

    public boolean running() {
        return lifecycleState.running();
    }

    public boolean beginConnecting() {
        return lifecycleState.beginConnecting();
    }

    public void endConnecting() {
        lifecycleState.endConnecting();
    }

    public long acceptOpen(WebSocket socket) {
        long generation = lifecycleState.acceptOpen(socket);
        connectFailureLogGate.reset();
        return generation;
    }

    public long detachCurrent() {
        return lifecycleState.detachCurrent();
    }

    public WebSocket webSocket() {
        return lifecycleState.webSocket();
    }

    public long activeConnectionGeneration() {
        return lifecycleState.activeConnectionGeneration();
    }

    public boolean isCurrentSocket(WebSocket socket) {
        return lifecycleState.isCurrentSocket(socket);
    }

    public boolean shouldLogConnectFailure(String message) {
        return connectFailureLogGate.shouldLog(message);
    }
}
