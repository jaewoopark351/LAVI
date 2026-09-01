package lavi.minecraft.diagnostics.crafting.acquisition.scope;

//20260901_kpopmodder: Correlate a pre-root quantity capture with its later root activation.
public record IronPickaxeAcquisitionCommandKey(
        String commandSessionId,
        long commandConnectionGeneration,
        String commandRequestId,
        String commandCorrelationId
) {
}
