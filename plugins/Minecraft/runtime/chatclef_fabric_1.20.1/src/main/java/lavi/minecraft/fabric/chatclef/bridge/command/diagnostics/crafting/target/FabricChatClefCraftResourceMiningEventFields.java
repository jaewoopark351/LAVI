package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target;

import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEmissionStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceMismatchObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

//20260901_kpopmodder: Assemble bounded mining projection fields separately from evidence capture.
final class FabricChatClefCraftResourceMiningEventFields {
    private FabricChatClefCraftResourceMiningEventFields() {
    }

    static Map<String, Object> targetTransition(
            FabricChatClefCraftResourceAssociationSnapshot association,
            CraftResourceSourceEventName sourceEventName,
            Optional<CraftResourceTargetTuple> previousTuple,
            CraftResourceTargetTuple currentTuple,
            CraftResourceTargetAttemptDecision decision,
            String observationBoundary,
            String parentTaskClass,
            String childTaskClass,
            boolean sourceEmissionCompleted) {
        Map<String, Object> fields = common(
                association,
                sourceEventName,
                sourceEmissionCompleted
        );
        fields.put("targetAttemptSequence", sequence(decision.targetAttemptSequence()));
        fields.put("previousResourceStage", previousTuple
                .map(value -> value.resourceStage().name())
                .orElse("UNKNOWN"));
        fields.put("resourceStage", currentTuple.resourceStage().name());
        fields.put("previousTargetRole", previousTuple
                .map(value -> value.targetRole().name())
                .orElse("UNKNOWN"));
        fields.put("targetRole", currentTuple.targetRole().name());
        fields.put("targetPosition", currentTuple.targetPosition());
        fields.put("expectedBlockIds", currentTuple.expectedBlockIds());
        fields.put("expectedBlockIdsOmittedCount", currentTuple.expectedBlockIdsOmittedCount());
        fields.put("observationBoundary", observationBoundary);
        fields.put("parentTaskClass", parentTaskClass);
        fields.put("childTaskClass", childTaskClass);
        fields.put("diagnosticCaptureStatus", "complete");
        return fields;
    }

    static Map<String, Object> mismatch(
            FabricChatClefCraftResourceAssociationSnapshot association,
            CraftResourceMismatchObservation observation,
            CraftResourceTargetDiagnosticsRegistry registry,
            CraftResourceTargetTuple activeTuple,
            String observedBlockStateSummary,
            String parentTaskClass,
            String childTaskClass,
            boolean sourceEmissionCompleted) {
        Map<String, Object> fields = common(
                association,
                CraftResourceSourceEventName.MINE_TARGET_GOAL_REQUEST,
                sourceEmissionCompleted
        );
        IronPickaxeAcquisitionScopeBinding binding = association.binding().orElseThrow();
        Object targetAttemptSequence = "UNAVAILABLE";
        Optional<CraftResourceTargetAttemptSnapshot> targetSnapshot =
                registry.currentSnapshot(binding.key());
        if (targetSnapshot.isPresent()
                && targetSnapshot.get().currentTuple().filter(activeTuple::equals).isPresent()
                && targetSnapshot.get().targetAttemptSequence() > 0L) {
            targetAttemptSequence = targetSnapshot.get().targetAttemptSequence();
        }
        fields.put("targetAttemptSequence", targetAttemptSequence);
        fields.put("resourceStage", observation.resourceStage().name());
        fields.put("targetRole", observation.targetRole().name());
        fields.put("targetPosition", observation.targetPosition());
        fields.put("expectedBlockIds", observation.expectedBlockIds().orElseGet(java.util.List::of));
        fields.put("expectedBlockIdsOmittedCount", observation.expectedBlockIdsOmittedCount());
        fields.put("observedBlockId", observation.observedBlockId().orElse("UNAVAILABLE"));
        fields.put("observedBlockStateSummary", observedBlockStateSummary);
        fields.put("observationBoundary", observation.observationBoundary());
        fields.put("observationSource", "EXISTING_GET_GOAL_TASK_LOCAL");
        fields.put("parentTaskClass", parentTaskClass);
        fields.put("childTaskClass", childTaskClass);
        fields.put("diagnosticCaptureStatus", "complete");
        return fields;
    }

