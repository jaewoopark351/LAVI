package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260804_kpopmodder: Keep mutable ChatClef command execution state out of result orchestration.
final class FabricChatClefCommandExecutionState {
    private final FabricChatClefCommandContext context;
    private final FabricChatClefCommandRequest request;
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

    FabricChatClefCommandExecutionState(
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
    }

    void markFinishCallbackReceived(FabricChatClefTaskSnapshot taskAtFinish) {
        finishCallbackReceived = true;
        terminalTask = taskAtFinish;
    }

    void markFinishCallbackReceived(Task taskAtFinish) {
        finishCallbackReceived = true;
        terminalTask = FabricChatClefTaskSnapshot.capture(taskAtFinish);
    }

    void markFailure(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        failureType = exception.getClass().getSimpleName();
        failureMessage = nullSafeMessage(exception);
        terminalTask = taskAtFailure;
    }

    void markDispatchReturned(Task boundRootTask, FabricChatClefTaskSnapshot taskAfterDispatch) {
        dispatchReturned = true;
        this.boundRootTask = boundRootTask;
        this.taskAfterDispatch = taskAfterDispatch;
    }

    void markTaskFinishedObservation(FabricChatClefCommandTerminationObservation observation) {
        taskFinishedObservation = observation;
        if (observation != null) {
            terminalTask = FabricChatClefTaskSnapshot.capture(observation.task());
        }
    }

    boolean dispatchReturned() {
        return dispatchReturned;
    }

    boolean finishCallbackReceived() {
        return finishCallbackReceived;
    }

    boolean hasBoundRootTask() {
        return boundRootTask != null;
    }

    boolean matchesBoundRootTask(FabricChatClefCommandTerminationObservation observation) {
        return observation != null
                && observation.taskPresent()
                && boundRootTask != null
                && observation.task() == boundRootTask;
    }

    boolean matchesBoundRootTask(Task candidateTask) {
        return candidateTask != null
                && boundRootTask != null
                && candidateTask == boundRootTask;
    }

    String boundRootMatchReason(Task candidateTask) {
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

    Map<String, Object> boundRootRelationshipData(String candidateName, Task candidateTask) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(candidateName, FabricChatClefTaskSnapshot.capture(candidateTask).toMap());
        payload.put(candidateName + "_matches_bound_root_task", matchesBoundRootTask(candidateTask));
        payload.put(candidateName + "_bound_root_match_reason", boundRootMatchReason(candidateTask));
        payload.put("bound_root_task", FabricChatClefTaskSnapshot.capture(boundRootTask).toMap());
        return payload;
    }

    FabricChatClefCommandTerminationObservation taskFinishedObservation() {
        return taskFinishedObservation;
    }

    FabricChatClefCommandRequest request() {
        return request;
    }

    String normalizedCommand() {
        return normalizedCommand;
    }

    long dispatchStartedMs() {
        return dispatchStartedMs;
    }

    String dispatchThreadName() {
        return dispatchThreadName;
    }

    String failureType() {
        return failureType;
    }

    String failureMessage() {
        return failureMessage;
    }

    FabricChatClefCommandContext context() {
        return context;
    }

    FabricChatClefTaskSnapshot taskBeforeDispatch() {
        return taskBeforeDispatch;
    }

    FabricChatClefTaskSnapshot taskAfterDispatch() {
        return taskAfterDispatch;
    }

    FabricChatClefTaskSnapshot terminalTask() {
        return terminalTask;
    }

    FabricChatClefTaskSnapshot boundRootTask() {
        return FabricChatClefTaskSnapshot.capture(boundRootTask);
    }

    long elapsedMs() {
        return System.currentTimeMillis() - dispatchStartedMs;
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
