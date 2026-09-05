package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit;

//20260905_kpopmodder: Publish canonical diagnostics for STOP commit quarantine boundaries.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.diagnostics.FabricChatClefStopControlTransitionEmitter;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;

public final class FabricChatClefStopControlCommitDiagnostics {
    private final FabricChatClefStopControlTransitionEmitter transitionEmitter;

    public FabricChatClefStopControlCommitDiagnostics(
            FabricChatClefStopControlTransitionEmitter transitionEmitter
    ) {
        this.transitionEmitter = transitionEmitter;
    }

    public void quarantined(FabricChatClefStopControlContext context, String disposition) {
        transitionEmitter.contextBoundary(
                context,
                disposition,
                "quarantined",
                "blocked",
                true,
                "none"
        );
    }
}
