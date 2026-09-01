package lavi.minecraft.diagnostics.crafting.acquisition.scope;

import java.util.Optional;

//20260901_kpopmodder: Return activation and captured requirement evidence without emitting behavior.
public record IronPickaxeAcquisitionActivation(
        IronPickaxeAcquisitionScopeDecision decision,
        Optional<IronPickaxeAcquisitionScopeBinding> binding,
        boolean requirementCaptureAvailable
) {
    public IronPickaxeAcquisitionActivation {
        binding = binding == null ? Optional.empty() : binding;
    }
}
