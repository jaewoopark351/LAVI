package lavi.minecraft.fabric.chatclef.bridge.catalogue.names;

import java.util.Map;

//20260915_kpopmodder: Name coverage and resource-read failures are distinct immutable evidence.
public record FabricChatClefKoreanLanguageSnapshot(Map<String, String> names, int resourceCount, int failedCount) {
    public FabricChatClefKoreanLanguageSnapshot { names = Map.copyOf(names); }
}
