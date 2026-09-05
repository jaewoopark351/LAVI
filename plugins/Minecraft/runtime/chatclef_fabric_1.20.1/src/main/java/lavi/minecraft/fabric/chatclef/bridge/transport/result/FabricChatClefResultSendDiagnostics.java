package lavi.minecraft.fabric.chatclef.bridge.transport.result;

//20260905_kpopmodder: Keep command-result transport diagnostics out of send orchestration.

import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.concurrent.CompletionException;

public final class FabricChatClefResultSendDiagnostics {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefResultSendDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void inactiveGeneration(long generation, long activeGeneration) {
        diagnostics.warn(
                "ignored command_result for inactive generation="
                        + generation
                        + " active_generation="
                        + activeGeneration
        );
    }

    public String encodeFailed(Exception error) {
        String detail = throwableMessage(error);
        diagnostics.warn("command_result encode failed " + detail);
        return detail;
    }

    public String asyncSendFailed(Throwable error) {
        String detail = throwableMessage(error);
        diagnostics.warn("command_result async send failed " + detail);
        return detail;
    }

    public String sendFailed(Exception error) {
        String detail = throwableMessage(error);
        diagnostics.warn("command_result send failed " + detail);
        return detail;
    }

    private static String throwableMessage(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null
                ? error.getCause()
                : error;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
