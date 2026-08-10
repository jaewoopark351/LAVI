package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.current.FabricChatClefTaskRuntimeCurrentTaskSnapshotPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime.current.FabricChatClefTaskRuntimeOwnershipPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Keep current-task and ownership fields isolated at the runtime observation Map edge.
public final class FabricChatClefTaskRuntimeCurrentTaskPayloadMap {
    private FabricChatClefTaskRuntimeCurrentTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskSnapshot currentTask,
            FabricChatClefTaskOwnershipSnapshot ownership
    ) {
        FabricChatClefTaskRuntimeCurrentTaskSnapshotPayloadMap.writeTo(payload, currentTask);
        FabricChatClefTaskRuntimeOwnershipPayloadMap.writeTo(payload, ownership);
    }
}
