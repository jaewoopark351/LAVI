package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain.FabricChatClefTaskOwnershipSelectedChainClassPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain.FabricChatClefTaskOwnershipSelectedChainIdentityPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain.FabricChatClefTaskOwnershipSelectedChainTaskPathPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain.FabricChatClefTaskOwnershipSelectedChainUserTaskFlagPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.chain.FabricChatClefTaskOwnershipTaskRunnerActivePayloadMap;

import java.util.Map;

//20260808_kpopmodder: Isolate selected-chain ownership fields from task-root snapshot serialization.
public final class FabricChatClefTaskOwnershipChainPayloadMap {
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
        FabricChatClefTaskOwnershipTaskRunnerActivePayloadMap.writeTo(payload, taskRunnerActive);
        FabricChatClefTaskOwnershipSelectedChainClassPayloadMap.writeTo(payload, selectedChainClass);
        FabricChatClefTaskOwnershipSelectedChainIdentityPayloadMap.writeTo(payload, selectedChainIdentity);
        FabricChatClefTaskOwnershipSelectedChainUserTaskFlagPayloadMap.writeTo(payload, selectedChainIsUserTaskChain);
        FabricChatClefTaskOwnershipSelectedChainTaskPathPayloadMap.writeTo(payload, selectedChainTaskPath);
    }
}
