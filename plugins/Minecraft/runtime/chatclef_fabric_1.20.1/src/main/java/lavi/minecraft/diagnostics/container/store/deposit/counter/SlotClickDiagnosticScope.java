//#if MC == 12001
package lavi.minecraft.diagnostics.container.store.deposit.counter;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

//20260916_kpopmodder: Keep diagnostic binding and cleanup local to a single native slot action.
public final class SlotClickDiagnosticScope implements AutoCloseable {
    private final ScreenHandler handler;
    private final int slotIndex;
    private final int button;
    private final SlotActionType action;
    private boolean automatic;
    private boolean returned;

    private SlotClickDiagnosticScope(ScreenHandler handler, int slotIndex, int button, SlotActionType action) {
        this.handler = handler;
        this.slotIndex = slotIndex;
        this.button = button;
        this.action = action;
    }

    public static SlotClickDiagnosticScope begin(ScreenHandler handler, int slotIndex, int button,
                                                SlotActionType action) {
        SlotClickDiagnosticScope scope = new SlotClickDiagnosticScope(handler, slotIndex, button, action);
        scope.observe(() -> {
            StoreDepositDiagnostics.observeCounterProducer("OUTER_REDIRECT_ENTER", handler, slotIndex, button, action);
            if (!ChatClefDiagnostics.isBoundaryEnabled()) return;
            Task leaf = ChatClefDiagnostics.currentTaskForDiagnostics();
            if (leaf != null && StoreDepositDiagnostics.hasAutomaticTaskContext(leaf)) {
                scope.automatic = true;
                StoreDepositDiagnostics.beginAutomaticSlotAction(
                        leaf, handler, handler.syncId, slotIndex, button, action, cursorCopy(handler));
            }
        });
        return scope;
    }

    public void nativeReturned() {
        returned = true;
        observe(() -> {
            if (automatic) StoreDepositDiagnostics.observeAutomaticSlotActionReturn(cursorCopy(handler));
            StoreDepositDiagnostics.observeCounterProducer("OUTER_ACTION_RETURN", handler, slotIndex, button, action);
        });
    }

    public void mutationStarted(int ordinal, int windowSlot, Slot slot, ItemStack before, ItemStack after) {
        observe(() -> {
            if (automatic) StoreDepositDiagnostics.beginAutomaticSlotMutation(ordinal, slot, before, after);
            ContainerGuiDiagnostics.onLocalSlotMutation(handler, windowSlot, before, after);
        });
    }

    public void mutationEnded() {
        observe(() -> {
            if (automatic) StoreDepositDiagnostics.endAutomaticSlotMutation();
        });
    }

    @Override
    public void close() {
        // Separate attempts so failure to emit an exceptional boundary cannot prevent diagnostic-scope cleanup.
        observe(() -> {
            if (!returned) StoreDepositDiagnostics.observeCounterProducer(
                    "OUTER_ACTION_NOT_RETURNED", handler, slotIndex, button, action);
        });
        observe(() -> {
            if (automatic) StoreDepositDiagnostics.endAutomaticSlotAction();
        });
    }

    private void observe(Runnable observation) {
        try {
            observation.run();
        } catch (RuntimeException | LinkageError observationFailure) {
            // Only diagnostic operations enter this guard. Gameplay and event delivery never do.
            try {
                ObservationDiagnostics.captureFailed("deposit", "SLOT_CLICK_DIAGNOSTIC_UNAVAILABLE");
            } catch (RuntimeException | LinkageError unavailableFailureChannel) {
                // Existing bounded failure reporting is unavailable; never retry through the same sink.
            }
        }
    }

    private static ItemStack cursorCopy(ScreenHandler handler) {
        ItemStack cursor = handler.getCursorStack();
        return cursor == null ? null : cursor.copy();
    }
}
//#endif
