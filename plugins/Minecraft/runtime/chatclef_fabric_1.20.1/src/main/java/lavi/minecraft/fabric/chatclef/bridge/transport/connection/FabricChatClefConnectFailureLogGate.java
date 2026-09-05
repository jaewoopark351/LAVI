package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

//20260905_kpopmodder: Suppress only repeated WebSocket connection-failure diagnostics.

public final class FabricChatClefConnectFailureLogGate {
    private String lastLoggedConnectFailure;

    public boolean shouldLog(String message) {
        if (message.equals(lastLoggedConnectFailure)) {
            return false;
        }
        lastLoggedConnectFailure = message;
        return true;
    }

    public void reset() {
        lastLoggedConnectFailure = null;
    }
}
