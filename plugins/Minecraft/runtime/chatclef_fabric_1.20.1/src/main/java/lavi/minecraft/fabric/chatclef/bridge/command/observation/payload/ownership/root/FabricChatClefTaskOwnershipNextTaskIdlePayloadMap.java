package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root;

import java.util.Map;

public final class FabricChatClefTaskOwnershipNextTaskIdlePayloadMap {
    private static final String NEXT_TASK_IDLE_FLAG = "next_task_idle_flag";

    private FabricChatClefTaskOwnershipNextTaskIdlePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean nextTaskIdleFlag) {
        payload.put(NEXT_TASK_IDLE_FLAG, nextTaskIdleFlag);
    }
}
