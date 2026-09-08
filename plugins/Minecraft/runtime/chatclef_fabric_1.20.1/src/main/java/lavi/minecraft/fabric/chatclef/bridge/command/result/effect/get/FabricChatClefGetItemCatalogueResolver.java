package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

//20260907_kpopmodder: Separate GET grammar parsing from live TaskCatalogue resolution.
@FunctionalInterface
interface FabricChatClefGetItemCatalogueResolver {
    FabricChatClefGetItemCatalogueResolution resolve(String target);
}
