package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefFinishCallbackObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefCommandEffectTracker;

import java.util.function.Function;

//20260801_kpopmodder: Keep bridge result fidelity separate from ChatClef command callbacks.
public final class FabricChatClefCommandExecution {
    private final FabricChatClefCommandExecutionState state;
    private final FabricChatClefCommandResultFactory resultFactory;

    public FabricChatClefCommandExecution(
            FabricChatClefCommandContext context,
            String normalizedCommand,
            FabricChatClefTaskOwnershipEvidence taskBeforeDispatchEvidence
    ) {
        this.state = new FabricChatClefCommandExecutionState(context, normalizedCommand, taskBeforeDispatchEvidence);
        this.resultFactory = new FabricChatClefCommandResultFactory(state);
    }

    FabricChatClefCommandExecution(
            FabricChatClefCommandContext context,
            String normalizedCommand,
            FabricChatClefTaskOwnershipEvidence taskBeforeDispatchEvidence,
            Function<String, FabricChatClefCommandEffectTracker> effectTrackerFactory
    ) {
        this.state = new FabricChatClefCommandExecutionState(
                context,
                normalizedCommand,
                taskBeforeDispatchEvidence,
                effectTrackerFactory
        );
        this.resultFactory = new FabricChatClefCommandResultFactory(state);
    }

    public FabricChatClefCommandResultPayload runningResult() {
        return resultFactory.runningResult();
    }

    public int nextLifecycleEvidenceSequence() {
        return state.nextLifecycleEvidenceSequence();
    }

    public FabricChatClefCommandResultPayload runningLifecycleEvidenceResult(
            String stage,
            int evidenceSequence,
            String waitingReason,
            FabricChatClefTaskSnapshot currentRootTask,
            FabricChatClefStableRequestQuiescenceObservation quiescence
    ) {
        return resultFactory.runningLifecycleEvidenceResult(
                stage,
                evidenceSequence,
                waitingReason,
                currentRootTask,
                quiescence
        );
    }

    public void markFinishCallbackReceived(Task taskAtFinish) {
        state.markFinishCallbackReceived(taskAtFinish);
    }

    public FabricChatClefCommandResultPayload unknownAfterFinish(FabricChatClefTaskSnapshot taskAtFinish) {
        return resultFactory.unknownAfterFinish();
    }

