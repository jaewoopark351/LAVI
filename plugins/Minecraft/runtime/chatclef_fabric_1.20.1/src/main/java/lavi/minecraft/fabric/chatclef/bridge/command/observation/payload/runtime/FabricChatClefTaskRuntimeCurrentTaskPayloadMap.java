package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.runtime;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260808_kpopmodder: Keep current-task and ownership fields isolated at the runtime observation Map edge.
public final class FabricChatClefTaskRuntimeCurrentTaskPayloadMap {
    private static final String CURRENT_TASK = "current_task";
    private static final String OWNERSHIP = "ownership";

    private FabricChatClefTaskRuntimeCurrentTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskSnapshot currentTask,
            FabricChatClefTaskOwnershipSnapshot ownership
    ) {
        payload.put(CURRENT_TASK, currentTask.toMap());
        payload.put(OWNERSHIP, ownership.toMap());
    }
}
