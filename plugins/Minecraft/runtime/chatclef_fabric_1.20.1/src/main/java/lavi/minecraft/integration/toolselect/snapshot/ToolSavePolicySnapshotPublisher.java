package lavi.minecraft.integration.toolselect.snapshot;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.toolselect.ToolSavePolicySnapshotDiagnostics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;

//20260805_kpopmodder: Keep Minecraft inventory reads on the client thread and publish only immutable facts.
public final class ToolSavePolicySnapshotPublisher {
    private long publishedClientTick;
    private long generation;
    private String lastContextKey = "STARTUP";

    public void onEndClientTick(MinecraftClient client) {
        publishedClientTick++;
        AltoClef mod = AltoClef.getInstance();
        if (client == null) {
            publishUnavailable("NO_CLIENT", "unavailable", "unavailable");
            return;
        }
        if (client.player == null || client.world == null) {
            publishUnavailable("NO_WORLD_OR_PLAYER", worldKey(client), playerKey(client));
            return;
        }
        if (mod == null || mod.getItemStorage() == null) {
            publishUnavailable("NO_ALTOCLEF_ITEM_STORAGE", worldKey(client), playerKey(client));
            return;
        }

        String worldKey = worldKey(client);
        String playerKey = playerKey(client);
        updateGeneration("READY|" + worldKey + "|" + playerKey);
        boolean hasDiamondPickaxe = mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE);
        ToolSavePolicySnapshot snapshot = ToolSavePolicySnapshot.ready(
                generation,
                publishedClientTick,
                hasDiamondPickaxe,
                worldKey,
                playerKey
        );
        ToolSavePolicySnapshotProvider.publish(snapshot);
        ToolSavePolicySnapshotDiagnostics.logPublished(snapshot);
    }

    public void onClientStopping() {
        publishedClientTick++;
        updateGeneration("CLIENT_STOPPING");
        ToolSavePolicySnapshot snapshot = ToolSavePolicySnapshot.unavailable(
                generation,
                publishedClientTick,
                "CLIENT_STOPPING",
                "unavailable",
                "unavailable"
        );
        ToolSavePolicySnapshotProvider.publish(snapshot);
        ToolSavePolicySnapshotDiagnostics.logPublished(snapshot);
    }

    private void publishUnavailable(String status, String worldKey, String playerKey) {
        updateGeneration(status + "|" + worldKey + "|" + playerKey);
        ToolSavePolicySnapshot snapshot = ToolSavePolicySnapshot.unavailable(
                generation,
                publishedClientTick,
                status,
                worldKey,
                playerKey
        );
        ToolSavePolicySnapshotProvider.publish(snapshot);
        ToolSavePolicySnapshotDiagnostics.logPublished(snapshot);
    }

    private void updateGeneration(String contextKey) {
        if (!contextKey.equals(lastContextKey)) {
            generation++;
            lastContextKey = contextKey;
        }
    }

    private String worldKey(MinecraftClient client) {
        if (client == null || client.world == null) {
            return "unavailable";
        }
        return client.world.getRegistryKey().getValue()
                + "#identity=" + Integer.toHexString(System.identityHashCode(client.world));
    }

    private String playerKey(MinecraftClient client) {
        if (client == null || client.player == null) {
            return "unavailable";
        }
        return client.player.getUuidAsString()
                + "#identity=" + Integer.toHexString(System.identityHashCode(client.player));
    }
}
