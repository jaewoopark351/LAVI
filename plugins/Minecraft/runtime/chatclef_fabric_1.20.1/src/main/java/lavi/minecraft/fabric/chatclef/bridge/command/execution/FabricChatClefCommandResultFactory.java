package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandDeadlinePayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultFidelity;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefNonterminalLifecycleEvidencePayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

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

    FabricChatClefCommandResultPayload runningLifecycleEvidenceResult(
            String stage,
            int evidenceSequence,
            String waitingReason,
            FabricChatClefTaskSnapshot currentRootTask,
            FabricChatClefStableRequestQuiescenceObservation quiescence
    ) {
        return FabricChatClefCommandResult.running(
                request().requestId,
                "Fabric ChatClef command lifecycle evidence observed.",
                FabricChatClefNonterminalLifecycleEvidencePayload.of(
                        data(stage),
                        stage,
                        evidenceSequence,
                        waitingReason,
                        currentRootTask,
                        quiescence
                )
        );
    }

    FabricChatClefCommandResultPayload unknownAfterFinish() {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "Fabric ChatClef command callback finished, but Minecraft goal success was not verified.",
                data("finish_callback_without_verified_success")
        );
    }

    FabricChatClefCommandResultPayload unknownFromPreexistingUnchangedIdleRoot() {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "Fabric ChatClef command callback finished, but Minecraft goal success was not verified.",
                data("finish_callback_without_new_command_owned_root")
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
        return FabricChatClefCommandResult.deadlineExceeded(
                request().requestId,
                message,
                FabricChatClefCommandDeadlinePayload.markTaskMayStillBeRunning(data("deadline_exceeded"))
        );
    }

    FabricChatClefCommandResultDataPayload duplicateTerminalPayload(String reason) {
        return data(reason);
    }

    FabricChatClefCommandResultDataPayload diagnosticPayload(String diagnosticReason) {
        return FabricChatClefCommandDiagnosticPayload.diagnosticData(
                diagnosticReason,
                request(),
                state.normalizedCommand(),
                state.elapsedMs(),
                data(diagnosticReason)
        );
    }

    private FabricChatClefCommandResultDataPayload data(String resultReason) {
        return data(resultReason, state.taskFinishedObservation());
    }

    private FabricChatClefCommandResultDataPayload data(
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
                fidelityFor(resultReason),
                observation
        );
    }

    private FabricChatClefCommandResultFidelity fidelityFor(String resultReason) {
        return switch (resultReason) {
            case "dispatch_started" -> FabricChatClefCommandResultFidelity.DISPATCH_STARTED_ONLY;
            case "finish_callback_without_new_command_owned_root",
                 "finish_callback_without_verified_success" ->
                    FabricChatClefCommandResultFidelity.CALLBACK_WITHOUT_MATCHING_USER_TASK_EVENT;
            case "callback_completed_without_user_task" ->
                    FabricChatClefCommandResultFidelity.CALLBACK_WITHOUT_USER_TASK;
            case "matching_task_finished",
                 "matching_task_stopped",
                 "task_observation_unclassified" ->
                    FabricChatClefCommandResultFidelity.CALLBACK_PLUS_MATCHING_USER_TASK_EVENT;
            case "task_identity_mismatch" ->
                    FabricChatClefCommandResultFidelity.CALLBACK_PLUS_NONMATCHING_USER_TASK_EVENT;
            case "command_exception" -> FabricChatClefCommandResultFidelity.COMMAND_EXCEPTION_OBSERVED;
            case "dispatch_exception" -> FabricChatClefCommandResultFidelity.DISPATCH_EXCEPTION_OBSERVED;
            case "deadline_exceeded" -> FabricChatClefCommandResultFidelity.DEADLINE_WITHOUT_VERIFIED_TERMINAL;
            default -> FabricChatClefCommandResultFidelity.UNKNOWN;
        };
    }

    private FabricChatClefCommandRequest request() {
        return state.request();
    }
}
