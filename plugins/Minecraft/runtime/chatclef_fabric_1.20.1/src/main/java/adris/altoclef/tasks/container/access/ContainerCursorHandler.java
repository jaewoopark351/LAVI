package adris.altoclef.tasks.container.access;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Optional;

//20260729_kpopmodder: Added this helper to isolate cursor cleanup before opening containers.
public final class ContainerCursorHandler {

    private final StateChangeLogger debugLogger;

    public ContainerCursorHandler(StateChangeLogger debugLogger) {
        this.debugLogger = debugLogger;
    }

    public boolean hasCursorItem() {
        return !StorageHelper.getItemStackInCursorSlot().isEmpty();
    }

    public Task handleCursorItem(AltoClef mod) {
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        debugLogger.state("clear cursor before opening container: cursor="
                + ContainerTaskDiagnostics.describeStack(cursorStack));
        Optional<Slot> toMoveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
        if (toMoveTo.isEmpty()) {
            return new EnsureFreeInventorySlotTask();
        }
        if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return null;
        }
        mod.getSlotHandler().clickSlot(toMoveTo.get(), 0, SlotActionType.PICKUP);
        return null;
    }
}
