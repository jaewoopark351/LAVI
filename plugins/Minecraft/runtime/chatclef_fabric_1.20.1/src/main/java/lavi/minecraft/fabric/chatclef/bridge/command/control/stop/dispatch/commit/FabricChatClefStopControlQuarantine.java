package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit;

//20260905_kpopmodder: Quarantine an unreleasable STOP without fabricating a wire result.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;

public final class FabricChatClefStopControlQuarantine {
    private final FabricChatClefStopControlDedupeFinalizer dedupeFinalizer;
    private final FabricChatClefStopControlCommitDiagnostics diagnostics;

    public FabricChatClefStopControlQuarantine(
            FabricChatClefStopControlDedupeFinalizer dedupeFinalizer,
            FabricChatClefStopControlCommitDiagnostics diagnostics
    ) {
        this.dedupeFinalizer = dedupeFinalizer;
        this.diagnostics = diagnostics;
    }

    public void apply(FabricChatClefStopControlContext context, String disposition) {
        context.markQuarantined();
        dedupeFinalizer.finalizeIdentity(context);
        diagnostics.quarantined(context, disposition);
    }
}
