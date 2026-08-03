package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;

import java.util.HashMap;
import java.util.Map;

//20260801_kpopmodder: Keep bridge result fidelity separate from ChatClef command callbacks.
public final class FabricChatClefCommandExecution {
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
    }

    public Map<String, Object> runningResult() {
        return FabricChatClefCommandResult.running(
                request.requestId,
                "Fabric ChatClef command dispatch started.",
                data("dispatch_started")
        );
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
        return FabricChatClefCommandResult.unknown(
                request.requestId,
                "Fabric ChatClef command callback finished, but Minecraft goal success was not verified.",
                data("finish_callback_without_verified_success")
        );
    }

    public Map<String, Object> failedFromCommandException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        failureType = exception.getClass().getSimpleName();
        failureMessage = nullSafeMessage(exception);
        terminalTask = taskAtFailure;
        return FabricChatClefCommandResult.failed(
                request.requestId,
                failureType + ": " + failureMessage,
                data("command_exception")
        );
    }

    public Map<String, Object> failedFromDispatchException(Throwable exception, FabricChatClefTaskSnapshot taskAtFailure) {
        failureType = exception.getClass().getSimpleName();
        failureMessage = nullSafeMessage(exception);
        terminalTask = taskAtFailure;
        return FabricChatClefCommandResult.failed(
                request.requestId,
                failureType + ": " + failureMessage,
                data("dispatch_exception")
        );
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
        return FabricChatClefCommandResult.completed(
                request.requestId,
                "ChatClef user task reached natural completion.",
                data("matching_task_finished", observation)
        );
    }

    public Map<String, Object> completedWithoutUserTask() {
        return FabricChatClefCommandResult.completed(
                request.requestId,
                "ChatClef command completed without starting a user task.",
                data("callback_completed_without_user_task")
        );
    }

    public Map<String, Object> failedFromStoppedTask(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.failed(
                request.requestId,
                "ChatClef user task stopped before natural completion.",
                data("matching_task_stopped", observation)
        );
    }

    public Map<String, Object> unknownFromTaskObservation(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.unknown(
                request.requestId,
                "ChatClef user task completion could not be safely classified.",
                data("task_observation_unclassified", observation)
        );
    }

    public Map<String, Object> unknownFromTaskIdentityMismatch(FabricChatClefCommandTerminationObservation observation) {
        return FabricChatClefCommandResult.unknown(
                request.requestId,
                "ChatClef user task finished, but it did not match the command root task.",
                data("task_identity_mismatch", observation)
        );
    }

    public Map<String, Object> deadlineExceededResult(String message) {
        Map<String, Object> payload = data("deadline_exceeded");
        payload.put("automation_cancelled", false);
        payload.put("task_may_still_be_running", true);
        payload.put("late_terminal_event_will_be_ignored", true);
        return FabricChatClefCommandResult.deadlineExceeded(
                request.requestId,
                message,
                payload
        );
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
        return data(reason);
    }

    public Map<String, Object> diagnosticData(String diagnosticReason) {
        return FabricChatClefCommandDiagnosticPayload.diagnosticData(
                diagnosticReason,
                request,
                normalizedCommand,
                System.currentTimeMillis() - dispatchStartedMs,
                data(diagnosticReason)
        );
    }

    private Map<String, Object> data(String resultReason) {
        return data(resultReason, taskFinishedObservation);
    }

    private Map<String, Object> data(
            String resultReason,
            FabricChatClefCommandTerminationObservation observation
    ) {
        return FabricChatClefCommandDiagnosticPayload.commandData(
                resultReason,
                dispatchStartedMs,
                dispatchReturned,
                dispatchThreadName,
                normalizedCommand,
                finishCallbackReceived,
                failureType,
                failureMessage,
                context,
                taskBeforeDispatch,
                taskAfterDispatch,
                terminalTask,
                FabricChatClefTaskSnapshot.capture(boundRootTask),
                observation
        );
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
