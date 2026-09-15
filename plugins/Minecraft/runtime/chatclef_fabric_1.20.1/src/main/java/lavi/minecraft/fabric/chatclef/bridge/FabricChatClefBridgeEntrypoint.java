package lavi.minecraft.fabric.chatclef.bridge;

import lavi.minecraft.fabric.chatclef.bridge.runtime.FabricChatClefBridgeComponents;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalRetentionDiagnostics;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260801_kpopmodder: Register the Fabric-only LAVI ChatClef bridge and tick dispatcher.
public final class FabricChatClefBridgeEntrypoint implements ModInitializer {
    private final FabricChatClefBridgeComponents components = FabricChatClefBridgeComponents.create();

    @Override
    public void onInitialize() {
        components.taskFinishedObserver().register();
        //#if MC == 12001
        //20260915_kpopmodder: Resource reload invalidates; all registry reads stay on the client thread.
        var catalogueCapture = new lavi.minecraft.fabric.chatclef.bridge.catalogue.FabricChatClefCatalogueCapture(components.diagnostics());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(new lavi.minecraft.fabric.chatclef.bridge.catalogue.lifecycle.FabricChatClefCatalogueReloadListener(catalogueCapture));
        ClientLifecycleEvents.CLIENT_STARTED.register(catalogueCapture::captureIfChanged);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            catalogueCapture.captureIfChanged(client);
            components.bridgeClient().publishCatalogueIfChanged();
        });
        //#endif
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> components.bridgeClient().start());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> components.bridgeClient().stop());
        ClientTickEvents.END_CLIENT_TICK.register(components.commandDispatcher()::onEndClientTick);
        ClientTickEvents.END_CLIENT_TICK.register(
                FabricChatClefCraftResourceTerminalRetentionDiagnostics::onEndClientTick
        );
        components.diagnostics().info("entrypoint registered");
    }
}
