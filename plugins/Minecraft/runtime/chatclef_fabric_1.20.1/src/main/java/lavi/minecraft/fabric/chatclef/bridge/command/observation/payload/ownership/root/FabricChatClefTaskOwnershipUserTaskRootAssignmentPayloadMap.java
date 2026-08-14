package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root;

import java.util.Map;

public final class FabricChatClefTaskOwnershipUserTaskRootAssignmentPayloadMap {
    private static final String USER_TASK_ROOT_ASSIGNMENT_ID = "user_task_root_assignment_id";

    private FabricChatClefTaskOwnershipUserTaskRootAssignmentPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String userTaskRootAssignmentId) {
        payload.put(USER_TASK_ROOT_ASSIGNMENT_ID, userTaskRootAssignmentId);
    }
}
