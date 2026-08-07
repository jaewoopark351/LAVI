package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.payload.FabricChatClefCommandLifecyclePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

import java.util.Map;

//20260805_kpopmodder: Keep command lifecycle diagnostic fields typed until the existing Map edge.
public final class FabricChatClefCommandLifecyclePayload implements FabricChatClefCommandResultDataPayload {
    private final String resultReason;
    private final long dispatchStartedMs;
    private final boolean dispatchReturned;
    private final String dispatchThreadName;
    private final String normalizedCommand;
    private final boolean finishCallbackReceived;
    private final String failureType;
    private final String failureMessage;
    private final FabricChatClefCommandContext context;
    private final FabricChatClefTaskSnapshot taskBeforeDispatch;
    private final FabricChatClefTaskSnapshot taskAfterDispatch;
    private final FabricChatClefTaskSnapshot terminalTask;
    private final FabricChatClefTaskSnapshot boundRootTask;
    private final FabricChatClefCommandTerminationObservation observation;

    private FabricChatClefCommandLifecyclePayload(
            String resultReason,
            long dispatchStartedMs,
            boolean dispatchReturned,
            String dispatchThreadName,
            String normalizedCommand,
            boolean finishCallbackReceived,
            String failureType,
            String failureMessage,
            FabricChatClefCommandContext context,
            FabricChatClefTaskSnapshot taskBeforeDispatch,
            FabricChatClefTaskSnapshot taskAfterDispatch,
            FabricChatClefTaskSnapshot terminalTask,
            FabricChatClefTaskSnapshot boundRootTask,
            FabricChatClefCommandTerminationObservation observation
    ) {
        this.resultReason = resultReason;
        this.dispatchStartedMs = dispatchStartedMs;
        this.dispatchReturned = dispatchReturned;
        this.dispatchThreadName = dispatchThreadName;
        this.normalizedCommand = normalizedCommand;
        this.finishCallbackReceived = finishCallbackReceived;
        this.failureType = failureType;
        this.failureMessage = failureMessage;
        this.context = context;
        this.taskBeforeDispatch = taskBeforeDispatch;
        this.taskAfterDispatch = taskAfterDispatch;
        this.terminalTask = terminalTask;
        this.boundRootTask = boundRootTask;
        this.observation = observation;
    }

    public static FabricChatClefCommandLifecyclePayload of(
            String resultReason,
            long dispatchStartedMs,
            boolean dispatchReturned,
            String dispatchThreadName,
            String normalizedCommand,
            boolean finishCallbackReceived,
            String failureType,
            String failureMessage,
            FabricChatClefCommandContext context,
            FabricChatClefTaskSnapshot taskBeforeDispatch,
            FabricChatClefTaskSnapshot taskAfterDispatch,
            FabricChatClefTaskSnapshot terminalTask,
            FabricChatClefTaskSnapshot boundRootTask,
            FabricChatClefCommandTerminationObservation observation
    ) {
        return new FabricChatClefCommandLifecyclePayload(
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
                boundRootTask,
                observation
        );
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefCommandLifecyclePayloadMap.toMap(
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
                boundRootTask,
                observation
        );
    }
}
