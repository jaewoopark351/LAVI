package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

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
    private volatile FabricChatClefTaskSnapshot taskAfterDispatch;
    private volatile FabricChatClefTaskSnapshot terminalTask;

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

    public Map<String, Object> unknownAfterFinish(FabricChatClefTaskSnapshot taskAtFinish) {
        finishCallbackReceived = true;
        terminalTask = taskAtFinish;
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

    public void markDispatchReturned(FabricChatClefTaskSnapshot taskAfterDispatch) {
        dispatchReturned = true;
        this.taskAfterDispatch = taskAfterDispatch;
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

    private Map<String, Object> data(String resultReason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("result_fidelity", "bridge_callback_is_not_goal_success");
        payload.put("result_reason", resultReason);
        payload.put("dispatch_started_ms", dispatchStartedMs);
        payload.put("dispatch_returned", dispatchReturned);
        payload.put("dispatch_thread", dispatchThreadName);
        payload.put("normalized_command_length", normalizedCommand.length());
        payload.put("finish_callback_received", finishCallbackReceived);
        payload.put("failure_type", failureType);
        payload.put("failure_message", failureMessage);
        payload.put("ownership", context.ownershipData());
        payload.put("task_before_dispatch", taskBeforeDispatch.toMap());
        payload.put("task_after_dispatch", taskAfterDispatch.toMap());
        payload.put("terminal_task", terminalTask.toMap());
        return payload;
    }

    private static String nullSafeMessage(Throwable error) {
        return error.getMessage() == null ? "" : error.getMessage();
    }
}
