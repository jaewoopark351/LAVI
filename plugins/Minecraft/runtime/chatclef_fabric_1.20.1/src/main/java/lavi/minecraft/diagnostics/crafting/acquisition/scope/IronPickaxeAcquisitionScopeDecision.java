package lavi.minecraft.diagnostics.crafting.acquisition.scope;

//20260901_kpopmodder: Report fail-closed scope activation without changing command execution.
public record IronPickaxeAcquisitionScopeDecision(
        boolean activated,
        String reason,
        IronPickaxeAcquisitionScopeKey key
) {
}
