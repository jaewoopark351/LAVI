package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefOriginalCancellationResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandDeadlinePayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultFidelity;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefLifecycleEvidenceSequencePayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefNonterminalLifecycleEvidencePayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.storehome.FabricChatClefStoreHomeResultProjector;

//20260804_kpopmodder: Separate command result map construction from mutable command execution state.
//20260827_kpopmodder: Attach typed STORE_HOME outcomes without changing generic lifecycle classification.
final class FabricChatClefCommandResultFactory {
    private final FabricChatClefCommandExecutionState state;
    private final FabricChatClefStoreHomeResultProjector storeHomeResultProjector;
    private final FabricChatClefOriginalCancellationResultFactory originalCancellationResultFactory;

    FabricChatClefCommandResultFactory(FabricChatClefCommandExecutionState state) {
        this.state = state;
        this.storeHomeResultProjector = new FabricChatClefStoreHomeResultProjector();
        this.originalCancellationResultFactory = new FabricChatClefOriginalCancellationResultFactory();
    }

    FabricChatClefCommandResultPayload runningResult() {
        return FabricChatClefCommandResult.running(
                request().requestId,
                "Fabric ChatClef command dispatch started.",
                FabricChatClefLifecycleEvidenceSequencePayload.of(
                        data("dispatch_started"),
                        state.initialLifecycleEvidenceSequence()
                )
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
                storeHomeResultProjector.fromCommandWithoutUserTask(
                        data("finish_callback_without_new_command_owned_root"),
                        state.normalizedCommand()
                )
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
                //20260907_kpopmodder: Project family-specific read-only evidence only after matching Task completion.
                state.commandEffectTracker().fromMatchingCompletion(
                        storeHomeResultProjector.fromMatchingTask(
                                data("matching_task_finished", observation),
                                state.normalizedCommand(),
                                observation
                        )
                )
        );
    }

    FabricChatClefCommandResultPayload completedWithoutUserTask() {
        return FabricChatClefCommandResult.completed(
                request().requestId,
                "ChatClef command completed without starting a user task.",
                storeHomeResultProjector.fromCommandWithoutUserTask(
                        data("callback_completed_without_user_task"),
                        state.normalizedCommand()
                )
        );
    }

    FabricChatClefCommandResultPayload failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.failed(
                request().requestId,
                "ChatClef user task stopped before natural completion.",
                storeHomeResultProjector.fromMatchingTask(
                        data("matching_task_stopped", observation),
                        state.normalizedCommand(),
                        observation
                )
        );
    }

    FabricChatClefCommandResultPayload cancelledFromUserStop() {
        return originalCancellationResultFactory.create(state.context());
    }

    FabricChatClefCommandResultPayload unknownFromTaskObservation(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.unknown(
                request().requestId,
                "ChatClef user task completion could not be safely classified.",
                storeHomeResultProjector.fromMatchingTask(
                        data("task_observation_unclassified", observation),
                        state.normalizedCommand(),
                        observation
                )
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
                data(diagnosticReason, FabricChatClefCommandResultFidelity.UNKNOWN)
        );
    }

    private FabricChatClefCommandResultDataPayload data(String resultReason) {
        return data(resultReason, state.taskFinishedObservation());
    }

    private FabricChatClefCommandResultDataPayload data(
            String resultReason,
            FabricChatClefCommandTerminationObservation observation
    ) {
        return data(resultReason, fidelityFor(resultReason), observation);
    }

    private FabricChatClefCommandResultDataPayload data(
            String resultReason,
            FabricChatClefCommandResultFidelity resultFidelity
    ) {
        return data(resultReason, resultFidelity, state.taskFinishedObservation());
    }

    private FabricChatClefCommandResultDataPayload data(
            String resultReason,
            FabricChatClefCommandResultFidelity resultFidelity,
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
                resultFidelity,
                observation
        );
    }

    private FabricChatClefCommandResultFidelity fidelityFor(String resultReason) {
        return switch (resultReason) {
            case "dispatch_started" -> FabricChatClefCommandResultFidelity.DISPATCH_STARTED_ONLY;
            case "finish_callback_without_new_command_owned_root",
                 "finish_callback_without_verified_success",
                 "finish_callback_observed_nonterminal",
                 "stable_request_quiescence_observed" ->
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
