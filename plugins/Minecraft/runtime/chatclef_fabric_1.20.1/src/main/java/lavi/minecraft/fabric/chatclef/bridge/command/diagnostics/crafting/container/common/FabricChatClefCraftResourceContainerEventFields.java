package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common;

import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.reference.FabricChatClefCraftResourceReferenceDecision;

import java.util.LinkedHashMap;
import java.util.Map;

//20260901_kpopmodder: Assemble bounded container reference fields without duplicating source facts.
public final class FabricChatClefCraftResourceContainerEventFields {
    private FabricChatClefCraftResourceContainerEventFields() {
    }

    public static Map<String, Object> targetRoleTransition(
            FabricChatClefCraftResourceAssociationSnapshot association,
            CraftResourceSourceEventName sourceEventName,
            FabricChatClefCraftResourceReferenceDecision reference,
            String observationBoundary,
            String semanticAuthority,
            String parentTaskClass,
            String childTaskClass) {
        IronPickaxeAcquisitionScopeBinding binding = association.binding().orElseThrow();
        CraftResourceTargetTuple current = reference.current();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("sourceEventName", sourceEventName.name());
        fields.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        fields.put("commandRequestId", binding.key().commandRequestId());
        fields.put("commandCorrelationId", binding.key().commandCorrelationId());
        fields.put("commandSessionId", binding.key().commandSessionId());
        fields.put("commandConnectionGeneration", binding.key().commandConnectionGeneration());
        fields.put("rootAssignmentId", binding.key().rootAssignmentId());
        fields.put("rootGeneration", binding.key().rootGeneration());
        fields.put("rootTaskClass", binding.rootTaskClass());
        fields.put("boundRootTaskInstanceId", binding.key().boundRootTaskInstanceId());
        fields.put("targetAttemptSequence", "UNAVAILABLE");
        fields.put("targetAttemptStarted", false);
        fields.put("selectedChainClass", association.selectedChainClass());
        fields.put(
                "selectedChainIsUserTaskChain",
                "adris.altoclef.chains.UserTaskChain".equals(association.selectedChainClass())
        );
        fields.put("selectedChainPathSize", association.selectedChainPathSize());
        fields.put("selectedChainPathTruncated", association.selectedChainPathTruncated());
        fields.put("associationStatus", association.decision().status().name());
        fields.put("associationEvidence", association.decision().reason());
        fields.put("associationCaptureClientTick", association.captureClientTick());
        fields.put("associationCaptureThread", association.captureThread());
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
        fields.put("parentTaskClass", parentTaskClass);
        fields.put("childTaskClass", childTaskClass);
        fields.put(
                "previousResourceStage",
                reference.previous()
                        .map(tuple -> tuple.resourceStage().name())
                        .orElse("UNKNOWN")
        );
        fields.put("resourceStage", current.resourceStage().name());
        fields.put(
                "previousTargetRole",
                reference.previous()
                        .map(tuple -> tuple.targetRole().name())
                        .orElse("UNKNOWN")
        );
        fields.put("targetRole", current.targetRole().name());
        fields.put("targetPosition", current.targetPosition());
        fields.put("expectedBlockIds", current.expectedBlockIds());
        fields.put("expectedBlockIdsOmittedCount", current.expectedBlockIdsOmittedCount());
        fields.put("referenceTransitionCount", reference.transitionCount());
        fields.put(
                "referenceDetailEligibleTransitionCount",
                reference.detailEligibleTransitionCount()
        );
        fields.put(
                "referenceSuppressedDetailTransitionCount",
                reference.suppressedDetailTransitionCount()
        );
        fields.put("referenceCounterSaturated", reference.counterSaturated());
        fields.put("observationBoundary", observationBoundary);
        fields.put("semanticAuthority", semanticAuthority);
        fields.put("diagnosticCaptureStatus", "complete_reference_only");
        return fields;
    }
}
