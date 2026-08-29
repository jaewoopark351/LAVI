package lavi.minecraft.task.container.home.execution.transfer.click;

import adris.altoclef.AltoClef;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

//20260829_kpopmodder: Read only the cursor and controller readiness for an exact quick-move.
public final class HomeStorageQuickMoveReadinessChecker {
    public HomeStorageQuickMoveReadiness cursor(ScreenHandler handler) {
        ItemStack cursor = handler == null ? ItemStack.EMPTY : handler.getCursorStack();
        return cursor == null || cursor.isEmpty()
                ? HomeStorageQuickMoveReadiness.READY
                : HomeStorageQuickMoveReadiness.CURSOR_NOT_EMPTY;
    }

    public HomeStorageQuickMoveReadiness slotAction(AltoClef mod) {
        return mod.getSlotHandler() != null
                && mod.getController() != null
                && mod.getSlotHandler().canDoSlotAction()
                ? HomeStorageQuickMoveReadiness.READY
                : HomeStorageQuickMoveReadiness.SLOT_ACTION_DELAY;
    }
}
