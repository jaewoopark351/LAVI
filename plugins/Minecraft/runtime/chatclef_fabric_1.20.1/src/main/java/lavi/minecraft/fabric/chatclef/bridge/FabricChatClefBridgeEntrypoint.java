package lavi.minecraft.fabric.chatclef.bridge;

import lavi.minecraft.fabric.chatclef.bridge.runtime.FabricChatClefBridgeComponents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260801_kpopmodder: Register the Fabric-only LAVI ChatClef bridge and tick dispatcher.
public final class FabricChatClefBridgeEntrypoint implements ModInitializer {
    private final FabricChatClefBridgeComponents components = FabricChatClefBridgeComponents.create();

    @Override
    public void onInitialize() {
        components.taskFinishedObserver().register();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> components.bridgeClient().start());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> components.bridgeClient().stop());
        ClientTickEvents.END_CLIENT_TICK.register(components.commandDispatcher()::onEndClientTick);
        components.diagnostics().info("entrypoint registered");
    }
}
