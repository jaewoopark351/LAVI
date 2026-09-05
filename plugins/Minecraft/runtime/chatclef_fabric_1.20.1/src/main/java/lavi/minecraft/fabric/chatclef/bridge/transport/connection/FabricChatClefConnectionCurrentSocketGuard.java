package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

import java.net.http.WebSocket;

//20260905_kpopmodder: Admit detach callbacks only for the currently owned WebSocket.
public final class FabricChatClefConnectionCurrentSocketGuard {
    private final FabricChatClefWebSocketConnectionState connectionState;

    public FabricChatClefConnectionCurrentSocketGuard(
            FabricChatClefWebSocketConnectionState connectionState
    ) {
        this.connectionState = connectionState;
    }

    public FabricChatClefConnectionDetachAdmission evaluate(WebSocket webSocket) {
        long generation = connectionState.activeConnectionGeneration();
        return new FabricChatClefConnectionDetachAdmission(
                connectionState.isCurrentSocket(webSocket),
                generation
        );
    }
}
