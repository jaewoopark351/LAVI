package lavi.minecraft.fabric.chatclef.bridge;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandDispatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfig;
import lavi.minecraft.fabric.chatclef.bridge.config.FabricChatClefBridgeConfigLoader;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeJson;
import lavi.minecraft.fabric.chatclef.bridge.protocol.FabricChatClefBridgeMessageFactory;
import lavi.minecraft.fabric.chatclef.bridge.state.FabricChatClefBridgeState;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefBridgeClient;
import lavi.minecraft.fabric.chatclef.bridge.transport.FabricChatClefReconnectScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260801_kpopmodder: Register the Fabric-only LAVI ChatClef bridge and tick dispatcher.
public final class FabricChatClefBridgeEntrypoint implements ModInitializer {
    private final FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
    private final FabricChatClefCommandQueue commandQueue = new FabricChatClefCommandQueue();
    private final FabricChatClefBridgeClient bridgeClient = createBridgeClient();
    private final FabricChatClefCommandDispatcher commandDispatcher = new FabricChatClefCommandDispatcher(
            commandQueue,
            bridgeClient,
            diagnostics
    );

    @Override
    public void onInitialize() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> bridgeClient.start());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> bridgeClient.stop());
        ClientTickEvents.END_CLIENT_TICK.register(commandDispatcher::onEndClientTick);
        diagnostics.info("entrypoint registered");
    }

    private FabricChatClefBridgeClient createBridgeClient() {
        FabricChatClefBridgeConfig config = new FabricChatClefBridgeConfigLoader().load();
        return new FabricChatClefBridgeClient(
                config,
                commandQueue,
                new FabricChatClefBridgeState(),
                diagnostics,
                new FabricChatClefBridgeJson(),
                new FabricChatClefBridgeMessageFactory(),
                new FabricChatClefReconnectScheduler()
        );
    }
}
