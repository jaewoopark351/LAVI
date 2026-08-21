package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefStableRequestQuiescenceObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefFinishCallbackObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

//20260804_kpopmodder: Keep mutable ChatClef command execution state out of result orchestration.
final class FabricChatClefCommandExecutionState {
    private final FabricChatClefCommandContext context;
    private final FabricChatClefCommandRequest request;
    private final String normalizedCommand;
    private final long dispatchStartedMs;
    private final String dispatchThreadName;
    private final FabricChatClefTaskOwnershipEvidence taskBeforeDispatchEvidence;
    private final FabricChatClefTaskSnapshot taskBeforeDispatch;
    private volatile boolean executorExecuteInvocationOpen;
    private volatile boolean dispatchReturned;
    private volatile boolean finishCallbackReceived;
    private volatile long finishCallbackReceivedAtMs;
    private volatile FabricChatClefFinishCallbackObservation firstFinishCallbackObservation;
    private volatile int finishCallbackDuplicateCount;
    private volatile String failureType = "";
    private volatile String failureMessage = "";
    private volatile Task boundRootTask;
    private volatile FabricChatClefTaskOwnershipEvidence taskAfterDispatchEvidence;
    private volatile FabricChatClefRootOwnershipClassification rootOwnershipClassification =
            FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN;
    private volatile FabricChatClefStableRequestQuiescenceObservation preexistingIdleRootStabilityObservation;
    private volatile FabricChatClefTaskSnapshot taskAfterDispatch;
    private volatile FabricChatClefTaskSnapshot terminalTask;
    private volatile FabricChatClefCommandTerminationObservation taskFinishedObservation;

    FabricChatClefCommandExecutionState(
            FabricChatClefCommandContext context,
            String normalizedCommand,
            FabricChatClefTaskOwnershipEvidence taskBeforeDispatchEvidence
    ) {
        this.context = context;
        this.request = context.request();
        this.normalizedCommand = normalizedCommand;
        this.taskBeforeDispatchEvidence = taskBeforeDispatchEvidence == null
                ? FabricChatClefTaskOwnershipEvidence.empty()
                : taskBeforeDispatchEvidence;
        this.taskBeforeDispatch = this.taskBeforeDispatchEvidence.rootTaskSnapshot();
        this.dispatchStartedMs = System.currentTimeMillis();
        this.dispatchThreadName = Thread.currentThread().getName();
        this.taskAfterDispatchEvidence = FabricChatClefTaskOwnershipEvidence.empty();
        this.taskAfterDispatch = FabricChatClefTaskSnapshot.capture(null);
        this.terminalTask = FabricChatClefTaskSnapshot.capture(null);
    }

    synchronized void openExecutorExecuteInvocation() {
        executorExecuteInvocationOpen = true;
    }

    synchronized void closeExecutorExecuteInvocation() {
        executorExecuteInvocationOpen = false;
    }

    synchronized void markFinishCallbackReceived(Task taskAtFinish) {
        if (finishCallbackReceived) {
            finishCallbackDuplicateCount++;
            return;
        }
        FabricChatClefFinishCallbackObservation observation =
                FabricChatClefFinishCallbackObservation.capture(taskAtFinish, executorExecuteInvocationOpen);
        firstFinishCallbackObservation = observation;
        finishCallbackReceived = true;
        if (finishCallbackReceivedAtMs == 0L) {
            finishCallbackReceivedAtMs = System.currentTimeMillis();
        }
        terminalTask = observation.taskSnapshot();
    }

    void markFailure(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        failureType = exception.getClass().getSimpleName();
        failureMessage = nullSafeMessage(exception);
        terminalTask = taskAtFailure;
    }

    void markDispatchReturned(
            FabricChatClefTaskOwnershipEvidence taskAfterDispatchEvidence,
            FabricChatClefRootOwnershipClassification rootOwnershipClassification
    ) {
        dispatchReturned = true;
        this.taskAfterDispatchEvidence = taskAfterDispatchEvidence == null
                ? FabricChatClefTaskOwnershipEvidence.empty()
                : taskAfterDispatchEvidence;
        this.rootOwnershipClassification = rootOwnershipClassification == null
                ? FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN
                : rootOwnershipClassification;
        this.boundRootTask = this.rootOwnershipClassification
                == FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT
                ? this.taskAfterDispatchEvidence.rootTask()
                : null;
        this.taskAfterDispatch = this.taskAfterDispatchEvidence.rootTaskSnapshot();
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

    long finishCallbackReceivedAtMs() {
        return finishCallbackReceivedAtMs;
    }

    FabricChatClefFinishCallbackObservation firstFinishCallbackObservation() {
        return firstFinishCallbackObservation;
    }

    int finishCallbackDuplicateCount() {
        return finishCallbackDuplicateCount;
    }

    boolean finishCallbackFirstObservedBeforeDispatchReturn() {
        return firstFinishCallbackObservation != null
                && firstFinishCallbackObservation.observedBeforeDispatchReturn();
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

    FabricChatClefBoundRootTaskRelationshipPayload boundRootRelationshipPayload(String candidateName, Task candidateTask) {
        return FabricChatClefBoundRootTaskRelationshipPayload.of(
                candidateName,
                FabricChatClefTaskSnapshot.capture(candidateTask),
                matchesBoundRootTask(candidateTask),
                boundRootMatchReason(candidateTask),
                FabricChatClefTaskSnapshot.capture(boundRootTask)
        );
    }

    FabricChatClefCommandTerminationObservation taskFinishedObservation() {
        return taskFinishedObservation;
    }

    FabricChatClefTaskOwnershipEvidence taskBeforeDispatchEvidence() {
        return taskBeforeDispatchEvidence;
    }

    FabricChatClefTaskOwnershipEvidence taskAfterDispatchEvidence() {
        return taskAfterDispatchEvidence;
    }

    FabricChatClefRootOwnershipClassification rootOwnershipClassification() {
        return rootOwnershipClassification;
    }

    void markPreexistingIdleRootStabilityObservation(
            FabricChatClefStableRequestQuiescenceObservation observation
    ) {
        preexistingIdleRootStabilityObservation = observation;
    }

    FabricChatClefStableRequestQuiescenceObservation preexistingIdleRootStabilityObservation() {
        return preexistingIdleRootStabilityObservation;
    }

    boolean preexistingIdleRootStabilityQualified() {
        return preexistingIdleRootStabilityObservation != null
                && preexistingIdleRootStabilityObservation.qualified();
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
