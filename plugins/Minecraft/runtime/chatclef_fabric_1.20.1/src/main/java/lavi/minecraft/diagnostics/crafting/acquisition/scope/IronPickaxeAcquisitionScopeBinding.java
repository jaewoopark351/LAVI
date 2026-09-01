package lavi.minecraft.diagnostics.crafting.acquisition.scope;

import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementDecision;

import java.util.Objects;

//20260901_kpopmodder: Bind immutable command/root evidence to one active diagnostic scope.
public record IronPickaxeAcquisitionScopeBinding(
        IronPickaxeAcquisitionScopeKey key,
        Object boundRootTask,
        String rootTaskClass,
        CraftResourceRequirementDecision requirement,
        long activatedAtClientTick,
        long activatedAtMonotonicNanos
) {
    public IronPickaxeAcquisitionScopeBinding {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(requirement, "requirement");
        rootTaskClass = rootTaskClass == null || rootTaskClass.isBlank()
                ? "UNAVAILABLE"
                : rootTaskClass;
    }
}
