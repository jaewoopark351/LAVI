package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain;

import java.util.Map;

public final class FabricChatClefTaskOwnershipSelectedChainClassPayloadMap {
    private static final String SELECTED_CHAIN_CLASS = "selected_chain_class";

    private FabricChatClefTaskOwnershipSelectedChainClassPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String selectedChainClass) {
        payload.put(SELECTED_CHAIN_CLASS, selectedChainClass);
    }
}
