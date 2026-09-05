package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage;

//20260905_kpopmodder: Own ordinary-command STOP marker bind and exceptional cleanup.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlCommandLifecycle;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlTaskOwnershipReader;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefOrdinaryCommandStopCapture;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

public final class FabricChatClefStopControlMarkerBindingStage {
    private final FabricChatClefStopControlCommandLifecycle commandLifecycle;
    private final FabricChatClefStopControlTaskOwnershipReader taskOwnershipReader;

    public FabricChatClefStopControlMarkerBindingStage(
            FabricChatClefStopControlCommandLifecycle commandLifecycle,
            FabricChatClefStopControlTaskOwnershipReader taskOwnershipReader
    ) {
        this.commandLifecycle = commandLifecycle;
        this.taskOwnershipReader = taskOwnershipReader;
    }

    public FabricChatClefStopControlMarkerBindingResult bind(
            FabricChatClefStopControlContext context,
            FabricChatClefOrdinaryCommandStopCapture capture
    ) {
        if (capture.pending() && capture.context().terminalPayloadCommitted()) {
            return FabricChatClefStopControlMarkerBindingResult.failed("target_observation_failed");
        }
        if (!capture.active()) {
            return FabricChatClefStopControlMarkerBindingResult.success(false, null);
        }
        FabricChatClefTaskOwnershipEvidence evidence = taskOwnershipReader.readOrNull();
        if (evidence == null || !evidence.available()) {
            return FabricChatClefStopControlMarkerBindingResult.failed("target_observation_failed");
        }
        Task boundRoot = evidence.rootTask();
        try {
            boolean markerBound = commandLifecycle.bindUserStop(
                    capture.context(),
                    context.request().identity(),
                    boundRoot
            );
            return FabricChatClefStopControlMarkerBindingResult.success(markerBound, boundRoot);
        } catch (Throwable error) {
            return FabricChatClefStopControlMarkerBindingResult.failed("target_observation_failed");
        }
    }

    public void clearAfterInvocationFailure(
            FabricChatClefStopControlContext context,
            FabricChatClefOrdinaryCommandStopCapture capture,
            FabricChatClefStopControlMarkerBindingResult binding
    ) {
        if (!binding.markerBound()) {
            return;
        }
        try {
            commandLifecycle.clearUserStop(capture.context(), context.request().identity());
        } catch (Throwable ignored) {
            // The already-attempted STOP remains unknown and quarantined.
        }
    }
}
