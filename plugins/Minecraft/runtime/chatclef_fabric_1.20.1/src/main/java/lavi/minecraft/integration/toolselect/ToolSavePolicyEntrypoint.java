package lavi.minecraft.integration.toolselect;

import lavi.minecraft.integration.toolselect.snapshot.ToolSavePolicySnapshotPublisher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

//20260805_kpopmodder: Publish tool-save policy snapshots from the client thread before Baritone worker reads.
public final class ToolSavePolicyEntrypoint implements ModInitializer {
    private final ToolSavePolicySnapshotPublisher publisher = new ToolSavePolicySnapshotPublisher();

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(publisher::onEndClientTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> publisher.onClientStopping());
    }
}
