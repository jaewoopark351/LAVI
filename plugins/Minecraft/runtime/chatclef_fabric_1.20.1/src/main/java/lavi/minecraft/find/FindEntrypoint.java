package lavi.minecraft.find;

import net.fabricmc.api.ModInitializer;
//#if MC == 12001
//$$ import lavi.minecraft.find.catalog.FindCatalogRuntime;
//$$ import lavi.minecraft.find.command.FindCommandRegistrar;
//$$ import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//$$ import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
//$$ import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
//$$ import net.minecraft.resource.ResourceManager;
//$$ import net.minecraft.resource.ResourceType;
//$$ import net.minecraft.util.Identifier;
//#endif

//20260914_kpopmodder: Add only the Fabric 1.20.1 FIND integration; other supported runtime variants remain unchanged.
public final class FindEntrypoint implements ModInitializer {
    @Override public void onInitialize() {
        //#if MC == 12001
//$$         FindCommandRegistrar registrar = new FindCommandRegistrar();
//$$         ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
//$$             @Override public Identifier getFabricId() { return new Identifier("lavi", "find_catalog"); }
//$$             @Override public void reload(ResourceManager manager) { FindCatalogRuntime.instance().invalidate(); }
//$$         });
//$$         ClientTickEvents.END_CLIENT_TICK.register(client -> {
//$$             FindCatalogRuntime.instance().onEndClientTick(client);
//$$             registrar.onEndClientTick();
//$$         });
        //#endif
    }
}
