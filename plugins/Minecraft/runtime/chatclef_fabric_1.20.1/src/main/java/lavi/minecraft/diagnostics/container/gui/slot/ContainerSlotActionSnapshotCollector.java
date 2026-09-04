package lavi.minecraft.diagnostics.container.gui.slot;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

//20260904_kpopmodder: Capture the request immediately before the unchanged controller call.
public final class ContainerSlotActionSnapshotCollector {
    private final ContainerItemCountSnapshotCollector counts =
            new ContainerItemCountSnapshotCollector();

    public ContainerSlotActionObservation capture(
            ScreenHandler handler,
            int syncId,
            int windowSlot,
            int button,
            SlotActionType actionType,
            ClientPlayerEntity player,
            long gameTick) {
        return new ContainerSlotActionObservation(
                identity(handler),
                handler == null ? "none" : handler.getClass().getName(),
                syncId,
                windowSlot,
                button,
                actionType == null ? "unavailable" : actionType.name(),
                counts.capture(handler, player, windowSlot, null),
                gameTick
        );
    }

    public ContainerItemCountSnapshot after(
            ScreenHandler handler,
            ClientPlayerEntity player,
            ContainerSlotActionObservation action) {
        return counts.capture(
                handler,
                player,
                action == null ? -1 : action.windowSlot(),
                action == null || action.before() == null ? null : action.before().focusItem()
        );
    }

    private static String identity(Object value) {
        return value == null
                ? "unavailable"
                : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}
