package adris.altoclef.mixins;

import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mixin(ScreenHandler.class)
public abstract class SlotClickMixin {

    //#if MC >= 11701
    @Redirect(
            method = "internalOnSlotClick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/ScreenHandler;internalOnSlotClick(IILnet/minecraft/screen/slot/SlotActionType;Lnet/minecraft/entity/player/PlayerEntity;)V")
    )
    private void slotClick(ScreenHandler self, int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // TODO: "self" is misleading, reread Mixin docs to understand the implications here.

        // This calculation is already done, BUT we also want a "before&after" type beat.

        List<Slot> afterSlots = self.slots;
        List<ItemStack> beforeStacks = new ArrayList<>(afterSlots.size());
        for (Slot slot : afterSlots) {
            beforeStacks.add(slot.getStack().copy());
        }
        Task exactDiagnosticLeaf = automaticDepositDiagnosticLeaf();
        boolean automaticDepositDiagnostics = exactDiagnosticLeaf != null;
        if (automaticDepositDiagnostics) {
            StoreDepositDiagnostics.beginAutomaticSlotAction(
                    exactDiagnosticLeaf,
                    self,
                    self.syncId,
                    slotIndex,
                    button,
                    actionType,
                    diagnosticCursorCopy(self)
            );
        }
        try {
            // Perform slot changes potentially
            self.onSlotClick(slotIndex, button, actionType, player);
            if (automaticDepositDiagnostics) {
                StoreDepositDiagnostics.observeAutomaticSlotActionReturn(
                        diagnosticCursorCopy(self)
                );
            }
            // Check for changes and alert
            int mutationOrdinal = 0;
            for (int i = 0; i < beforeStacks.size(); ++i) {
                ItemStack before = beforeStacks.get(i);
                ItemStack after = afterSlots.get(i).getStack();
                if (!ItemStack.areEqual(before, after)) {
                    adris.altoclef.util.slots.Slot slot = adris.altoclef.util.slots.Slot.getFromCurrentScreen(i);
                    if (automaticDepositDiagnostics) {
                        StoreDepositDiagnostics.beginAutomaticSlotMutation(
                                ++mutationOrdinal,
                                slot,
                                before,
                                after
                        );
                    }
                    //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
                    ContainerGuiDiagnostics.onLocalSlotMutation(self, i, before, after);
                    try {
                        EventBus.publish(new SlotClickChangedEvent(slot, before, after));
                    } finally {
                        if (automaticDepositDiagnostics) {
                            StoreDepositDiagnostics.endAutomaticSlotMutation();
                        }
                    }
                }
            }
        } finally {
            if (automaticDepositDiagnostics) {
                StoreDepositDiagnostics.endAutomaticSlotAction();
            }
        }
    }

    private static Task automaticDepositDiagnosticLeaf() {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return null;
        }
        try {
            Task exactLeaf = ChatClefDiagnostics.currentTaskForDiagnostics();
            return StoreDepositDiagnostics.hasAutomaticTaskContext(exactLeaf)
                    ? exactLeaf
                    : null;
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static ItemStack diagnosticCursorCopy(ScreenHandler handler) {
        try {
            ItemStack cursor = handler == null ? null : handler.getCursorStack();
            return cursor == null ? null : cursor.copy();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }
    //#endif

}
