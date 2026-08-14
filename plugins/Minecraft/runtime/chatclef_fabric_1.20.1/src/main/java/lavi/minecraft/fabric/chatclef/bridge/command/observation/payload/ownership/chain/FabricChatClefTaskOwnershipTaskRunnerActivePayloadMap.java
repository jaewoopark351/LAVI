package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain;

import java.util.Map;

public final class FabricChatClefTaskOwnershipTaskRunnerActivePayloadMap {
    private static final String TASK_RUNNER_ACTIVE = "task_runner_active";

    private FabricChatClefTaskOwnershipTaskRunnerActivePayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean taskRunnerActive) {
        payload.put(TASK_RUNNER_ACTIVE, taskRunnerActive);
    }
}
