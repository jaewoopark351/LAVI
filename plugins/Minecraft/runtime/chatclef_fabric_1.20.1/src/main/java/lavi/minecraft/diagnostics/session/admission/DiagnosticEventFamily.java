package lavi.minecraft.diagnostics.session.admission;

public enum DiagnosticEventFamily {
    //#if MC == 12001
    //20260913_kpopmodder: Separate new behavior evidence from ordinary repeats and the historical diagnostic pools.
    TOOL_EQUIP_FIRST(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.TOOL_EQUIP_FIRST_SLOTS, 1, false),
    TOOL_PLACEMENT_TERMINAL(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.TOOL_PLACEMENT_TERMINAL_SLOTS, 1, false),
    BLOCK_PROTECTION_BOUNDARY(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.BLOCK_PROTECTION_BOUNDARY_SLOTS, 1, false),
    //#endif
    RESOURCE_MINING_FIRST(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.RESOURCE_FIRST_SLOTS_PER_DOMAIN, 1, false),
    RESOURCE_DEPOSIT_FIRST(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.RESOURCE_FIRST_SLOTS_PER_DOMAIN, 1, false),
    RESOURCE_BUILDER_FIRST(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.RESOURCE_FIRST_SLOTS_PER_DOMAIN, 1, false),
    RESOURCE_OBSERVATION_TERMINAL(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.RESOURCE_TERMINAL_SLOTS, 1, false),
    RESOURCE_MINING_SUMMARY(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.RESOURCE_SUMMARY_SLOTS_PER_DOMAIN, 1, false),
    RESOURCE_DEPOSIT_SUMMARY(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.RESOURCE_SUMMARY_SLOTS_PER_DOMAIN, 1, false),
    RESOURCE_BUILDER_SUMMARY(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.RESOURCE_SUMMARY_SLOTS_PER_DOMAIN, 1, false),
    BLOCK_COLLECTION_FIRST(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.BLOCK_COLLECTION_FIRST_SLOTS, 1, false),
    BLOCK_COLLECTION_SUMMARY(DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.BLOCK_COLLECTION_SUMMARY_SLOTS, 1, false),
    ORDINARY_DETAIL(
            DiagnosticAdmissionTier.ORDINARY,
            DiagnosticSessionLimits.ORDINARY_CEILING,
            1,
            false),
    CANONICAL_CAP(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.CANONICAL_CAP_SLOTS,
            1,
            true),
    FINAL_SNAPSHOT(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.FINAL_SNAPSHOT_SLOTS,
            1,
            true),
    ABNORMAL_STORE_TERMINAL(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.ABNORMAL_STORE_TERMINAL_SLOTS,
            DiagnosticSessionLimits.TERMINAL_GROUP_SIZE,
            false),
    ROUTINE_STORE_TERMINAL(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.ROUTINE_STORE_TERMINAL_SLOTS,
            DiagnosticSessionLimits.TERMINAL_GROUP_SIZE,
            false),
    EXCEPTION_COVERAGE(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.EXCEPTION_COVERAGE_SLOTS,
            1,
            false),
    AGGREGATE_CHECKPOINT(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.AGGREGATE_CHECKPOINT_SLOTS,
            1,
            false),
    NON_STORE_TERMINAL(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.NON_STORE_TERMINAL_SLOTS,
            1,
            false),
    SUPPRESSION_CONTROL(
            DiagnosticAdmissionTier.CRITICAL,
            DiagnosticSessionLimits.SUPPRESSION_CONTROL_SLOTS,
            1,
            false);

    private final DiagnosticAdmissionTier tier;
    private final int slotQuota;
    private final int admissionUnitSlots;
    private final boolean reservedInternalFamily;

    DiagnosticEventFamily(DiagnosticAdmissionTier tier,
                          int slotQuota,
                          int admissionUnitSlots,
                          boolean reservedInternalFamily) {
        this.tier = tier;
        this.slotQuota = slotQuota;
        this.admissionUnitSlots = admissionUnitSlots;
        this.reservedInternalFamily = reservedInternalFamily;
    }

    public DiagnosticAdmissionTier tier() {
        return tier;
    }

    public int slotQuota() {
        return slotQuota;
    }

    public int admissionUnitSlots() {
        return admissionUnitSlots;
    }

    public boolean reservedInternalFamily() {
        return reservedInternalFamily;
    }

    public boolean groupFamily() {
        return admissionUnitSlots == DiagnosticSessionLimits.TERMINAL_GROUP_SIZE;
    }
}
