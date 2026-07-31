package lavi.minecraft.integration.carryon.snapshot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

//20260731_kpopmodder: Keep screen/container state reads separate from player pose snapshots.
public final class CarryOnScreenSnapshotCollector {
    private CarryOnScreenSnapshotCollector() {
    }

    public static CarryOnScreenSnapshot collect(MinecraftClient client, ClientPlayerEntity player) {
        return new CarryOnScreenSnapshot(
                screenName(client),
                screenHandlerName(player),
                screenHandlerSyncId(player),
                cursorStack(player)
        );
    }

    private static String screenName(MinecraftClient client) {
        if (client == null || client.currentScreen == null) {
            return "none";
        }
        return client.currentScreen.getClass().getSimpleName();
    }

    private static String screenHandlerName(ClientPlayerEntity player) {
        if (player == null || player.currentScreenHandler == null) {
            return "unavailable";
        }
        return player.currentScreenHandler.getClass().getSimpleName();
    }

    private static String screenHandlerSyncId(ClientPlayerEntity player) {
        if (player == null || player.currentScreenHandler == null) {
            return "unavailable";
        }
        return Integer.toString(player.currentScreenHandler.syncId);
    }

    private static String cursorStack(ClientPlayerEntity player) {
        if (player == null || player.currentScreenHandler == null) {
            return "unavailable";
        }
        ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
        return cursorStack == null ? "unavailable" : String.valueOf(cursorStack);
    }
}
