package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;

import java.util.Objects;
import java.util.Set;

//20260901_kpopmodder: Expose one immutable view of bounded target diagnostic state.
public record CraftResourceTargetDiagnosticsRegistrySnapshot(
        int activeScopeCount,
        Set<IronPickaxeAcquisitionScopeKey> activeScopeKeys,
        CraftResourceMismatchSnapshot mismatchSnapshot,
        CraftResourceExceptionSnapshot exceptionSnapshot) {

    public CraftResourceTargetDiagnosticsRegistrySnapshot {
        activeScopeKeys = Set.copyOf(Objects.requireNonNull(
                activeScopeKeys,
                "activeScopeKeys"
        ));
        mismatchSnapshot = Objects.requireNonNull(mismatchSnapshot, "mismatchSnapshot");
        exceptionSnapshot = Objects.requireNonNull(exceptionSnapshot, "exceptionSnapshot");
    }
}
