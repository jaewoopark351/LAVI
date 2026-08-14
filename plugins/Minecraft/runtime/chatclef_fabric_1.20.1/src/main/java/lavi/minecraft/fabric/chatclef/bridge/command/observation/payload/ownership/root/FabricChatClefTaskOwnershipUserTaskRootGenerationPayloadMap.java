package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root;

import java.util.Map;

public final class FabricChatClefTaskOwnershipUserTaskRootGenerationPayloadMap {
    private static final String USER_TASK_ROOT_GENERATION = "user_task_root_generation";

    private FabricChatClefTaskOwnershipUserTaskRootGenerationPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, long userTaskRootGeneration) {
        payload.put(USER_TASK_ROOT_GENERATION, userTaskRootGeneration);
    }
}
