package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting.current.FabricChatClefWaitingCurrentTaskRelationshipPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.payload.waiting.current.FabricChatClefWaitingCurrentTaskMatchReasonPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefBoundRootTaskRelationshipPayload;

import java.util.Map;

//20260808_kpopmodder: Keep terminal-wait current-task fields separate without changing emitted keys.
public final class FabricChatClefWaitingCurrentTaskPayloadMap {
    private FabricChatClefWaitingCurrentTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> details,
            FabricChatClefBoundRootTaskRelationshipPayload currentTask,
            String currentTaskBoundRootMatchReason
    ) {
        FabricChatClefWaitingCurrentTaskRelationshipPayloadMap.writeTo(details, currentTask);
        FabricChatClefWaitingCurrentTaskMatchReasonPayloadMap.writeTo(details, currentTaskBoundRootMatchReason);
    }
}
