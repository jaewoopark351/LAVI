package lavi.minecraft.diagnostics.container.store.deposit.candidate;

//20260826_kpopmodder: Name the exact raw-candidate predicate observation without changing candidate selection.
public enum StoreContainerRawCandidateOutcome {
    RAW_ACCEPTED,
    RAW_REJECTED,
    RAW_NOT_VISITED_BY_FILTERED_SCAN,
    RAW_UNAVAILABLE,
    FILTERED_SCAN_DID_NOT_COMPLETE
}
