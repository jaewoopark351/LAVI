package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root;

import java.util.Map;

public final class FabricChatClefTaskOwnershipUserTaskRootIdentityPayloadMap {
    private static final String USER_TASK_ROOT_IDENTITY = "user_task_root_identity";

    private FabricChatClefTaskOwnershipUserTaskRootIdentityPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String userTaskRootIdentity) {
        payload.put(USER_TASK_ROOT_IDENTITY, userTaskRootIdentity);
    }
}
