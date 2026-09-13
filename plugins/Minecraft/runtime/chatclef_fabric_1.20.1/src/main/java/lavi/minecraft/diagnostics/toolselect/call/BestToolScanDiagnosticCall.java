package lavi.minecraft.diagnostics.toolselect.call;

import adris.altoclef.AltoClef;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.toolselect.BestToolSlotDiagnostics;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;

//20260913_kpopmodder: Protect both lazy scan initialization and later diagnostic callbacks.
public final class BestToolScanDiagnosticCall {
    private static final ToolDiagnosticInvocation INVOCATION =
            new ToolDiagnosticInvocation("best_tool_slot", () -> BestToolSlotDiagnostics.disableOwner("CALL_INITIALIZATION_OR_OBSERVATION_FAILED"));
    private final BestToolSlotDiagnostics.Scan scan;

    private BestToolScanDiagnosticCall(BestToolSlotDiagnostics.Scan scan) {
        this.scan = scan;
    }

    public static BestToolScanDiagnosticCall start(BlockState state) {
        return new BestToolScanDiagnosticCall(INVOCATION.call(() -> BestToolSlotDiagnostics.start(state), null));
    }

    public void observeToolCandidate(AltoClef mod, Slot slot, ItemStack stack, boolean suitable,
                                     boolean shouldSave, double speed, boolean becameBest) {
        if (scan != null) {
            INVOCATION.run(() -> scan.observeToolCandidate(mod, slot, stack, suitable, shouldSave, speed, becameBest));
        }
    }

    public void observeShearsCandidate(Slot slot, ItemStack stack, boolean effective, boolean selected) {
        if (scan != null) {
            INVOCATION.run(() -> scan.observeShearsCandidate(slot, stack, effective, selected));
        }
    }

    public void logReturn(Slot selected, String reason, double speed) {
        if (scan != null) {
            INVOCATION.run(() -> scan.logReturn(selected, reason, speed));
        }
    }
}
