package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

//20260801_kpopmodder: Keep bridge result fidelity separate from ChatClef command callbacks.
public final class FabricChatClefCommandExecution {
    private final FabricChatClefCommandExecutionState state;
    private final FabricChatClefCommandResultFactory resultFactory;

    public FabricChatClefCommandExecution(
            FabricChatClefCommandContext context,
            String normalizedCommand,
            FabricChatClefTaskSnapshot taskBeforeDispatch
    ) {
        this.state = new FabricChatClefCommandExecutionState(context, normalizedCommand, taskBeforeDispatch);
        this.resultFactory = new FabricChatClefCommandResultFactory(state);
    }

    public FabricChatClefCommandResultPayload runningResult() {
        return resultFactory.runningResult();
    }

    public void markFinishCallbackReceived(FabricChatClefTaskSnapshot taskAtFinish) {
        state.markFinishCallbackReceived(taskAtFinish);
    }

    public void markFinishCallbackReceived(Task taskAtFinish) {
        state.markFinishCallbackReceived(taskAtFinish);
    }

    public FabricChatClefCommandResultPayload unknownAfterFinish(FabricChatClefTaskSnapshot taskAtFinish) {
        markFinishCallbackReceived(taskAtFinish);
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

    public void markDispatchReturned(Task boundRootTask, FabricChatClefTaskSnapshot taskAfterDispatch) {
        state.markDispatchReturned(boundRootTask, taskAfterDispatch);
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

    public boolean hasBoundRootTask() {
        return state.hasBoundRootTask();
    }

    public boolean matchesBoundRootTask(FabricChatClefCommandTerminationObservation observation) {
        return state.matchesBoundRootTask(observation);
    }

    public boolean matchesBoundRootTask(Task candidateTask) {
        return state.matchesBoundRootTask(candidateTask);
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

    public FabricChatClefCommandResultPayload failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.failedFromStoppedTask(observation);
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

    public boolean markTerminalSent() {
        return state.context().markTerminalSent();
    }

    public String requestId() {
        return state.context().requestId();
    }

    public FabricChatClefCommandContext context() {
        return state.context();
    }

    public FabricChatClefCommandResultDataPayload duplicateTerminalPayload(String reason) {
        return resultFactory.duplicateTerminalPayload(reason);
    }

    public FabricChatClefCommandResultDataPayload diagnosticPayload(String diagnosticReason) {
        return resultFactory.diagnosticPayload(diagnosticReason);
    }
}
