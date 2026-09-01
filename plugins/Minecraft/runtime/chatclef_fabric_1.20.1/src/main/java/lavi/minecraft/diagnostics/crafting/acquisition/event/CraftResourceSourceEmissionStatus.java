package lavi.minecraft.diagnostics.crafting.acquisition.event;

//20260901_kpopmodder: Distinguish an observed source boundary from its physical log outcome.
public enum CraftResourceSourceEmissionStatus {
    EMISSION_CALLS_RETURNED,
    SOURCE_DETAIL_NOT_EMITTED;

    public static CraftResourceSourceEmissionStatus from(boolean sourceEmissionCompleted) {
        return sourceEmissionCompleted
                ? EMISSION_CALLS_RETURNED
                : SOURCE_DETAIL_NOT_EMITTED;
    }
}
