package lavi.minecraft.diagnostics.interaction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

//20260805_kpopmodder: Keep screen state reads separate from block-interaction attribution.
public final class BlockInteractionScreenSnapshot {
    private final String screenName;
    private final String screenHandlerName;
    private final String screenHandlerSyncId;

    private BlockInteractionScreenSnapshot(String screenName, String screenHandlerName, String screenHandlerSyncId) {
        this.screenName = screenName;
        this.screenHandlerName = screenHandlerName;
        this.screenHandlerSyncId = screenHandlerSyncId;
    }

    public static BlockInteractionScreenSnapshot current(MinecraftClient client, ClientPlayerEntity player) {
        return new BlockInteractionScreenSnapshot(
                screenName(client),
                screenHandlerName(player),
                screenHandlerSyncId(player)
        );
    }

    public String screenName() {
        return screenName;
    }

    public String screenHandlerName() {
        return screenHandlerName;
    }

    public String screenHandlerSyncId() {
        return screenHandlerSyncId;
    }

    private static String screenName(MinecraftClient client) {
        return client == null || client.currentScreen == null
                ? "none"
                : client.currentScreen.getClass().getSimpleName();
    }

    private static String screenHandlerName(ClientPlayerEntity player) {
        return player == null || player.currentScreenHandler == null
                ? "unavailable"
                : player.currentScreenHandler.getClass().getSimpleName();
    }

    private static String screenHandlerSyncId(ClientPlayerEntity player) {
        return player == null || player.currentScreenHandler == null
                ? "unavailable"
                : Integer.toString(player.currentScreenHandler.syncId);
    }
}
