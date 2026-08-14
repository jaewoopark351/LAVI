package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain;

import java.util.Map;

public final class FabricChatClefTaskOwnershipSelectedChainUserTaskFlagPayloadMap {
    private static final String SELECTED_CHAIN_IS_USER_TASK_CHAIN = "selected_chain_is_user_task_chain";

    private FabricChatClefTaskOwnershipSelectedChainUserTaskFlagPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, boolean selectedChainIsUserTaskChain) {
        payload.put(SELECTED_CHAIN_IS_USER_TASK_CHAIN, selectedChainIsUserTaskChain);
    }
}
