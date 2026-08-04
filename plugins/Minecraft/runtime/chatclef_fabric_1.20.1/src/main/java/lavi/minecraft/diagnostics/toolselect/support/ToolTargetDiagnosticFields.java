package lavi.minecraft.diagnostics.toolselect.support;

import adris.altoclef.util.MiningRequirement;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.BlockState;

//20260805_kpopmodder: Share tool target diagnostic field formatting without changing emitted log keys.
public final class ToolTargetDiagnosticFields {
    private ToolTargetDiagnosticFields() {
    }

    public static String blockState(BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> targetState);
    }

    public static String blockId(BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.getBlock());
    }

    public static String requiresTool(BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.isToolRequired());
    }

    public static String minimumMiningRequirement(BlockState targetState) {
        return ChatClefDiagnostics.safeValue(
                () -> targetState == null ? null : MiningRequirement.getMinimumRequirementForBlock(targetState.getBlock())
        );
    }
}
