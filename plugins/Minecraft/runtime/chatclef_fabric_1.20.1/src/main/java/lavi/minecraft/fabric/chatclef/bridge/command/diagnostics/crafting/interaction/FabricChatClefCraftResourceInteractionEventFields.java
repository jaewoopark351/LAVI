package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction;

import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.reference.FabricChatClefCraftResourceInteractionAttemptReference;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Reference authoritative interaction logs without inventing an attempt.
final class FabricChatClefCraftResourceInteractionEventFields {
    private FabricChatClefCraftResourceInteractionEventFields() {
    }

    static Map<String, Object> attemptReference(
            FabricChatClefCraftResourceInteractionAttemptReference reference,
            CraftResourceSourceEventName sourceEventName,
            String phase,
            Optional<FabricChatClefCraftResourceAssociationSnapshot> boundaryAssociation) {
        IronPickaxeAcquisitionScopeKey key = reference.scopeKey();
        CraftResourceTargetTuple tuple = reference.targetTuple();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("sourceEventName", sourceEventName.name());
        fields.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        fields.put("commandRequestId", key.commandRequestId());
        fields.put("commandCorrelationId", key.commandCorrelationId());
        fields.put("commandSessionId", key.commandSessionId());
        fields.put("commandConnectionGeneration", key.commandConnectionGeneration());
        fields.put("rootAssignmentId", key.rootAssignmentId());
        fields.put("rootGeneration", key.rootGeneration());
        fields.put("boundRootTaskInstanceId", key.boundRootTaskInstanceId());
        fields.put("targetAttemptSequence", reference.targetAttemptSequence());
        fields.put("targetAttemptStarted", false);
        fields.put("targetLedgerMutation", "NONE_IMMUTABLE_REFERENCE_ONLY");
        fields.put("resourceStage", tuple.resourceStage().name());
        fields.put("targetRole", tuple.targetRole().name());
        fields.put("targetPosition", tuple.targetPosition());
        fields.put("expectedBlockIds", tuple.expectedBlockIds());
        fields.put("expectedBlockIdsOmittedCount", tuple.expectedBlockIdsOmittedCount());
        fields.put("interactionId", reference.interactionId());
        fields.put("interactionReferencePhase", phase);
        fields.put("interactionHeadClientTick", reference.headClientTick());
        fields.put("sourceTargetBlockId", reference.sourceTargetBlockId());
        fields.put(
                "sourceTargetBlockIdSemantics",
                "EXISTING_CONTEXT_TRANSLATION_KEY_FIELD"
        );
        fields.put("sourceTargetKind", reference.sourceTargetKind());
        fields.put("associationStatusAtHead", "COMMAND_ROOT_DESCENDANT");
        appendBoundaryAssociation(fields, key, boundaryAssociation);
        fields.put(
                "semanticAuthority",
                sourceEventName == CraftResourceSourceEventName.CONTAINER_OPEN_ATTEMPT_OBSERVED
                        ? "CONTAINER_OPEN_ATTEMPT_OBSERVED"
                        : "CONTAINER_OPEN_RETURN_OBSERVED"
        );
        fields.put("diagnosticCaptureStatus", "partial_source_sequence_unavailable");
        return fields;
    }

    private static void appendBoundaryAssociation(
            Map<String, Object> fields,
            IronPickaxeAcquisitionScopeKey key,
            Optional<FabricChatClefCraftResourceAssociationSnapshot> association) {
        if (association.isEmpty()) {
            fields.put("associationStatusAtBoundary", "UNKNOWN");
            fields.put("associationScopeMatchesAtBoundary", false);
            return;
        }
        FabricChatClefCraftResourceAssociationSnapshot snapshot = association.get();
        fields.put("associationStatusAtBoundary", snapshot.decision().status().name());
        fields.put(
                "associationScopeMatchesAtBoundary",
                snapshot.binding().isPresent()
                        && key.equals(snapshot.binding().get().key())
        );
        fields.put("associationEvidenceAtBoundary", snapshot.decision().reason());
        fields.put("associationCaptureClientTick", snapshot.captureClientTick());
    }
}
