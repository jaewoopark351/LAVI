package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

public final class FabricChatClefTaskOwnershipUserTaskRootPayloadMap {
    private static final String USER_TASK_ROOT = "user_task_root";

    private FabricChatClefTaskOwnershipUserTaskRootPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, FabricChatClefTaskSnapshot userTaskRoot) {
        payload.put(USER_TASK_ROOT, userTaskRoot.toMap());
    }
}
