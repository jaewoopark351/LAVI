package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

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

    public Map<String, Object> runningResult() {
        return resultFactory.runningResult();
    }

    public void markFinishCallbackReceived(FabricChatClefTaskSnapshot taskAtFinish) {
        state.markFinishCallbackReceived(taskAtFinish);
    }

    public void markFinishCallbackReceived(Task taskAtFinish) {
        state.markFinishCallbackReceived(taskAtFinish);
    }

    public Map<String, Object> unknownAfterFinish(FabricChatClefTaskSnapshot taskAtFinish) {
        markFinishCallbackReceived(taskAtFinish);
        return resultFactory.unknownAfterFinish();
    }

    public Map<String, Object> failedFromCommandException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        state.markFailure(exception, taskAtFailure);
        return resultFactory.failedFromCommandException();
    }

    public Map<String, Object> failedFromDispatchException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
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

    public Map<String, Object> boundRootRelationshipData(String candidateName, Task candidateTask) {
        return state.boundRootRelationshipData(candidateName, candidateTask);
    }

    public FabricChatClefCommandTerminationObservation taskFinishedObservation() {
        return state.taskFinishedObservation();
    }

    public Map<String, Object> completedFromTaskFinished(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.completedFromTaskFinished(observation);
    }

    public Map<String, Object> completedWithoutUserTask() {
        return resultFactory.completedWithoutUserTask();
    }

    public Map<String, Object> failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.failedFromStoppedTask(observation);
    }

    public Map<String, Object> unknownFromTaskObservation(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.unknownFromTaskObservation(observation);
    }

    public Map<String, Object> unknownFromTaskIdentityMismatch(FabricChatClefCommandTerminationObservation observation) {
        return resultFactory.unknownFromTaskIdentityMismatch(observation);
    }

    public Map<String, Object> deadlineExceededResult(String message) {
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

    public Map<String, Object> duplicateTerminalData(String reason) {
        return resultFactory.duplicateTerminalData(reason);
    }

    public Map<String, Object> diagnosticData(String diagnosticReason) {
        return resultFactory.diagnosticData(diagnosticReason);
    }
}
