package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

//20260907_kpopmodder: Project GET evidence only at the matching-completion result boundary.
public final class FabricChatClefGetItemEffectResultProjector {
    public FabricChatClefCommandResultDataPayload fromMatchingCompletion(
            FabricChatClefCommandResultDataPayload basePayload,
            FabricChatClefGetItemEffectTracker tracker
    ) {
        if (tracker == null || !tracker.tracked()) {
            return basePayload;
        }
        return new FabricChatClefGetItemEffectResultDataPayload(
                basePayload,
                tracker.terminalEvidence()
        );
    }
}
