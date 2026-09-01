package lavi.minecraft.diagnostics.session.lifecycle;

import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionLimits;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;

//20260831_kpopmodder: Project one admitted final-session snapshot into bounded event fields.
public final class DiagnosticSessionSnapshotEventFields {
    private DiagnosticSessionSnapshotEventFields() {
    }

    public static Object[] requiredFields(DiagnosticSessionSnapshot snapshot,
                                          String snapshotKind) {
        return new Object[]{
                "diagnosticSessionId", snapshot.diagnosticSessionId(),
                "snapshotKind", snapshotKind,
                "snapshotSelfAccounting", "ADMISSION_INCLUDED_EMISSION_OUTCOME_EXCLUDED",
                "hardCap", DiagnosticSessionLimits.HARD_CAP,
                "ordinaryBudget", DiagnosticSessionLimits.ORDINARY_CEILING,
                "criticalReserve", DiagnosticSessionLimits.CRITICAL_RESERVE,
                "admittedRequests", snapshot.admittedRequests(),
                "admittedSlots", snapshot.admittedSlots(),
                "ordinarySlotsUsed", snapshot.ordinarySlotsUsed(),
                "criticalSlotsUsed", snapshot.criticalSlotsUsed(),
                "criticalReserveRemaining", snapshot.criticalReserveRemaining(),
                "suppressedRequests", snapshot.suppressedRequests(),
                "emissionPending", snapshot.emissionPending(),
                "emissionInProgress", snapshot.emissionInProgress(),
                "emissionCompleted", snapshot.emissionCompleted(),
                "emissionFailedAfterAdmission", snapshot.emissionFailedAfterAdmission(),
                "capEventClaimed", snapshot.capEventClaimed(),
                "capTrigger", snapshot.capTrigger(),
                "finalSnapshotAdmitted", snapshot.finalSnapshotAdmitted(),
                "lastTokenSequence", snapshot.lastTokenSequence(),
                "tokenSequenceAvailable", snapshot.tokenSequenceAvailable(),
                "counterSaturated", snapshot.counterSaturated(),
                "familyCounters", snapshot.familyCounters(),
                "deliveryStatus", "UNVERIFIED",
                "behavior_effect", "none"
        };
    }

    public static Object[] fields(DiagnosticSessionSnapshot snapshot,
                                  String snapshotKind,
                                  Object[] lifecycleFields) {
        Object[] sessionFields = requiredFields(snapshot, snapshotKind);
        if (lifecycleFields == null || lifecycleFields.length == 0) {
            return sessionFields;
        }
        Object[] merged = new Object[sessionFields.length + lifecycleFields.length];
        System.arraycopy(sessionFields, 0, merged, 0, sessionFields.length);
        System.arraycopy(lifecycleFields, 0, merged, sessionFields.length, lifecycleFields.length);
        return merged;
    }
}
