package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting.FabricChatClefWaitingCurrentTaskPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting.FabricChatClefWaitingReasonPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting.FabricChatClefWaitingRuntimePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefWaitingForTerminalConditionDetailsPayloadMap {
    private FabricChatClefWaitingForTerminalConditionDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(
            String waitingReason,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask,
            String currentTaskBoundRootMatchReason,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> details = new HashMap<>();
        FabricChatClefWaitingReasonPayloadMap.writeTo(details, waitingReason);
        FabricChatClefWaitingCurrentTaskPayloadMap.writeTo(
                details,
                currentTask,
                currentTaskBoundRootMatchReason
        );
        FabricChatClefWaitingRuntimePayloadMap.writeTo(details, runtime);
        return details;
    }
}
