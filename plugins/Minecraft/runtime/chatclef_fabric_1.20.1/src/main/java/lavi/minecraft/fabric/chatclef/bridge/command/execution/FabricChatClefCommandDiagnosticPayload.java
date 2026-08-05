package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260803_kpopmodder: Keep command lifecycle state separate from diagnostic payload map assembly.
public final class FabricChatClefCommandDiagnosticPayload {
    private FabricChatClefCommandDiagnosticPayload() {
    }

    public static Map<String, Object> commandData(
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
        ).toMap();
    }

    public static Map<String, Object> diagnosticData(
            String diagnosticReason,
            FabricChatClefCommandRequest request,
            String normalizedCommand,
            long elapsedMs,
            Map<String, Object> commandData
    ) {
        return new FabricChatClefCommandDiagnosticResultPayload(
                diagnosticReason,
                request,
                normalizedCommand,
                elapsedMs,
                commandData
        ).toMap();
    }
}
