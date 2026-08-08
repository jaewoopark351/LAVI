package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260808_kpopmodder: Keep UserTask root ownership fields separate from selected-chain serialization.
public final class FabricChatClefTaskOwnershipRootPayloadMap {
    private static final String USER_TASK_ROOT = "user_task_root";
    private static final String USER_TASK_ROOT_CLASS = "user_task_root_class";
    private static final String USER_TASK_ROOT_IDENTITY = "user_task_root_identity";
    private static final String USER_TASK_ROOT_ASSIGNMENT_ID = "user_task_root_assignment_id";
    private static final String USER_TASK_ROOT_GENERATION = "user_task_root_generation";
    private static final String USER_TASK_RUNNING_IDLE = "user_task_running_idle";
    private static final String NEXT_TASK_IDLE_FLAG = "next_task_idle_flag";

    private FabricChatClefTaskOwnershipRootPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskSnapshot userTaskRoot,
            String userTaskRootClass,
            String userTaskRootIdentity,
            String userTaskRootAssignmentId,
            long userTaskRootGeneration,
            boolean userTaskRunningIdle,
            boolean nextTaskIdleFlag
    ) {
        payload.put(USER_TASK_ROOT, userTaskRoot.toMap());
        payload.put(USER_TASK_ROOT_CLASS, userTaskRootClass);
        payload.put(USER_TASK_ROOT_IDENTITY, userTaskRootIdentity);
        payload.put(USER_TASK_ROOT_ASSIGNMENT_ID, userTaskRootAssignmentId);
        payload.put(USER_TASK_ROOT_GENERATION, userTaskRootGeneration);
        payload.put(USER_TASK_RUNNING_IDLE, userTaskRunningIdle);
        payload.put(NEXT_TASK_IDLE_FLAG, nextTaskIdleFlag);
    }
}
