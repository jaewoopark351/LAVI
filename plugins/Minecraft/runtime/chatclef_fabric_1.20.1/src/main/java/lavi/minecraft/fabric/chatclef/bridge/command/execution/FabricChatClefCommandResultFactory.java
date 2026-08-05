package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandDeadlinePayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.Map;

//20260804_kpopmodder: Separate command result map construction from mutable command execution state.
final class FabricChatClefCommandResultFactory {
    private final FabricChatClefCommandExecutionState state;

    FabricChatClefCommandResultFactory(FabricChatClefCommandExecutionState state) {
        this.state = state;
    }

    FabricChatClefCommandResultPayload runningResult() {
        return FabricChatClefCommandResult.running(
                request().requestId,
                "Fabric ChatClef command dispatch started.",
                data("dispatch_started")
        );
    }

    FabricChatClefCommandResultPayload unknownAfterFinish() {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "Fabric ChatClef command callback finished, but Minecraft goal success was not verified.",
                data("finish_callback_without_verified_success")
        );
    }

    FabricChatClefCommandResultPayload failedFromCommandException() {
        return FabricChatClefCommandResult.failed(
                request().requestId,
                state.failureType() + ": " + state.failureMessage(),
                data("command_exception")
        );
    }

    FabricChatClefCommandResultPayload failedFromDispatchException() {
        return FabricChatClefCommandResult.failed(
                request().requestId,
                state.failureType() + ": " + state.failureMessage(),
                data("dispatch_exception")
        );
    }

    FabricChatClefCommandResultPayload completedFromTaskFinished(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.completed(
                request().requestId,
                "ChatClef user task reached natural completion.",
                data("matching_task_finished", observation)
        );
    }

    FabricChatClefCommandResultPayload completedWithoutUserTask() {
        return FabricChatClefCommandResult.completed(
                request().requestId,
                "ChatClef command completed without starting a user task.",
                data("callback_completed_without_user_task")
        );
    }

    FabricChatClefCommandResultPayload failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.failed(
                request().requestId,
                "ChatClef user task stopped before natural completion.",
                data("matching_task_stopped", observation)
        );
    }

    FabricChatClefCommandResultPayload unknownFromTaskObservation(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "ChatClef user task completion could not be safely classified.",
                data("task_observation_unclassified", observation)
        );
    }

    FabricChatClefCommandResultPayload unknownFromTaskIdentityMismatch(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "ChatClef user task finished, but it did not match the command root task.",
                data("task_identity_mismatch", observation)
        );
    }

    FabricChatClefCommandResultPayload deadlineExceededResult(String message) {
        Map<String, Object> payload = data("deadline_exceeded");
        FabricChatClefCommandDeadlinePayload.markTaskMayStillBeRunning(payload);
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
                state.normalizedCommand(),
                state.elapsedMs(),
                data(diagnosticReason)
        );
    }

    private Map<String, Object> data(String resultReason) {
        return data(resultReason, state.taskFinishedObservation());
    }

    private Map<String, Object> data(
            String resultReason,
            FabricChatClefCommandTerminationObservation observation
    ) {
        return FabricChatClefCommandDiagnosticPayload.commandData(
                resultReason,
                state.dispatchStartedMs(),
                state.dispatchReturned(),
                state.dispatchThreadName(),
                state.normalizedCommand(),
                state.finishCallbackReceived(),
                state.failureType(),
                state.failureMessage(),
                state.context(),
                state.taskBeforeDispatch(),
                state.taskAfterDispatch(),
                state.terminalTask(),
                state.boundRootTask(),
                observation
        );
    }

    private FabricChatClefCommandRequest request() {
        return state.request();
    }
}
