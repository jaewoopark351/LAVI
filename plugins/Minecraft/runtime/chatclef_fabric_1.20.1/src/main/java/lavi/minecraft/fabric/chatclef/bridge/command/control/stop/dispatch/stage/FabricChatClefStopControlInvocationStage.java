package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.stage;

//20260905_kpopmodder: Commit and invoke the registered StopCommand exactly once.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopCommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;

public final class FabricChatClefStopControlInvocationStage {
    private final FabricChatClefStopCommandExecutor stopCommandExecutor;

    public FabricChatClefStopControlInvocationStage(FabricChatClefStopCommandExecutor stopCommandExecutor) {
        this.stopCommandExecutor = stopCommandExecutor;
    }

    public void commit(
            FabricChatClefStopControlContext context,
            FabricChatClefStopControlMarkerBindingResult binding,
            long clientTick
    ) {
        context.markExecuted(clientTick, binding.markerBound(), binding.boundRootTask());
    }

    public FabricChatClefStopControlInvocationOutcome invokeRegisteredStop() {
        try {
            stopCommandExecutor.executeRegisteredStop();
            return FabricChatClefStopControlInvocationOutcome.success();
        } catch (Throwable error) {
            return FabricChatClefStopControlInvocationOutcome.failed("stop_command_exception");
        }
    }
}
