package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain;

import java.util.Map;

public final class FabricChatClefTaskOwnershipSelectedChainTaskPathPayloadMap {
    private static final String SELECTED_CHAIN_TASK_PATH = "selected_chain_task_path";

    private FabricChatClefTaskOwnershipSelectedChainTaskPathPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String selectedChainTaskPath) {
        payload.put(SELECTED_CHAIN_TASK_PATH, selectedChainTaskPath);
    }
}
