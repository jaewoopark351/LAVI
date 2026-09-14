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
        FabricChatClefCommandResultDataPayload runningData = data("dispatch_started");
        //#if MC == 12001
        runningData = state.gotoResultTracker().bindingData(runningData);
        //#endif
        return FabricChatClefCommandResult.running(
                request().requestId,
                "Fabric ChatClef command dispatch started.",
                FabricChatClefLifecycleEvidenceSequencePayload.of(
                        runningData,
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
        //#if MC == 12001
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindOperationRetirement.retire(state.boundRootReference(), "command_exception");
        //#endif
        return FabricChatClefCommandResult.failed(
                request().requestId,
                state.failureType() + ": " + state.failureMessage(),
                data("command_exception")
        );
    }

    FabricChatClefCommandResultPayload failedFromDispatchException() {
        //#if MC == 12001
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindOperationRetirement.retire(state.boundRootReference(), "dispatch_exception");
        //#endif
        return FabricChatClefCommandResult.failed(
                request().requestId,
                state.failureType() + ": " + state.failureMessage(),
                data("dispatch_exception")
        );
    }

    FabricChatClefCommandResultPayload completedFromTaskFinished(FabricChatClefCommandTerminationObservation observation) {
        //#if MC == 12001
//$$         //20260914_kpopmodder: FIND's exact owner projection is independent of the unchanged GOTO/general branches.
//$$         FabricChatClefCommandResultPayload findResult = new lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindResultProjector()
//$$                 .fromMatchingCompletion(request().requestId, data("matching_task_finished", observation),
//$$                         state.boundRootReference(), observation, state.userStopBound());
//$$         if (findResult != null) return findResult;
        //20260913_kpopmodder: Preserve generic classification gates, then project the bound owner's frozen result.
        FabricChatClefCommandResultPayload gotoResult = state.gotoResultTracker().matchingCompletion(
                data("matching_task_finished", observation), observation, state.userStopBound());
        if (gotoResult != null) return gotoResult;
        //#endif
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

    //#if MC == 12001
    //20260913_kpopmodder: Reuse the existing FAILED/UNKNOWN wire contract and immutable outbox commit.
    FabricChatClefCommandResultPayload failedFromRootRetirement(String reason) {
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindOperationRetirement.retire(state.boundRootReference(), reason);
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindDiagnosticRetirement.observe(state.boundRootReference(), reason);
        if (state.rootTermination().completion() != null && state.rootTermination().completion().stopStateAvailable()
                && state.rootTermination().completion().stopped()) {
            FabricChatClefCommandResultPayload miningFailure = miningToolFailure();
            if (miningFailure != null) return miningFailure;
        }
        return FabricChatClefCommandResult.failed(request().requestId,
                "ChatClef command lost its user task ownership before verified completion.",
                lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.payload.RootRetirementPayload.attach(
                        data(reason), state.rootTermination()));
    }
    FabricChatClefCommandResultPayload unknownFromRootCompletion(String reason) {
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindOperationRetirement.retire(state.boundRootReference(), reason);
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindDiagnosticRetirement.observe(state.boundRootReference(), reason);
        return FabricChatClefCommandResult.unknown(request().requestId,
                "ChatClef user task completion was observed, but the complete result handoff was unavailable.",
                lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.payload.RootRetirementPayload.attach(
                        data(reason), state.rootTermination()));
    }
    private FabricChatClefCommandResultPayload miningToolFailure() {
        var completion = state.rootTermination().completion();
        String reason = completion == null ? "" : completion.miningToolFailureReason();
        if (reason.isEmpty()) return null;
        var values = new java.util.LinkedHashMap<String, Object>(data("mining_tool_preparation_failed").toMap());
        values.put("mining_tool_failure_reason", reason);
        return FabricChatClefCommandResult.failed(request().requestId, "Mining tool preparation failed: " + reason,
                FabricChatClefCommandResultDataPayload.fromMap(values));
    }
    //#endif


    FabricChatClefCommandResultPayload failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        //#if MC == 12001
//$$         if (state.matchesBoundRootTask(observation)) lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindDiagnosticRetirement.observe(state.boundRootReference(), "matching_task_stopped");
        if (observation != null && observation.taskStopped() && state.matchesBoundRootTask(observation)) {
            FabricChatClefCommandResultPayload miningFailure = miningToolFailure();
            if (miningFailure != null) return miningFailure;
        }
        //#endif
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
        //#if MC == 12001
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindOperationRetirement.retire(state.boundRootReference(), "user_stop");
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindDiagnosticRetirement.observe(state.boundRootReference(), "user_stop");
        //#endif
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
        //#if MC == 12001
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindOperationRetirement.retire(state.boundRootReference(), "bridge_deadline_exceeded");
//$$         lavi.minecraft.fabric.chatclef.bridge.command.result.find.FabricChatClefFindDiagnosticRetirement.observe(state.boundRootReference(), "bridge_deadline_exceeded");
        //#endif
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
