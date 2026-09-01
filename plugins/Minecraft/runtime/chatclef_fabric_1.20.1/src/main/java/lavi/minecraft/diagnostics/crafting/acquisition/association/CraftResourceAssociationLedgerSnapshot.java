package lavi.minecraft.diagnostics.crafting.acquisition.association;

//20260901_kpopmodder: Expose bounded association accounting without retaining Task objects.
public record CraftResourceAssociationLedgerSnapshot(
        CraftResourceAssociationStatus currentStatus,
        long chainOwnerTransitionCount,
        long commandDescendantObservationCount,
        long unownedObservationCount,
        long unknownAssociationCount,
        long observationGapCount,
        String lastObservationGapBoundary,
        String lastObservationGapReason,
        boolean counterSaturated
) {
}
