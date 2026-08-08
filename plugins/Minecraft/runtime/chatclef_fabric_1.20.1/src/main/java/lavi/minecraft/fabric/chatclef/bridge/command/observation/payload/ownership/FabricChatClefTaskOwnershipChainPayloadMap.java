package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership;

import java.util.Map;

//20260808_kpopmodder: Isolate selected-chain ownership fields from task-root snapshot serialization.
public final class FabricChatClefTaskOwnershipChainPayloadMap {
    private static final String TASK_RUNNER_ACTIVE = "task_runner_active";
    private static final String SELECTED_CHAIN_CLASS = "selected_chain_class";
    private static final String SELECTED_CHAIN_IDENTITY = "selected_chain_identity";
    private static final String SELECTED_CHAIN_IS_USER_TASK_CHAIN = "selected_chain_is_user_task_chain";
    private static final String SELECTED_CHAIN_TASK_PATH = "selected_chain_task_path";

    private FabricChatClefTaskOwnershipChainPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            boolean taskRunnerActive,
            String selectedChainClass,
            String selectedChainIdentity,
            boolean selectedChainIsUserTaskChain,
            String selectedChainTaskPath
    ) {
        payload.put(TASK_RUNNER_ACTIVE, taskRunnerActive);
        payload.put(SELECTED_CHAIN_CLASS, selectedChainClass);
        payload.put(SELECTED_CHAIN_IDENTITY, selectedChainIdentity);
        payload.put(SELECTED_CHAIN_IS_USER_TASK_CHAIN, selectedChainIsUserTaskChain);
        payload.put(SELECTED_CHAIN_TASK_PATH, selectedChainTaskPath);
    }
}