    static Map<String, Object> targetClosure(
            FabricChatClefCraftResourceAssociationSnapshot association,
            CraftResourceTargetTuple closedTuple,
            CraftResourceTargetAttemptDecision decision,
            String closureKind,
            String closureReason,
            String miningPositionAfterBoundary,
            boolean sourceEmissionCompleted) {
        Map<String, Object> fields = common(
                association,
                CraftResourceSourceEventName.MINE_TARGET_ABANDONED,
                sourceEmissionCompleted
        );
        fields.put("targetAttemptSequence", sequence(decision.targetAttemptSequence()));
        fields.put("resourceStage", closedTuple.resourceStage().name());
        fields.put("targetRole", closedTuple.targetRole().name());
        fields.put("targetPosition", closedTuple.targetPosition());
        fields.put("expectedBlockIds", closedTuple.expectedBlockIds());
        fields.put("expectedBlockIdsOmittedCount", closedTuple.expectedBlockIdsOmittedCount());
        fields.put("targetClosureKind", closureKind);
        fields.put("targetClosureReason", closureReason);
        fields.put("miningPositionAfterBoundary", miningPositionAfterBoundary);
        fields.put("observationBoundary", "AFTER_APPLIED_CLEAR_OR_AT_OWNER_STOP");
        fields.put("diagnosticCaptureStatus", "complete");
        return fields;
    }

    private static Map<String, Object> common(
            FabricChatClefCraftResourceAssociationSnapshot association,
            CraftResourceSourceEventName sourceEventName,
            boolean sourceEmissionCompleted) {
        IronPickaxeAcquisitionScopeBinding binding = association.binding().orElseThrow();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("sourceEventName", sourceEventName.name());
        fields.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        fields.put(
                "sourceEventEmissionStatus",
                CraftResourceSourceEmissionStatus.from(sourceEmissionCompleted).name()
        );
        fields.put("commandRequestId", binding.key().commandRequestId());
        fields.put("commandCorrelationId", binding.key().commandCorrelationId());
        fields.put("commandSessionId", binding.key().commandSessionId());
        fields.put("commandConnectionGeneration", binding.key().commandConnectionGeneration());
        fields.put("rootAssignmentId", binding.key().rootAssignmentId());
        fields.put("rootGeneration", binding.key().rootGeneration());
        fields.put("rootTaskClass", binding.rootTaskClass());
        fields.put("boundRootTaskInstanceId", binding.key().boundRootTaskInstanceId());
        fields.put("associationStatus", association.decision().status().name());
        fields.put("associationEvidence", association.decision().reason());
        fields.put("associationCaptureClientTick", association.captureClientTick());
        fields.put("associationCaptureThread", association.captureThread());
        fields.put("selectedChainClass", association.selectedChainClass());
        fields.put("selectedChainPathSize", association.selectedChainPathSize());
        fields.put("selectedChainPathTruncated", association.selectedChainPathTruncated());
        fields.put(
                "emittingTaskPresentInSelectedChainPath",
                association.emittingTaskPresentInSelectedChainPath()
        );
        fields.put(
                "selectedChainPathRootMatchesBoundRoot",
                association.selectedChainPathRootMatchesBoundRoot()
        );
        fields.put("sourceTaskClass", association.sourceTaskClass());
        fields.put("sourceTaskInstanceId", association.sourceTaskInstanceId());
        return fields;
    }

    private static Object sequence(OptionalLong sequence) {
        return sequence.isPresent() ? sequence.getAsLong() : "UNAVAILABLE";
    }
}
