package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Own the current WebSocket state mutation for an admitted detach.
public final class FabricChatClefConnectionDetachCommit {
    private final FabricChatClefWebSocketConnectionState connectionState;

    public FabricChatClefConnectionDetachCommit(
            FabricChatClefWebSocketConnectionState connectionState
    ) {
        this.connectionState = connectionState;
    }

    public void detachCurrent() {
        connectionState.detachCurrent();
    }
}
