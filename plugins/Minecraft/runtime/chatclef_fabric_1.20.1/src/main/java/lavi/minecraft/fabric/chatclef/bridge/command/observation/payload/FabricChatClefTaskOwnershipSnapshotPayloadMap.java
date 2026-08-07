package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.HashMap;
import java.util.Map;

//20260808_kpopmodder: Isolate task ownership snapshot Map keys for diagnostics-only lifecycle evidence.
public final class FabricChatClefTaskOwnershipSnapshotPayloadMap {
    private static final String AVAILABLE = "available";
    private static final String ERROR = "error";
    private static final String CAPTURED_AT_MS = "captured_at_ms";
    private static final String CAPTURED_CLIENT_TICK = "captured_client_tick";
    private static final String CAPTURE_THREAD = "capture_thread";
    private static final String USER_TASK_ROOT = "user_task_root";
    private static final String USER_TASK_ROOT_CLASS = "user_task_root_class";
    private static final String USER_TASK_ROOT_IDENTITY = "user_task_root_identity";
    private static final String USER_TASK_ROOT_ASSIGNMENT_ID = "user_task_root_assignment_id";
    private static final String USER_TASK_ROOT_GENERATION = "user_task_root_generation";
    private static final String USER_TASK_RUNNING_IDLE = "user_task_running_idle";
    private static final String NEXT_TASK_IDLE_FLAG = "next_task_idle_flag";
    private static final String TASK_RUNNER_ACTIVE = "task_runner_active";
    private static final String SELECTED_CHAIN_CLASS = "selected_chain_class";
    private static final String SELECTED_CHAIN_IDENTITY = "selected_chain_identity";
    private static final String SELECTED_CHAIN_IS_USER_TASK_CHAIN = "selected_chain_is_user_task_chain";
    private static final String SELECTED_CHAIN_TASK_PATH = "selected_chain_task_path";

    private FabricChatClefTaskOwnershipSnapshotPayloadMap() {
    }

    public static Map<String, Object> toMap(
            boolean available,
            String error,
            long capturedAtMs,
            long capturedClientTick,
            String captureThread,
            FabricChatClefTaskSnapshot userTaskRoot,
            String userTaskRootClass,
            String userTaskRootIdentity,
            String userTaskRootAssignmentId,
            long userTaskRootGeneration,
            boolean userTaskRunningIdle,
            boolean nextTaskIdleFlag,
            boolean taskRunnerActive,
            String selectedChainClass,
            String selectedChainIdentity,
            boolean selectedChainIsUserTaskChain,
            String selectedChainTaskPath
    ) {
        Map<String, Object> payload = new HashMap<>();
        payload.put(AVAILABLE, available);
        payload.put(ERROR, error);
        payload.put(CAPTURED_AT_MS, capturedAtMs);
        payload.put(CAPTURED_CLIENT_TICK, capturedClientTick);
        payload.put(CAPTURE_THREAD, captureThread);
        payload.put(USER_TASK_ROOT, userTaskRoot.toMap());
        payload.put(USER_TASK_ROOT_CLASS, userTaskRootClass);
        payload.put(USER_TASK_ROOT_IDENTITY, userTaskRootIdentity);
        payload.put(USER_TASK_ROOT_ASSIGNMENT_ID, userTaskRootAssignmentId);
        payload.put(USER_TASK_ROOT_GENERATION, userTaskRootGeneration);
        payload.put(USER_TASK_RUNNING_IDLE, userTaskRunningIdle);
        payload.put(NEXT_TASK_IDLE_FLAG, nextTaskIdleFlag);
        payload.put(TASK_RUNNER_ACTIVE, taskRunnerActive);
        payload.put(SELECTED_CHAIN_CLASS, selectedChainClass);
        payload.put(SELECTED_CHAIN_IDENTITY, selectedChainIdentity);
        payload.put(SELECTED_CHAIN_IS_USER_TASK_CHAIN, selectedChainIsUserTaskChain);
        payload.put(SELECTED_CHAIN_TASK_PATH, selectedChainTaskPath);
        return payload;
    }
}
