package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;

import java.util.Map;

//20260804_kpopmodder: Separate command result map construction from mutable command execution state.
final class FabricChatClefCommandResultFactory {
    private final FabricChatClefCommandExecution execution;

    FabricChatClefCommandResultFactory(FabricChatClefCommandExecution execution) {
        this.execution = execution;
    }

    Map<String, Object> runningResult() {
        return FabricChatClefCommandResult.running(
                request().requestId,
                "Fabric ChatClef command dispatch started.",
                data("dispatch_started")
        );
    }

    Map<String, Object> unknownAfterFinish() {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "Fabric ChatClef command callback finished, but Minecraft goal success was not verified.",
                data("finish_callback_without_verified_success")
        );
    }

    Map<String, Object> failedFromCommandException() {
        return FabricChatClefCommandResult.failed(
                request().requestId,
                execution.failureTypeForResult() + ": " + execution.failureMessageForResult(),
                data("command_exception")
        );
    }

    Map<String, Object> failedFromDispatchException() {
        return FabricChatClefCommandResult.failed(
                request().requestId,
                execution.failureTypeForResult() + ": " + execution.failureMessageForResult(),
                data("dispatch_exception")
        );
    }

    Map<String, Object> completedFromTaskFinished(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.completed(
                request().requestId,
                "ChatClef user task reached natural completion.",
                data("matching_task_finished", observation)
        );
    }

    Map<String, Object> completedWithoutUserTask() {
        return FabricChatClefCommandResult.completed(
                request().requestId,
                "ChatClef command completed without starting a user task.",
                data("callback_completed_without_user_task")
        );
    }

    Map<String, Object> failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.failed(
                request().requestId,
                "ChatClef user task stopped before natural completion.",
                data("matching_task_stopped", observation)
        );
    }

    Map<String, Object> unknownFromTaskObservation(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "ChatClef user task completion could not be safely classified.",
                data("task_observation_unclassified", observation)
        );
    }

    Map<String, Object> unknownFromTaskIdentityMismatch(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "ChatClef user task finished, but it did not match the command root task.",
                data("task_identity_mismatch", observation)
        );
    }

    Map<String, Object> deadlineExceededResult(String message) {
        Map<String, Object> payload = data("deadline_exceeded");
        payload.put("automation_cancelled", false);
        payload.put("task_may_still_be_running", true);
        payload.put("late_terminal_event_will_be_ignored", true);
        return FabricChatClefCommandResult.deadlineExceeded(
                request().requestId,
                message,
                payload
        );
    }

    Map<String, Object> duplicateTerminalData(String reason) {
        return data(reason);
    }

    Map<String, Object> diagnosticData(String diagnosticReason) {
        return FabricChatClefCommandDiagnosticPayload.diagnosticData(
                diagnosticReason,
                request(),
                execution.normalizedCommandForResult(),
                execution.elapsedMsForResult(),
                data(diagnosticReason)
        );
    }

    private Map<String, Object> data(String resultReason) {
        return data(resultReason, execution.taskFinishedObservationForResult());
    }

    private Map<String, Object> data(
            String resultReason,
            FabricChatClefCommandTerminationObservation observation
    ) {
        return FabricChatClefCommandDiagnosticPayload.commandData(
                resultReason,
                execution.dispatchStartedMsForResult(),
                execution.dispatchReturnedForResult(),
                execution.dispatchThreadNameForResult(),
                execution.normalizedCommandForResult(),
                execution.finishCallbackReceivedForResult(),
                execution.failureTypeForResult(),
                execution.failureMessageForResult(),
                execution.contextForResult(),
                execution.taskBeforeDispatchForResult(),
                execution.taskAfterDispatchForResult(),
                execution.terminalTaskForResult(),
                execution.boundRootTaskForResult(),
                observation
        );
    }

    private FabricChatClefCommandRequest request() {
        return execution.requestForResult();
    }
}
