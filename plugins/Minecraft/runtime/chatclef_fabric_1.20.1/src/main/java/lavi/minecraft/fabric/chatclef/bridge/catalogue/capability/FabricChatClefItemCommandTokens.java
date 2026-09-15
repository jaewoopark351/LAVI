//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.catalogue.capability;

import adris.altoclef.TaskCatalogue;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Items;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

//20260915_kpopmodder: Derive native ItemList capability separately from registry name coverage.
public final class FabricChatClefItemCommandTokens {
    private final Map<Item, List<String>> exactAliases = new HashMap<>();

    public FabricChatClefItemCommandTokens() {
        for (String alias : TaskCatalogue.resourceNames().stream().sorted().toList()) {
            Item[] matches = TaskCatalogue.getItemMatches(alias);
            if (matches != null && matches.length == 1 && matches[0] != null)
                exactAliases.computeIfAbsent(matches[0], ignored -> new ArrayList<>()).add(alias);
        }
    }

    public Map<String, String> tokens(Item item, String registryPath) {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("give", ItemHelper.stripItemName(item));
        List<String> aliases = exactAliases.getOrDefault(item, List.of());
        String token = aliases.contains(registryPath) ? registryPath : aliases.stream().findFirst().orElse(null);
        if (token != null) {
            tokens.put("get", token);
            tokens.put("deposit", token);
            tokens.put("deposit_all", token);
            // Both native command validation and its actual task cast must support the item.
            if (item instanceof Equipment && (item instanceof ArmorItem || item == Items.SHIELD)) tokens.put("equip", token);
        }
        return tokens;
    }

    public List<String> exactAliases(Item item) { return List.copyOf(exactAliases.getOrDefault(item, List.of())); }
}
//#endif
