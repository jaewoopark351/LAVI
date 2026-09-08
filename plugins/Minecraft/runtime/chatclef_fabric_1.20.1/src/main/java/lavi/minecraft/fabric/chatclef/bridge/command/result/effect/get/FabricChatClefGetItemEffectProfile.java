package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import java.util.List;

//20260907_kpopmodder: Keep an immutable single-target GET acquisition profile.
public final class FabricChatClefGetItemEffectProfile {
    private static final String EFFECT_PROFILE_ID = "fabric_chatclef_get_acquire_delta";
    private static final int EFFECT_PROFILE_VERSION = 1;
    private static final String EFFECT_KIND = "get_acquisition_delta";
    private static final String QUANTITY_SEMANTICS = "ACQUIRE_DELTA";

    private final boolean tracked;
    private final String targetItem;
    private final int requestedCount;
    private final List<String> targetMatchIds;
    private final boolean legacyFlatCompatible;

    private FabricChatClefGetItemEffectProfile(
            boolean tracked,
            String targetItem,
            int requestedCount,
            List<String> targetMatchIds,
            boolean legacyFlatCompatible
    ) {
        this.tracked = tracked;
        this.targetItem = targetItem == null ? "" : targetItem;
        this.requestedCount = requestedCount;
        this.targetMatchIds = targetMatchIds == null
                ? List.of()
                : List.copyOf(targetMatchIds);
        this.legacyFlatCompatible = legacyFlatCompatible;
    }

    public static FabricChatClefGetItemEffectProfile fromNormalizedCommand(String command) {
        return FabricChatClefGetItemEffectProfileDecoder.decode(command);
    }

    static FabricChatClefGetItemEffectProfile tracked(
            String targetItem,
            int requestedCount,
            List<String> targetMatchIds,
            boolean legacyFlatCompatible
    ) {
        return new FabricChatClefGetItemEffectProfile(
                true,
                targetItem,
                requestedCount,
                targetMatchIds,
                legacyFlatCompatible
        );
    }

    static FabricChatClefGetItemEffectProfile untracked() {
        return new FabricChatClefGetItemEffectProfile(
                false,
                "",
                0,
                List.of(),
                false
        );
    }

    public boolean tracked() {
        return tracked;
    }

    public String effectProfileId() {
        return tracked ? EFFECT_PROFILE_ID : "";
    }

    public int effectProfileVersion() {
        return tracked ? EFFECT_PROFILE_VERSION : 0;
    }

    public String effectKind() {
        return tracked ? EFFECT_KIND : "";
    }

    public String quantitySemantics() {
        return tracked ? QUANTITY_SEMANTICS : "";
    }

    public String targetItem() {
        return tracked ? targetItem : "";
    }

    public int requestedCount() {
        return tracked ? requestedCount : 0;
    }

    public List<String> targetMatchIds() {
        return targetMatchIds;
    }

    boolean legacyFlatCompatible() {
        return tracked && legacyFlatCompatible;
    }
}
