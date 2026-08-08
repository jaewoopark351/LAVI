package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.FabricChatClefTaskFinishedEventPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.FabricChatClefTaskFinishedMatchPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.taskfinished.FabricChatClefTaskFinishedRuntimePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.FabricChatClefTaskFinishedObservationPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;

import java.util.HashMap;
import java.util.Map;

public final class FabricChatClefTaskFinishedEventDetailsPayloadMap {
    private FabricChatClefTaskFinishedEventDetailsPayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefTaskFinishedObservationPayload taskFinishedEvent,
            boolean matchedBoundRootTask,
            FabricChatClefBoundRootTaskRelationshipPayload eventTaskRelationship,
            String eventTaskBoundRootMatchReason,
            boolean finishCallbackReceived,
            FabricChatClefTaskRuntimeObservationPayload runtime
    ) {
        Map<String, Object> payload = new HashMap<>();
        FabricChatClefTaskFinishedEventPayloadMap.writeTo(payload, taskFinishedEvent);
        FabricChatClefTaskFinishedMatchPayloadMap.writeTo(
                payload,
                matchedBoundRootTask,
                eventTaskRelationship,
                eventTaskBoundRootMatchReason
        );
        FabricChatClefTaskFinishedRuntimePayloadMap.writeTo(
                payload,
                finishCallbackReceived,
                runtime
        );
        return payload;
    }
}
