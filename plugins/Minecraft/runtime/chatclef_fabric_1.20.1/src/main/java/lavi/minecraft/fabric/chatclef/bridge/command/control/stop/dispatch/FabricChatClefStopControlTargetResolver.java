package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Resolve a captured ordinary command against the immutable STOP target scope.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;

public final class FabricChatClefStopControlTargetResolver {
    public String resolve(
            FabricChatClefStopControlRequest request,
            FabricChatClefOrdinaryCommandStopCapture capture
    ) {
        if (capture.absent()) {
            return "none";
        }
        if (request.targetScope() == FabricChatClefStopControlTargetScope.TRACKED_COMMAND
                && request.matchesTarget(capture.context())) {
            return "exact";
        }
        return "captured_current";
    }
}
