package lavi.minecraft.diagnostics.inventory;

import lavi.minecraft.diagnostics.inventory.snapshot.InventoryFurnaceSlotsSnapshot;
import lavi.minecraft.diagnostics.inventory.snapshot.InventoryScreenDriftClassifier;
import lavi.minecraft.diagnostics.inventory.snapshot.InventorySnapshotValues;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

//20260805_kpopmodder: Snapshot screen and handler identity without invoking inventory trackers.
public final class InventoryScreenSnapshot {
    private final String screenClass;
    private final String screenIdentity;
    private final String playerHandlerClass;
    private final String playerHandlerIdentity;
    private final String playerHandlerSyncId;
    private final String playerHandlerSlotCount;
    private final String handledScreenHandlerClass;
    private final String handledScreenHandlerIdentity;
    private final String handledScreenHandlerSyncId;
    private final String screenHandlerMatchesPlayerHandler;
    private final String cursorItemId;
    private final String cursorItemCount;
    private final String cursorEmpty;
    private final InventoryFurnaceSlotsSnapshot furnaceSlots;

    private InventoryScreenSnapshot(String screenClass,
                                    String screenIdentity,
                                    String playerHandlerClass,
                                    String playerHandlerIdentity,
                                    String playerHandlerSyncId,
                                    String playerHandlerSlotCount,
                                    String handledScreenHandlerClass,
                                    String handledScreenHandlerIdentity,
                                    String handledScreenHandlerSyncId,
                                    String screenHandlerMatchesPlayerHandler,
                                    String cursorItemId,
                                    String cursorItemCount,
                                    String cursorEmpty,
                                    InventoryFurnaceSlotsSnapshot furnaceSlots) {
        this.screenClass = screenClass;
        this.screenIdentity = screenIdentity;
        this.playerHandlerClass = playerHandlerClass;
        this.playerHandlerIdentity = playerHandlerIdentity;
        this.playerHandlerSyncId = playerHandlerSyncId;
        this.playerHandlerSlotCount = playerHandlerSlotCount;
        this.handledScreenHandlerClass = handledScreenHandlerClass;
        this.handledScreenHandlerIdentity = handledScreenHandlerIdentity;
        this.handledScreenHandlerSyncId = handledScreenHandlerSyncId;
        this.screenHandlerMatchesPlayerHandler = screenHandlerMatchesPlayerHandler;
        this.cursorItemId = cursorItemId;
        this.cursorItemCount = cursorItemCount;
        this.cursorEmpty = cursorEmpty;
        this.furnaceSlots = furnaceSlots;
    }

    public static InventoryScreenSnapshot capture() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ClientPlayerEntity player = client == null ? null : client.player;
            Screen screen = client == null ? null : client.currentScreen;
            ScreenHandler playerHandler = player == null ? null : player.currentScreenHandler;
            ScreenHandler handledHandler = handledScreenHandler(screen);
            ItemStack cursorStack = playerHandler == null ? ItemStack.EMPTY : playerHandler.getCursorStack();
            return new InventoryScreenSnapshot(
                    InventorySnapshotValues.className(screen, "none"),
                    InventorySnapshotValues.identity(screen),
                    InventorySnapshotValues.className(playerHandler, "unavailable"),
                    InventorySnapshotValues.identity(playerHandler),
                    InventorySnapshotValues.syncId(playerHandler),
                    InventorySnapshotValues.slotCount(playerHandler),
                    InventorySnapshotValues.className(handledHandler, "not_applicable"),
                    InventorySnapshotValues.identity(handledHandler),
                    InventorySnapshotValues.syncId(handledHandler),
                    Boolean.toString(handledHandler != null && handledHandler == playerHandler),
                    InventorySnapshotValues.itemId(cursorStack),
                    InventorySnapshotValues.count(cursorStack),
                    Boolean.toString(cursorStack == null || cursorStack.isEmpty()),
                    InventoryFurnaceSlotsSnapshot.capture(playerHandler)
            );
        } catch (RuntimeException | LinkageError error) {
            String unavailable = "unavailable:" + error.getClass().getSimpleName();
            return new InventoryScreenSnapshot(
                    unavailable, unavailable, unavailable, unavailable, unavailable, unavailable,
                    unavailable, unavailable, unavailable, unavailable, unavailable, unavailable,
                    unavailable,
                    InventoryFurnaceSlotsSnapshot.unavailable(unavailable)
            );
        }
    }

    public String driftReason(InventoryScreenSnapshot current) {
        return InventoryScreenDriftClassifier.driftReason(this, current);
    }

    public String stableKey() {
        return screenClass + "|"
                + playerHandlerClass + "|"
                + playerHandlerSyncId + "|"
                + playerHandlerSlotCount + "|"
                + handledScreenHandlerClass + "|"
                + handledScreenHandlerSyncId + "|"
                + cursorItemId + "|"
                + cursorItemCount;
    }

    public Object[] beginFields(String suffix) {
        return merge(new Object[]{
                "screenClass" + suffix, screenClass,
                "screenIdentity" + suffix, screenIdentity,
                "playerHandlerClass" + suffix, playerHandlerClass,
                "playerHandlerIdentity" + suffix, playerHandlerIdentity,
                "playerHandlerSyncId" + suffix, playerHandlerSyncId,
                "playerHandlerSlotCount" + suffix, playerHandlerSlotCount,
                "handledScreenHandlerClass" + suffix, handledScreenHandlerClass,
                "handledScreenHandlerIdentity" + suffix, handledScreenHandlerIdentity,
                "handledScreenHandlerSyncId" + suffix, handledScreenHandlerSyncId,
                "screenHandlerMatchesPlayerHandler" + suffix, screenHandlerMatchesPlayerHandler,
                "cursorItemId" + suffix, cursorItemId,
                "cursorItemCount" + suffix, cursorItemCount,
                "cursorEmpty" + suffix, cursorEmpty
        }, furnaceSlots.fields(suffix));
    }

    public String screenClass() {
        return screenClass;
    }

    public String playerHandlerClass() {
        return playerHandlerClass;
    }

    public String playerHandlerIdentity() {
        return playerHandlerIdentity;
    }

    public String playerHandlerSyncId() {
        return playerHandlerSyncId;
    }

    public String playerHandlerSlotCount() {
        return playerHandlerSlotCount;
    }

    public String screenHandlerMatchesPlayerHandler() {
        return screenHandlerMatchesPlayerHandler;
    }

    private static ScreenHandler handledScreenHandler(Screen screen) {
        if (screen instanceof HandledScreen<?> handledScreen) {
            return handledScreen.getScreenHandler();
        }
        return null;
    }

    private static Object[] merge(Object[] first, Object[] second) {
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }
}
