package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage;

//20260905_kpopmodder: Carry one immutable STOP marker-binding outcome.

import adris.altoclef.tasksystem.Task;

public record FabricChatClefStopControlMarkerBindingResult(
        boolean succeeded,
        boolean markerBound,
        Task boundRootTask,
        String failureReason
) {
    public static FabricChatClefStopControlMarkerBindingResult success(
            boolean markerBound,
            Task boundRootTask
    ) {
        return new FabricChatClefStopControlMarkerBindingResult(true, markerBound, boundRootTask, "");
    }

    public static FabricChatClefStopControlMarkerBindingResult failed(String reason) {
        return new FabricChatClefStopControlMarkerBindingResult(false, false, null, reason);
    }
}
