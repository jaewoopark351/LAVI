package lavi.minecraft.fabric.chatclef.bridge.transport.connection;

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

//20260905_kpopmodder: Keep detach callback diagnostics separate from connection mutation.
public final class FabricChatClefConnectionDetachDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefConnectionDetachDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void staleClose(int statusCode, String reason) {
        diagnostics.warn(
                "ignored close from stale WebSocket status="
                        + statusCode
                        + " reason="
                        + reason
        );
    }

    public void closed(long generation, int statusCode, String reason) {
        diagnostics.warn(
                "closed generation="
                        + generation
                        + " status="
                        + statusCode
                        + " reason="
                        + reason
        );
    }

    public void staleError(Throwable error) {
        diagnostics.warn("ignored error from stale WebSocket " + errorMessage(error));
    }

    public void failed(long generation, String message) {
        diagnostics.warn("connection error generation=" + generation + " " + message);
    }

    public String errorMessage(Throwable error) {
        return error.getClass().getSimpleName() + ": " + error.getMessage();
    }
}
