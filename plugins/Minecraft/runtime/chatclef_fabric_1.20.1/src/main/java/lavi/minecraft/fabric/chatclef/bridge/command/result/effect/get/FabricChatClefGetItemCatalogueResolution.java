package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

//20260907_kpopmodder: Keep one catalogue target's resolved match set immutable.
final class FabricChatClefGetItemCatalogueResolution {
    static final int MAX_TARGET_MATCH_IDS = 2048;
    private static final Pattern MINECRAFT_ITEM_ID =
            Pattern.compile("minecraft:[a-z0-9_./-]+");

    private final boolean resolved;
    private final List<String> targetMatchIds;

    private FabricChatClefGetItemCatalogueResolution(
            boolean resolved,
            List<String> targetMatchIds
    ) {
        this.resolved = resolved;
        this.targetMatchIds = targetMatchIds == null
                ? List.of()
                : List.copyOf(targetMatchIds);
    }

    static FabricChatClefGetItemCatalogueResolution resolved(List<String> targetMatchIds) {
        if (targetMatchIds == null
                || targetMatchIds.isEmpty()
                || targetMatchIds.size() > MAX_TARGET_MATCH_IDS) {
            return unavailable();
        }
        TreeSet<String> canonicalIds = new TreeSet<>();
        for (String targetMatchId : targetMatchIds) {
            if (targetMatchId == null
                    || !MINECRAFT_ITEM_ID.matcher(targetMatchId).matches()) {
                return unavailable();
            }
            canonicalIds.add(targetMatchId);
        }
        return new FabricChatClefGetItemCatalogueResolution(
                true,
                List.copyOf(canonicalIds)
        );
    }

    static FabricChatClefGetItemCatalogueResolution unavailable() {
        return new FabricChatClefGetItemCatalogueResolution(
                false,
                List.of()
        );
    }

    boolean resolved() {
        return resolved;
    }

    List<String> targetMatchIds() {
        return targetMatchIds;
    }
}
