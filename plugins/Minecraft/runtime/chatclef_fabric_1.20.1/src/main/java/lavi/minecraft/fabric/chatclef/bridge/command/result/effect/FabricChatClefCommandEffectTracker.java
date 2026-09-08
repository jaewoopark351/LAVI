package lavi.minecraft.fabric.chatclef.bridge.command.result.effect;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

//20260907_kpopmodder: Define the narrow result-only port for one command's effect evidence.
public interface FabricChatClefCommandEffectTracker {
    FabricChatClefCommandResultDataPayload fromMatchingCompletion(
            FabricChatClefCommandResultDataPayload basePayload
    );
}
