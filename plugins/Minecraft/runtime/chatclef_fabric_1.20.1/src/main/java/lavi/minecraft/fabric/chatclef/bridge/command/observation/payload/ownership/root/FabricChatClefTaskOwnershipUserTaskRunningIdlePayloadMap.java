package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root;

import java.util.Map;

public final class FabricChatClefTaskOwnershipUserTaskRunningIdlePayloadMap {
    private static final String USER_TASK_RUNNING_IDLE = "user_task_running_idle";

    private FabricChatClefTaskOwnershipUserTaskRunningIdlePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean userTaskRunningIdle) {
        payload.put(USER_TASK_RUNNING_IDLE, userTaskRunningIdle);
    }
}
