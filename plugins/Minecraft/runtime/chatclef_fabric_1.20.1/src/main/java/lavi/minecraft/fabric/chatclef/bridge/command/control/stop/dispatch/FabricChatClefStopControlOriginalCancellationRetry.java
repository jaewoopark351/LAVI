package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Retry only the captured ordinary command's committed STOP cancellation result.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

public final class FabricChatClefStopControlOriginalCancellationRetry {
    private final FabricChatClefStopControlCommandLifecycle commandLifecycle;

    public FabricChatClefStopControlOriginalCancellationRetry(
            FabricChatClefStopControlCommandLifecycle commandLifecycle
    ) {
        this.commandLifecycle = commandLifecycle;
    }

    public String failureReason(FabricChatClefCommandContext ordinaryContext, long nowMs) {
        if (ordinaryContext.terminalSendRetryExhausted()) {
            return "original_cancel_send_failed";
        }
        if (!ordinaryContext.terminalSendReady(nowMs)) {
            return "";
        }
        try {
            commandLifecycle.sendUserStopCancellation(ordinaryContext);
            return "";
        } catch (Throwable error) {
            return "original_cancel_result_submission_failed";
        }
    }
}
