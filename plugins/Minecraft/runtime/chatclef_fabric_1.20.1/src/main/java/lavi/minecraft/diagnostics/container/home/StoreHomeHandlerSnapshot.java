package lavi.minecraft.diagnostics.container.home;

import adris.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.ScreenHandler;

import java.util.Objects;

//20260828_kpopmodder: Capture bounded screen, handler, cursor, and world-time evidence.
public final class StoreHomeHandlerSnapshot {
    private final String screenClass;
    private final String handlerClass;
    private final String handlerIdentity;
    private final int syncId;
    private final int handlerSlotCount;
    private final StoreHomeStackIdentitySnapshot cursor;
    private final long worldTime;
    private final String captureStatus;
    private final String errorClass;

    private StoreHomeHandlerSnapshot(
            String screenClass,
            String handlerClass,
            String handlerIdentity,
            int syncId,
            int handlerSlotCount,
            StoreHomeStackIdentitySnapshot cursor,
            long worldTime,
            String captureStatus,
            String errorClass) {
        this.screenClass = Objects.requireNonNull(screenClass, "screenClass");
        this.handlerClass = Objects.requireNonNull(handlerClass, "handlerClass");
        this.handlerIdentity = Objects.requireNonNull(handlerIdentity, "handlerIdentity");
        this.syncId = syncId;
        this.handlerSlotCount = Math.max(0, handlerSlotCount);
        this.cursor = Objects.requireNonNull(cursor, "cursor");
        this.worldTime = worldTime;
        this.captureStatus = Objects.requireNonNull(captureStatus, "captureStatus");
        this.errorClass = Objects.requireNonNull(errorClass, "errorClass");
    }

    public static StoreHomeHandlerSnapshot capture(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return unavailable("player_unavailable");
        }
        ScreenHandler handler = mod.getPlayer().currentScreenHandler;
        Object screen = MinecraftClient.getInstance().currentScreen;
        StoreHomeStackIdentitySnapshot cursor = handler == null
                ? StoreHomeStackIdentitySnapshot.unavailable("handler_unavailable")
                : StoreHomeStackIdentitySnapshot.captureActual(handler.getCursorStack());
        String status = "complete".equals(cursor.captureStatus()) ? "complete" : "partial";
        return new StoreHomeHandlerSnapshot(
                className(screen),
                className(handler),
                identity(handler),
                handler == null ? -1 : handler.syncId,
                handler == null ? 0 : handler.slots.size(),
                cursor,
                mod.getWorld() == null ? -1L : mod.getWorld().getTime(),
                status,
                cursor.errorClass()
        );
    }

    public static StoreHomeHandlerSnapshot unavailable(String errorClass) {
        return new StoreHomeHandlerSnapshot(
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                -1,
                0,
                StoreHomeStackIdentitySnapshot.unavailable(errorClass),
                -1L,
                "partial",
                errorClass == null ? "unknown" : errorClass
        );
    }

    private static String className(Object value) {
        return value == null ? "none" : value.getClass().getName();
    }

    private static String identity(Object value) {
        return value == null
                ? "none"
                : value.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(value));
    }

    public String screenClass() {
        return screenClass;
    }

    public String handlerClass() {
        return handlerClass;
    }

    public String handlerIdentity() {
        return handlerIdentity;
    }

    public Object syncIdValue() {
        return syncId >= 0 ? syncId : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object handlerSlotCountValue() {
        return syncId >= 0 ? handlerSlotCount : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public StoreHomeStackIdentitySnapshot cursor() {
        return cursor;
    }

    public Object worldTimeValue() {
        return worldTime >= 0 ? worldTime : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public String captureStatus() {
        return captureStatus;
    }

    public String errorClass() {
        return errorClass;
    }
}
