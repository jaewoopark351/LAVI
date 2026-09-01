package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.optional;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationLedgerSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress.CraftResourceRequirementProgressSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.failure.CraftResourceFailureAggregateSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalScopeBinding;

import java.util.LinkedHashMap;
import java.util.Map;

import static lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadValue.unavailableIfBlank;
import static lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadValue.value;

//20260901_kpopmodder: Own supplemental terminal detail that may be dropped under the cap.
public final class FabricChatClefCraftResourceTerminalOptionalFields {
    private FabricChatClefCraftResourceTerminalOptionalFields() {
    }

    public static Map<String, Object> assemble(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            FabricChatClefCraftResourceTerminalEvidence evidence,
            CraftResourceTerminalSnapshot snapshot) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("associationStatusAtActivation", "COMMAND_ROOT_DESCENDANT");
        fields.put("terminalAssociationBasis", "EXACT_CONTEXT_AND_ROOT_ASSIGNMENT_BINDING");
        fields.put(
                "primaryCauseExceptionType",
                unavailableIfBlank(binding.primaryCauseExceptionType())
        );
        fields.put(
                "primaryCauseExceptionMessage",
                unavailableIfBlank(binding.primaryCauseExceptionMessage())
        );
        fields.put("resultSendInFlight", snapshot.resultSendInFlight());
        fields.put("lifecycleClearKind", snapshot.lifecycleClearKind().name());
        fields.put(
                "lifecycleClearEvidenceStatus",
                snapshot.lifecycleCleared()
                        ? "OBSERVED_AUTHORITATIVE_CLEAR_RESULT"
                        : snapshot.connectionDetachedObserved()
                                ? "UNAVAILABLE_DETACH_EVENT_DOES_NOT_EXPOSE_CLEAR_CAS_RESULT"
                                : "UNAVAILABLE_CLEAR_RESULT_NOT_OBSERVED"
        );
        fields.put("terminalElapsedNanos", snapshot.elapsedNanos());
        fields.put("terminalAdmissionRequestCount", snapshot.terminalAdmissionRequestCount());
        fields.put("lateOrDuplicateSignalCount", snapshot.lateOrDuplicateSignalCount());
        fields.put("terminalCounterSaturated", snapshot.counterSaturated());
        fields.put(
                "blockOptionalMetaEvidenceStatus",
                "UNAVAILABLE_SOURCE_NOT_PRESENT_IN_CHECKOUT"
        );
        appendRequirement(fields, evidence);
        appendTarget(fields, evidence);
        appendAssociation(fields, evidence);
        appendFailure(fields, evidence);
        appendSessionMismatch(
                fields,
                evidence.targetRegistrySnapshot().mismatchSnapshot(),
                snapshot.key().commandCorrelationId()
        );
        return fields;
    }

    private static void appendRequirement(
            Map<String, Object> fields,
            FabricChatClefCraftResourceTerminalEvidence evidence) {
        if (evidence.scopeBinding().isEmpty()) {
            fields.put("requirementEvidenceStatus", "UNAVAILABLE_SCOPE_BINDING_NOT_ACTIVE");
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = evidence.scopeBinding().get();
        CraftResourceRequirementDecision requirement = binding.requirement();
        fields.put("requirementEvidenceStatus", "CAPTURED_AUTHORITATIVE_DECISION");
        fields.put("deltaNeeded", value(requirement.deltaNeeded()));
        fields.put("quantityContract", requirement.quantityContract().name());
        fields.put("runtimeArtifactProof", requirement.artifactProof().name());
        fields.put("recipeOutputCount", value(requirement.recipeOutputCount()));
        fields.put("recipeCraftCount", value(requirement.recipeCraftCount()));
        if (evidence.requirementProgressSnapshot().isEmpty()) {
            fields.put("requirementProgressEvidenceStatus", "UNAVAILABLE_PROGRESS_LEDGER_NOT_ACTIVE");
            fields.put("activeRequirementItem", requirement.activeRequirementItem());
            fields.put("activeRequirementCount", value(requirement.activeRequirementCount()));
            return;
        }
        CraftResourceRequirementProgressSnapshot progress =
                evidence.requirementProgressSnapshot().get();
        fields.put("requirementProgressEvidenceStatus", "BOUNDED_SCOPE_LEDGER_CAPTURED");
        fields.put("activeRequirementItem", progress.activeRequirementItem());
        fields.put("activeRequirementCount", progress.activeRequirementCount());
        fields.put("activeRequirementStage", progress.resourceStage().name());
        fields.put("requirementProgressFirstObservedTick", progress.firstObservedTick());
        fields.put("requirementProgressLastObservedTick", progress.lastObservedTick());
        fields.put("suppressedRequirementDetailCount", progress.suppressedDetailCount());
        fields.put("unownedRequirementObservationCount", progress.unownedObservationCount());
        fields.put("unknownRequirementAssociationCount", progress.unknownAssociationCount());
        fields.put("omittedRequirementHistoryCount", progress.omittedRequirementHistoryCount());
        fields.put("requirementProgressCounterSaturated", progress.counterSaturated());
    }

    private static void appendTarget(
            Map<String, Object> fields,
            FabricChatClefCraftResourceTerminalEvidence evidence) {
        if (evidence.targetSnapshot().isEmpty()) {
            fields.put("targetEvidenceStatus", "UNAVAILABLE_TARGET_SCOPE_NOT_ACTIVE");
            return;
        }
        CraftResourceTargetAttemptSnapshot target = evidence.targetSnapshot().get();
        fields.put("targetEvidenceStatus", "BOUNDED_SCOPE_LEDGER_CAPTURED");
        fields.put("attemptTransitionCount", target.attemptTransitionCount());
        fields.put("detailEligibleTransitionCount", target.detailEligibleTransitionCount());
        fields.put("omittedTargetSampleCount", target.omittedTargetSampleCount());
        fields.put("unownedTargetObservationCount", target.unownedObservationCount());
        fields.put("unknownTargetAssociationCount", target.unknownAssociationCount());
        fields.put("targetCounterSaturated", target.counterSaturated());
        fields.put(
                "expectedBlockIdsOmittedCount",
                target.currentTuple().isPresent()
                        ? target.currentTuple().get().expectedBlockIdsOmittedCount()
                        : target.lastClosure().isPresent()
                                ? target.lastClosure().get().targetTuple()
                                        .expectedBlockIdsOmittedCount()
                                : "UNAVAILABLE_NO_TARGET_OBSERVED"
        );
    }

    private static void appendAssociation(
            Map<String, Object> fields,
            FabricChatClefCraftResourceTerminalEvidence evidence) {
        if (evidence.associationSnapshot().isEmpty()) {
            fields.put("associationEvidenceStatus", "UNAVAILABLE_SCOPE_LEDGER_NOT_ACTIVE");
            return;
        }
        CraftResourceAssociationLedgerSnapshot association =
                evidence.associationSnapshot().get();
        fields.put("associationEvidenceStatus", "BOUNDED_SCOPE_LEDGER_CAPTURED");
        fields.put("currentAssociationStatus", association.currentStatus().name());
        fields.put("associationObservationGapCount", association.observationGapCount());
        fields.put(
                "lastAssociationObservationGapBoundary",
                association.lastObservationGapBoundary()
        );
        fields.put(
                "lastAssociationObservationGapReason",
                association.lastObservationGapReason()
        );
        fields.put("associationCounterSaturated", association.counterSaturated());
    }

    private static void appendFailure(
            Map<String, Object> fields,
            FabricChatClefCraftResourceTerminalEvidence evidence) {
        if (evidence.failureSnapshot().isEmpty()) {
            fields.put("failureAggregateEvidenceStatus", "UNAVAILABLE_FAILURE_SCOPE_NOT_ACTIVE");
            return;
        }
        CraftResourceFailureAggregateSnapshot failure = evidence.failureSnapshot().get();
        fields.put("failureAggregateEvidenceStatus", "BOUNDED_SCOPE_LEDGER_CAPTURED");
        fields.put("unownedFailureObservationCount", failure.unownedObservationCount());
        fields.put("unknownFailureAssociationCount", failure.unknownAssociationCount());
        fields.put("failureCounterSaturated", failure.counterSaturated());
    }

    private static void appendSessionMismatch(
            Map<String, Object> fields,
            CraftResourceMismatchSnapshot mismatch,
            String correlationId) {
        fields.put(
                "sessionMismatchAggregateScope",
                "DIAGNOSTIC_SESSION_NOT_COMMAND_ATTRIBUTABLE"
        );
        fields.put("sessionOwnedMismatchOccurrenceCount", mismatch.ownedMismatchOccurrenceCount());
        fields.put("sessionMismatchCoverageGapCount", mismatch.coverageGapCount());
        fields.put("sessionMismatchDuplicateSuppressedCount", mismatch.duplicateSuppressedCount());
        fields.put(
                "sessionMismatchPerCorrelationLimitSuppressedCount",
                mismatch.perCorrelationLimitSuppressedCount()
        );
        fields.put("sessionMismatchLimitSuppressedCount", mismatch.sessionLimitSuppressedCount());
        fields.put("sessionMismatchAdmissionDeniedCount", mismatch.mismatchAdmissionDeniedCount());
        fields.put("sessionMismatchEmissionFailureCount", mismatch.mismatchEmissionFailureCount());
        fields.put(
                "retainedMismatchSignatureCountForCorrelation",
                mismatch.retainedSignatureCount(correlationId)
        );
        fields.put("mismatchCounterSaturated", mismatch.counterSaturated());
    }
}
