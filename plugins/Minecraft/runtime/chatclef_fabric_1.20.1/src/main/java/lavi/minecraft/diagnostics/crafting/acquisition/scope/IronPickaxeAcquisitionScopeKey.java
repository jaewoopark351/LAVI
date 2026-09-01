package lavi.minecraft.diagnostics.crafting.acquisition.scope;

//20260901_kpopmodder: Keep one opaque command/root identity for bounded iron-pickaxe diagnostics.
public record IronPickaxeAcquisitionScopeKey(
        String commandSessionId,
        long commandConnectionGeneration,
        String commandRequestId,
        String commandCorrelationId,
        String rootAssignmentId,
        long rootGeneration,
        String boundRootTaskInstanceId
) {
}
