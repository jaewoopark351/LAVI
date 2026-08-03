package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Keep bridge result fidelity separate from ChatClef command callbacks.
public final class FabricChatClefCommandExecution {
    private final FabricChatClefCommandContext context;
    private final FabricChatClefCommandRequest request;
    private final FabricChatClefCommandResultFactory resultFactory;
    private final String normalizedCommand;
    private final long dispatchStartedMs;
    private final String dispatchThreadName;
    private final FabricChatClefTaskSnapshot taskBeforeDispatch;
    private volatile boolean dispatchReturned;
    private volatile boolean finishCallbackReceived;
    private volatile String failureType = "";
    private volatile String failureMessage = "";
    private volatile Task boundRootTask;
    private volatile FabricChatClefTaskSnapshot taskAfterDispatch;
    private volatile FabricChatClefTaskSnapshot terminalTask;
    private volatile FabricChatClefCommandTerminationObservation taskFinishedObservation;

    public FabricChatClefCommandExecution(
            FabricChatClefCommandContext context,
            String normalizedCommand,
            FabricChatClefTaskSnapshot taskBeforeDispatch
    ) {
        this.context = context;
        this.request = context.request();
        this.normalizedCommand = normalizedCommand;
        this.taskBeforeDispatch = taskBeforeDispatch;
        this.dispatchStartedMs = System.currentTimeMillis();
        this.dispatchThreadName = Thread.currentThread().getName();
        this.taskAfterDispatch = FabricChatClefTaskSnapshot.capture(null);
        this.terminalTask = FabricChatClefTaskSnapshot.capture(null);
        this.resultFactory = new FabricChatClefCommandResultFactory(this);
    }

    public Map<String, Object> runningResult() {
        return resultFactory.runningResult();
    }

    public void markFinishCallbackReceived(FabricChatClefTaskSnapshot taskAtFinish) {
        finishCallbackReceived = true;
        terminalTask = taskAtFinish;
    }

    public void markFinishCallbackReceived(Task taskAtFinish) {
        finishCallbackReceived = true;
        terminalTask = FabricChatClefTaskSnapshot.capture(taskAtFinish);
    }

    public Map<String, Object> unknownAfterFinish(FabricChatClefTaskSnapshot taskAtFinish) {
        markFinishCallbackReceived(taskAtFinish);
        return resultFactory.unknownAfterFinish();
    }

    public Map<String, Object> failedFromCommandException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        failureType = exception.getClass().getSimpleName();
        failureMessage = nullSafeMessage(exception);
        terminalTask = taskAtFailure;
        return resultFactory.failedFromCommandException();
    }

    public Map<String, Object> failedFromDispatchException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        failureType = exception.getClass().getSimpleName();
        failureMessage = nullSafeMessage(exception);
        terminalTask = taskAtFailure;
        return resultFactory.failedFromDispatchException();
    }

    public void markDispatchReturned(Task boundRootTask, FabricChatClefTaskSnapshot taskAfterDispatch) {
        dispatchReturned = true;
        this.boundRootTask = boundRootTask;
        this.taskAfterDispatch = taskAfterDispatch;
    }

    public void markTaskFinishedObservation(FabricChatClefCommandTerminationObservation observation) {
        taskFinishedObservation = observation;
        if (observation != null) {
            terminalTask = FabricChatClefTaskSnapshot.capture(observation.task());
        }
    }

    public boolean dispatchReturned() {
        return dispatchReturned;
    }

    public boolean finishCallbackReceived() {
        return finishCallbackReceived;
    }

    public boolean hasBoundRootTask() {
        return boundRootTask != null;
    }

    public boolean matchesBoundRootTask(FabricChatClefCommandTerminationObservation observation) {
        return observation != null
                && observation.taskPresent()
                && boundRootTask != null
                && observation.task() == boundRootTask;
    }

    public boolean matchesBoundRootTask(Task candidateTask) {
        return candidateTask != null
                && boundRootTask != null
                && candidateTask == boundRootTask;
    }

    public String boundRootMatchReason(Task candidateTask) {
        if (boundRootTask == null) {
            return "no_bound_root_task";
        }
        if (candidateTask == null) {
            return "candidate_task_absent";
        }
        if (candidateTask == boundRootTask) {
            return "same_task_instance";
        }
        if (candidateTask.getClass() == boundRootTask.getClass()) {
            return "same_task_class_different_instance";
        }
        return "different_task_class";
    }

    public Map<String, Object> boundRootRelationshipData(String candidateName, Task candidateTask) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(candidateName, FabricChatClefTaskSnapshot.capture(candidateTask).toMap());
        payload.put(candidateName + "_matches_bound_root_task", matchesBoundRootTask(candidateTask));
        payload.put(candidateName + "_bound_root_match_reason", boundRootMatchReason(candidateTask));
        payload.put("bound_root_task", FabricChatClefTaskSnapshot.capture(boundRootTask).toMap());
        return payload;
    }

    public FabricChatClefCommandTerminationObservation taskFinishedObservation() {
        return taskFinishedObservation;
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
        return context.markTerminalSent();
    }

    public String requestId() {
        return context.requestId();
    }

    public FabricChatClefCommandContext context() {
        return context;
    }

    public Map<String, Object> duplicateTerminalData(String reason) {
        return resultFactory.duplicateTerminalData(reason);
    }

    public Map<String, Object> diagnosticData(String diagnosticReason) {
        return resultFactory.diagnosticData(diagnosticReason);
    }

    FabricChatClefCommandRequest requestForResult() {
        return request;
    }

    String normalizedCommandForResult() {
        return normalizedCommand;
    }

    long dispatchStartedMsForResult() {
        return dispatchStartedMs;
    }

    boolean dispatchReturnedForResult() {
        return dispatchReturned;
    }

    String dispatchThreadNameForResult() {
        return dispatchThreadName;
    }

    boolean finishCallbackReceivedForResult() {
        return finishCallbackReceived;
    }

    String failureTypeForResult() {
        return failureType;
    }

    String failureMessageForResult() {
        return failureMessage;
    }

    FabricChatClefCommandContext contextForResult() {
        return context;
    }

    FabricChatClefTaskSnapshot taskBeforeDispatchForResult() {
        return taskBeforeDispatch;
    }

    FabricChatClefTaskSnapshot taskAfterDispatchForResult() {
        return taskAfterDispatch;
    }

    FabricChatClefTaskSnapshot terminalTaskForResult() {
        return terminalTask;
    }

    FabricChatClefTaskSnapshot boundRootTaskForResult() {
        return FabricChatClefTaskSnapshot.capture(boundRootTask);
    }

    FabricChatClefCommandTerminationObservation taskFinishedObservationForResult() {
        return taskFinishedObservation;
    }

    long elapsedMsForResult() {
        return System.currentTimeMillis() - dispatchStartedMs;
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
