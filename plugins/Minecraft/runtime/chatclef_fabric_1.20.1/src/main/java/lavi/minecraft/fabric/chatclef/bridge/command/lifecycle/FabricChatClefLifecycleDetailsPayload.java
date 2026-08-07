package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnosticDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefEmptyDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefExceptionDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefFinishCallbackDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefQueueContextMismatchDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefReplacedActiveDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefTerminalDecisionDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefTerminalResultDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefWaitingForTerminalConditionDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

//20260805_kpopmodder: Keep command lifecycle detail fields typed until the Map edge.
public interface FabricChatClefLifecycleDetailsPayload extends FabricChatClefCommandDiagnosticDetailsPayload {
    static FabricChatClefLifecycleDetailsPayload empty() {
        return new FabricChatClefEmptyDetailsPayload();
    }

    static FabricChatClefLifecycleDetailsPayload replacedActive(String replacedActiveRequestId) {
        return new FabricChatClefReplacedActiveDetailsPayload(replacedActiveRequestId);
    }

    static FabricChatClefLifecycleDetailsPayload finishCallback(
            FabricChatClefBoundRootTaskRelationshipPayload callbackCurrentTask,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        return new FabricChatClefFinishCallbackDetailsPayload(callbackCurrentTask, runtime);
    }

    static FabricChatClefLifecycleDetailsPayload terminalDecision(String decisionReason) {
        return new FabricChatClefTerminalDecisionDetailsPayload(decisionReason);
    }

    static FabricChatClefLifecycleDetailsPayload terminalResult(boolean terminalSent, boolean lifecycleCleared) {
        return new FabricChatClefTerminalResultDetailsPayload(terminalSent, lifecycleCleared);
    }

    static FabricChatClefLifecycleDetailsPayload queueContextMismatch(
            boolean queueActivePresent,
            String queueActiveRequestId
    ) {
        return new FabricChatClefQueueContextMismatchDetailsPayload(queueActivePresent, queueActiveRequestId);
    }

    static FabricChatClefLifecycleDetailsPayload waitingForTerminalCondition(
            String waitingReason,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask,
            String currentTaskBoundRootMatchReason,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        return new FabricChatClefWaitingForTerminalConditionDetailsPayload(
                waitingReason,
                currentTask,
                currentTaskBoundRootMatchReason,
                runtime
        );
    }

    static FabricChatClefLifecycleDetailsPayload exception(Throwable exception) {
        return new FabricChatClefExceptionDetailsPayload(exception);
    }
}
