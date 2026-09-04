package lavi.minecraft.task.container.home.execution.transfer.click;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import lavi.minecraft.diagnostics.container.gui.slot.ContainerSlotActionProbe;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

//20260829_kpopmodder: Issue only one exact QUICK_MOVE through the existing controller owner.
public final class HomeStorageQuickMoveIssuer {
    public HomeStorageQuickMoveOutcome issue(
            AltoClef mod,
            ScreenHandler handler,
            int sourceWindowSlot) {
        ContainerSlotActionProbe containerProbe = ContainerSlotActionProbe.noop();
        try {
            mod.getSlotHandler().registerSlotAction();
            containerProbe = ContainerGuiDiagnostics.beginSlotAction(
                    handler,
                    handler.syncId,
                    sourceWindowSlot,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mod.getPlayer()
            );
            mod.getController().clickSlot(
                    handler.syncId,
                    sourceWindowSlot,
                    0,
                    SlotActionType.QUICK_MOVE,
                    mod.getPlayer()
            );
            containerProbe.returned();
            return HomeStorageQuickMoveOutcome.success();
        } catch (RuntimeException exception) {
            containerProbe.failed(exception);
            return HomeStorageQuickMoveOutcome.failure(
                    "slot_click_exception",
                    exception.getClass().getSimpleName()
            );
        }
    }
}
