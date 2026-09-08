package lavi.minecraft.fabric.chatclef.bridge.command.result.effect;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;

//20260907_kpopmodder: Preserve cautious no-effect behavior for unsupported command families.
public final class FabricChatClefNoEffectTracker implements FabricChatClefCommandEffectTracker {
    private static final FabricChatClefNoEffectTracker INSTANCE =
            new FabricChatClefNoEffectTracker();

    private FabricChatClefNoEffectTracker() {
    }

    public static FabricChatClefNoEffectTracker instance() {
        return INSTANCE;
    }

    @Override
    public FabricChatClefCommandResultDataPayload fromMatchingCompletion(
            FabricChatClefCommandResultDataPayload basePayload
    ) {
        return basePayload == null
                ? FabricChatClefCommandResultDataPayload.empty()
                : basePayload;
    }
}