    public FabricChatClefCommandResultPayload failedFromCommandException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        state.markFailure(exception, taskAtFailure);
        return resultFactory.failedFromCommandException();
    }

    public FabricChatClefCommandResultPayload failedFromDispatchException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        state.markFailure(exception, taskAtFailure);
        return resultFactory.failedFromDispatchException();
    }

    public void openExecutorExecuteInvocation() {
        state.openExecutorExecuteInvocation();
    }

    public void closeExecutorExecuteInvocation() {
        state.closeExecutorExecuteInvocation();
    }

    public void markDispatchReturned(
            FabricChatClefTaskOwnershipEvidence taskAfterDispatchEvidence,
            FabricChatClefRootOwnershipClassification rootOwnershipClassification
    ) {
        state.markDispatchReturned(taskAfterDispatchEvidence, rootOwnershipClassification);
    }

    public void markTaskFinishedObservation(FabricChatClefCommandTerminationObservation observation) {
        state.markTaskFinishedObservation(observation);
    }

    public boolean dispatchReturned() {
        return state.dispatchReturned();
    }

    public boolean finishCallbackReceived() {
        return state.finishCallbackReceived();
    }

    public long finishCallbackReceivedAtMs() {
        return state.finishCallbackReceivedAtMs();
    }

    public FabricChatClefFinishCallbackObservation firstFinishCallbackObservation() {
        return state.firstFinishCallbackObservation();
    }

    public int finishCallbackDuplicateCount() {
        return state.finishCallbackDuplicateCount();
    }

    public boolean finishCallbackFirstObservedBeforeDispatchReturn() {
        return state.finishCallbackFirstObservedBeforeDispatchReturn();
    }

    public boolean hasBoundRootTask() {
        return state.hasBoundRootTask();
    }

    public boolean matchesBoundRootTask(FabricChatClefCommandTerminationObservation observation) {
        return state.matchesBoundRootTask(observation);
    }

    public boolean matchesBoundRootTask(Task candidateTask) {
        return state.matchesBoundRootTask(candidateTask);
    }

    public boolean bindUserStop(FabricChatClefStopControlIdentity identity, Task candidateTask) {
        return state.bindUserStop(identity, candidateTask);
    }

    public boolean clearUserStop(FabricChatClefStopControlIdentity identity) {
        return state.clearUserStop(identity);
    }

    public boolean userStopBound() {
        return state.userStopBound();
    }

    public String boundRootMatchReason(Task candidateTask) {
        return state.boundRootMatchReason(candidateTask);
    }

    public FabricChatClefBoundRootTaskRelationshipPayload boundRootRelationshipPayload(
            String candidateName,
            Task candidateTask
    ) {
        return state.boundRootRelationshipPayload(candidateName, candidateTask);
    }

    public FabricChatClefCommandTerminationObservation taskFinishedObservation() {
        return state.taskFinishedObservation();
    }

    public FabricChatClefCommandResultPayload completedFromTaskFinished(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.completedFromTaskFinished(observation);
    }

    public FabricChatClefCommandResultPayload completedWithoutUserTask() {
        return resultFactory.completedWithoutUserTask();
    }

    public FabricChatClefCommandResultPayload unknownFromPreexistingUnchangedIdleRoot() {
        return resultFactory.unknownFromPreexistingUnchangedIdleRoot();
    }

    public FabricChatClefCommandResultPayload failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.failedFromStoppedTask(observation);
    }

    public FabricChatClefCommandResultPayload cancelledFromUserStop() {
        return resultFactory.cancelledFromUserStop();
    }

    public FabricChatClefCommandResultPayload unknownFromTaskObservation(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.unknownFromTaskObservation(observation);
    }

    public FabricChatClefCommandResultPayload unknownFromTaskIdentityMismatch(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.unknownFromTaskIdentityMismatch(observation);
    }

    public FabricChatClefCommandResultPayload deadlineExceededResult(String message) {
        return resultFactory.deadlineExceededResult(message);
    }

    public String requestId() {
        return state.context().requestId();
    }

    public String normalizedCommand() {
        return state.normalizedCommand();
    }

    public FabricChatClefCommandContext context() {
        return state.context();
    }

    public FabricChatClefTaskOwnershipEvidence taskBeforeDispatchEvidence() {
        return state.taskBeforeDispatchEvidence();
    }

    public FabricChatClefTaskOwnershipEvidence taskAfterDispatchEvidence() {
        return state.taskAfterDispatchEvidence();
    }

    public FabricChatClefRootOwnershipClassification rootOwnershipClassification() {
        return state.rootOwnershipClassification();
    }

    public void markPreexistingIdleRootStabilityObservation(
            FabricChatClefStableRequestQuiescenceObservation observation
    ) {
        state.markPreexistingIdleRootStabilityObservation(observation);
    }

    public FabricChatClefStableRequestQuiescenceObservation preexistingIdleRootStabilityObservation() {
        return state.preexistingIdleRootStabilityObservation();
    }

    public boolean preexistingIdleRootStabilityQualified() {
        return state.preexistingIdleRootStabilityQualified();
    }

    public FabricChatClefCommandResultDataPayload duplicateTerminalPayload(String reason) {
        return resultFactory.duplicateTerminalPayload(reason);
    }

    public FabricChatClefCommandResultDataPayload diagnosticPayload(String diagnosticReason) {
        return resultFactory.diagnosticPayload(diagnosticReason);
    }
}
