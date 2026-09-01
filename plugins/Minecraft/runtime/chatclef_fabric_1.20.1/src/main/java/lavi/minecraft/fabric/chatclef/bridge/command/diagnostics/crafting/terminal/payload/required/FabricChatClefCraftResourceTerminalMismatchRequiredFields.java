package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required;

import lavi.minecraft.diagnostics.crafting.acquisition.target.mismatch.command.CraftResourceCommandMismatchSnapshot;

import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Own exact-command mismatch counters apart from session quota totals.
public final class FabricChatClefCraftResourceTerminalMismatchRequiredFields {
    private FabricChatClefCraftResourceTerminalMismatchRequiredFields() {
    }

    public static void append(
            Map<String, Object> fields,
            Optional<CraftResourceCommandMismatchSnapshot> mismatchSnapshot) {
        fields.put("mismatchAggregateScope", "EXACT_COMMAND_SCOPE");
        if (mismatchSnapshot.isEmpty()) {
            fields.put("commandOwnedMismatchOccurrenceCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchCoverageGapCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchDuplicateSuppressedCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchPerCorrelationLimitSuppressedCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchSessionLimitSuppressedCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchAdmissionDeniedCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchEmissionFailureCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchRetainedSignatureCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchUnownedObservationCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchUnknownAssociationCount", "UNAVAILABLE_SCOPE_NOT_ACTIVE");
            fields.put("commandMismatchCounterSaturated", false);
            return;
        }
        CraftResourceCommandMismatchSnapshot mismatch = mismatchSnapshot.get();
        fields.put(
                "commandOwnedMismatchOccurrenceCount",
                mismatch.ownedMismatchOccurrenceCount()
        );
        fields.put("commandMismatchCoverageGapCount", mismatch.coverageGapCount());
        fields.put(
                "commandMismatchDuplicateSuppressedCount",
                mismatch.duplicateSuppressedCount()
        );
        fields.put(
                "commandMismatchPerCorrelationLimitSuppressedCount",
                mismatch.perCorrelationLimitSuppressedCount()
        );
        fields.put(
                "commandMismatchSessionLimitSuppressedCount",
                mismatch.sessionLimitSuppressedCount()
        );
        fields.put(
                "commandMismatchAdmissionDeniedCount",
                mismatch.admissionDeniedCount()
        );
        fields.put(
                "commandMismatchEmissionFailureCount",
                mismatch.emissionFailureCount()
        );
        fields.put(
                "commandMismatchRetainedSignatureCount",
                mismatch.retainedSignatureCount()
        );
        fields.put("commandMismatchUnownedObservationCount", mismatch.unownedObservationCount());
        fields.put("commandMismatchUnknownAssociationCount", mismatch.unknownAssociationCount());
        fields.put("commandMismatchCounterSaturated", mismatch.counterSaturated());
    }
}
