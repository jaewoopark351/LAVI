package lavi.minecraft.diagnostics.session.admission;

public enum DiagnosticEventFamily {
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
