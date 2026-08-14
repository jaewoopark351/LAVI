package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root;

import java.util.Map;

public final class FabricChatClefTaskOwnershipUserTaskRootClassPayloadMap {
    private static final String USER_TASK_ROOT_CLASS = "user_task_root_class";

    private FabricChatClefTaskOwnershipUserTaskRootClassPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String userTaskRootClass) {
        payload.put(USER_TASK_ROOT_CLASS, userTaskRootClass);
    }
}
