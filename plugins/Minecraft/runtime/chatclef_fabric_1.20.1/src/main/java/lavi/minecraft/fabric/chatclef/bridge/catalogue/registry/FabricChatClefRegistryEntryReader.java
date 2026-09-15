//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.catalogue.registry;

import lavi.minecraft.fabric.chatclef.bridge.catalogue.capability.FabricChatClefItemCommandTokens;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.capability.FabricChatClefBlockCommandTokens;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.encoding.FabricChatClefCatalogueEncoder;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260915_kpopmodder: Project only existing runtime IDs, translation keys and native command tokens.
public final class FabricChatClefRegistryEntryReader {
    public List<Map<String, Object>> read(Map<String, String> koreanNames) {
        List<Map<String, Object>> entries = new ArrayList<>();
        FabricChatClefItemCommandTokens itemTokens = new FabricChatClefItemCommandTokens();
        for (var item : Registries.ITEM) {
            var id = Registries.ITEM.getId(item);
            var tokens = itemTokens.tokens(item, id.getPath());
            var entry = entry("item", id.toString(), item.getTranslationKey(), koreanNames, tokens);
            entry.put("catalogue_aliases", itemTokens.exactAliases(item));
            entries.add(entry);
        }
        Map<Block, String> scanTokens = new FabricChatClefBlockCommandTokens().read();
        for (var block : Registries.BLOCK) {
            String scan = scanTokens.get(block);
            entries.add(entry("block", Registries.BLOCK.getId(block).toString(), block.getTranslationKey(),
                    koreanNames, scan == null ? Map.of() : Map.of("scan", scan)));
        }
        for (var entity : Registries.ENTITY_TYPE) {
            String id = Registries.ENTITY_TYPE.getId(entity).toString();
            Map<String, String> tokens = id.equals("minecraft:player") ? Map.of()
                    : Map.of("attack", entity.getUntranslatedName());
            entries.add(entry("entity", id, entity.getTranslationKey(), koreanNames, tokens));
        }
        if (entries.size() > FabricChatClefCatalogueEncoder.MAX_ENTRIES)
            throw new IllegalStateException("entry_limit");
        entries.sort(java.util.Comparator.comparing(value -> value.get("kind") + ":" + value.get("id")));
        return List.copyOf(entries);
    }

    private Map<String, Object> entry(String kind, String id, String key, Map<String, String> names,
                                      Map<String, String> tokens) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("kind", kind);
        entry.put("id", id);
        entry.put("translation_key", key);
        entry.put("korean_name", names.get(key));
        entry.put("tokens", tokens);
        var capabilities = new ArrayList<>(tokens.keySet());
        capabilities.add("find");
        entry.put("capabilities", capabilities);
        return entry;
    }

}
//#endif
