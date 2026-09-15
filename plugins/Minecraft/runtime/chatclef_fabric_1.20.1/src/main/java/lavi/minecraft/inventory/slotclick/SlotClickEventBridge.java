//#if MC == 12001
package lavi.minecraft.inventory.slotclick;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import lavi.minecraft.diagnostics.container.store.deposit.counter.SlotClickDiagnosticScope;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayList;
import java.util.List;

//20260916_kpopmodder: Own one client-local native click's existing slot-change publication contract.
public final class SlotClickEventBridge {
    private SlotClickEventBridge() {
    }

    public static void run(ScreenHandler handler, int slotIndex, int button, SlotActionType action,
                           PlayerEntity player, Runnable nativeClick) {
        // ScreenHandler is also used by the integrated server. Never publish its mutations to client trackers.
        if (!(player instanceof ClientPlayerEntity)) {
            nativeClick.run();
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || !client.isOnThread() || player != client.player
                || handler != player.currentScreenHandler) {
            nativeClick.run();
            return;
        }

        // Preserve the original producer's copied-before/live-after comparison and slot iteration order.
        List<Slot> slots = handler.slots;
        List<ItemStack> beforeStacks = new ArrayList<>(slots.size());
        for (Slot slot : slots) {
            beforeStacks.add(slot.getStack().copy());
        }

        SlotClickDiagnosticScope diagnostics = SlotClickDiagnosticScope.begin(handler, slotIndex, button, action);
        try {
            nativeClick.run();
            diagnostics.nativeReturned();
            int mutationOrdinal = 0;
            for (int i = 0; i < beforeStacks.size(); i++) {
                ItemStack before = beforeStacks.get(i);
                ItemStack after = slots.get(i).getStack();
                if (!ItemStack.areEqual(before, after)) {
                    adris.altoclef.util.slots.Slot slot = adris.altoclef.util.slots.Slot.getFromCurrentScreen(i);
                    diagnostics.mutationStarted(++mutationOrdinal, i, slot, before, after);
                    try {
                        EventBus.publish(new SlotClickChangedEvent(slot, before, after));
                    } finally {
                        diagnostics.mutationEnded();
                    }
                }
            }
        } finally {
            // Diagnostic cleanup never catches the native operation or the existing EventBus exception path.
            diagnostics.close();
        }
    }
}
//#endif
