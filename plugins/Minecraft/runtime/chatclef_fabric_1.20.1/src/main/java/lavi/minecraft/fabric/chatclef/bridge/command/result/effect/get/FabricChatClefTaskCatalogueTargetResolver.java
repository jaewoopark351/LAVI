package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import adris.altoclef.TaskCatalogue;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.TreeSet;

//20260907_kpopmodder: Resolve only actual registered TaskCatalogue targets and vanilla registry IDs.
final class FabricChatClefTaskCatalogueTargetResolver
        implements FabricChatClefGetItemCatalogueResolver {
    @Override
    public FabricChatClefGetItemCatalogueResolution resolve(String target) {
        try {
            if (!TaskCatalogue.taskExists(target)) {
                return FabricChatClefGetItemCatalogueResolution.unavailable();
            }
            Item[] catalogueMatches = TaskCatalogue.getItemMatches(target);
            if (catalogueMatches == null
                    || catalogueMatches.length == 0
                    || catalogueMatches.length
                    > FabricChatClefGetItemCatalogueResolution.MAX_TARGET_MATCH_IDS) {
                return FabricChatClefGetItemCatalogueResolution.unavailable();
            }
            TreeSet<String> matchIds = new TreeSet<>();
            for (Item match : catalogueMatches) {
                if (match == null) {
                    return FabricChatClefGetItemCatalogueResolution.unavailable();
                }
                Identifier registryId = Registries.ITEM.getId(match);
                if (registryId == null) {
                    return FabricChatClefGetItemCatalogueResolution.unavailable();
                }
                matchIds.add(registryId.toString());
            }
            return FabricChatClefGetItemCatalogueResolution.resolved(
                    List.copyOf(matchIds)
            );
        } catch (RuntimeException | LinkageError error) {
            return FabricChatClefGetItemCatalogueResolution.unavailable();
        }
    }
}
