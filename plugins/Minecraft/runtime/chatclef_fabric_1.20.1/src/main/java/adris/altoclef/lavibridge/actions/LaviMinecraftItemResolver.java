package adris.altoclef.lavibridge.actions;

//20260725_kpopmodder: Added this resolver so equip actions can target registry or catalogued item names.

import adris.altoclef.catalogue.TaskCatalogue;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class LaviMinecraftItemResolver {

    public Item resolve(String itemName) {
        String normalized = normalize(itemName);
        Item exactItem = resolveExactItem(normalized);
        if (exactItem != null) {
            return exactItem;
        }

        Item[] matches = TaskCatalogue.getItemMatches(normalized);
        if (matches.length > 0) {
            return matches[0];
        }

        throw new IllegalArgumentException("Unknown item: " + normalized);
    }

    private Item resolveExactItem(String itemName) {
        Identifier identifier = Identifier.of(toIdentifierText(itemName));
        if (!Registries.ITEM.containsId(identifier)) {
            return null;
        }
        return Registries.ITEM.get(identifier);
    }

    private String toIdentifierText(String itemName) {
        return itemName.contains(":") ? itemName : "minecraft:" + itemName;
    }

    private String normalize(String itemName) {
        String normalized = String.valueOf(itemName == null ? "" : itemName).trim().toLowerCase();
        if (normalized.startsWith("minecraft:")) {
            normalized = normalized.substring("minecraft:".length());
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("item is required.");
        }
        return normalized;
    }
}
