package lavi.minecraft.diagnostics.toolselect.call;

import lavi.minecraft.diagnostics.toolselect.ToolSavePolicySnapshotDiagnostics;
import lavi.minecraft.integration.toolselect.snapshot.ToolSavePolicySnapshot;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

//20260913_kpopmodder: Shield diagnostic publication/consumption without guarding the tool-save policy itself.
public final class ToolSavePolicyDiagnosticCall {
    private static final ToolDiagnosticInvocation INVOCATION =
            new ToolDiagnosticInvocation("tool_save_policy_snapshot", () -> ToolSavePolicySnapshotDiagnostics.disableOwner("CALL_INITIALIZATION_OR_OBSERVATION_FAILED"));

    private ToolSavePolicyDiagnosticCall() { }

    public static void logPublished(ToolSavePolicySnapshot snapshot) {
        INVOCATION.run(() -> ToolSavePolicySnapshotDiagnostics.logPublished(snapshot));
    }

    public static void logConsumed(ToolSavePolicySnapshot snapshot, Block block, ItemStack stack,
                                   String decisionReason, boolean shouldSave) {
        INVOCATION.run(() -> ToolSavePolicySnapshotDiagnostics.logConsumed(snapshot, block, stack, decisionReason, shouldSave));
    }
}
