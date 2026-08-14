package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain;

import java.util.Map;

public final class FabricChatClefTaskOwnershipSelectedChainIdentityPayloadMap {
    private static final String SELECTED_CHAIN_IDENTITY = "selected_chain_identity";

    private FabricChatClefTaskOwnershipSelectedChainIdentityPayloadMap() {
    }

    public static void writeTo(Map<String, Object> payload, String selectedChainIdentity) {
        payload.put(SELECTED_CHAIN_IDENTITY, selectedChainIdentity);
    }
}
