package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Invoke the established AltoClef user-task cancellation boundary.

import adris.altoclef.AltoClef;

public final class FabricChatClefAltoClefDetachedCommandCancellation
        implements FabricChatClefDetachedCommandCancellation {
    private final FabricChatClefConnectionDetachDiagnostics diagnostics;

    public FabricChatClefAltoClefDetachedCommandCancellation(
            FabricChatClefConnectionDetachDiagnostics diagnostics
    ) {
        this.diagnostics = diagnostics;
    }

    @Override
    public void cancel(String rootMatchReason) {
        AltoClef mod = AltoClef.getInstance();
        if (mod == null || mod.getUserTaskChain() == null) {
            diagnostics.cancellationUnavailable();
            return;
        }
        diagnostics.cancellingOwnedTask(rootMatchReason);
        mod.cancelUserTask();
    }
}
