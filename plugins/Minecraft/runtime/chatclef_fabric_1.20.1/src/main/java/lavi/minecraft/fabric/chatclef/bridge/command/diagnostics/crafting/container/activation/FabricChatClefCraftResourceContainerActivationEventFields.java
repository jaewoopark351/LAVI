package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Format installed-child transitions without reading game state again.
final class FabricChatClefCraftResourceContainerActivationEventFields {
    private FabricChatClefCraftResourceContainerActivationEventFields() {
    }

    static Map<String, Object> activated(
            FabricChatClefCraftResourceAssociationSnapshot association,
            FabricChatClefCraftResourceContainerActivationCandidate candidate,
            Optional<CraftResourceTargetTuple> previousTuple,
            long targetAttemptSequence,
            boolean sourceEmissionCompleted) {
        Map<String, Object> fields = base(association);
        fields.put(
                "sourceEventName",
                CraftResourceSourceEventName.CONTAINER_TASK_CHILD_RECONCILIATION.name()
        );
        fields.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        fields.put("candidateSourceEventName", candidate.candidateSourceEventName().name());
        fields.put("targetAttemptSequence", targetAttemptSequence);
        fields.put("targetAttemptStarted", true);
        fields.put("targetLedgerMutation", "ACTIVE_TARGET_PROVEN");
        fields.put("previousResourceStage", previousTuple
                .map(tuple -> tuple.resourceStage().name()).orElse("UNAVAILABLE"));
        fields.put("previousTargetRole", previousTuple
                .map(tuple -> tuple.targetRole().name()).orElse("UNAVAILABLE"));
        fields.put("previousTargetPosition", previousTuple
                .map(CraftResourceTargetTuple::targetPosition).orElse("UNAVAILABLE"));
        appendTuple(fields, candidate.targetTuple());
        fields.put("parentTaskClass", taskClass(candidate.parentTask()));
        fields.put("parentTaskInstanceId", taskInstanceId(candidate.parentTask()));
        fields.put("activeChildClass", taskClass(candidate.candidateTask()));
        fields.put("activeChildInstanceId", taskInstanceId(candidate.candidateTask()));
        fields.put("reconciliationSourceEmissionCompleted", sourceEmissionCompleted);
        fields.put("candidateSourceEmissionCompleted", true);
        fields.put("diagnosticCaptureStatus", "partial_source_sequence_unavailable");
        return fields;
    }

    static Map<String, Object> closed(
            FabricChatClefCraftResourceAssociationSnapshot association,
            FabricChatClefCraftResourceContainerActiveTarget active,
            boolean sourceEmissionCompleted,
            String closureReason) {
        Map<String, Object> fields = base(association);
        fields.put(
                "sourceEventName",
                CraftResourceSourceEventName.CONTAINER_TASK_CHILD_RECONCILIATION.name()
        );
        fields.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        fields.put("targetAttemptSequence", active.targetAttemptSequence());
        fields.put("targetAttemptStarted", false);
        fields.put("targetLedgerMutation", "TARGET_ABANDONED");
        appendTuple(fields, active.targetTuple());
        fields.put("parentTaskClass", taskClass(active.parentTask()));
        fields.put("parentTaskInstanceId", taskInstanceId(active.parentTask()));
        fields.put("closedChildClass", taskClass(active.activeChildTask()));
        fields.put("closedChildInstanceId", taskInstanceId(active.activeChildTask()));
        fields.put("targetClosureKind", "TARGET_ABANDONED");
        fields.put("targetClosureReason", closureReason);
        fields.put("reconciliationSourceEmissionCompleted", sourceEmissionCompleted);
        fields.put("diagnosticCaptureStatus", "partial_source_sequence_unavailable");
        return fields;
    }

    private static Map<String, Object> base(
            FabricChatClefCraftResourceAssociationSnapshot association) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (association.binding().isEmpty()) {
            return fields;
        }
        IronPickaxeAcquisitionScopeKey key = association.binding().get().key();
        fields.put("commandRequestId", key.commandRequestId());
        fields.put("commandCorrelationId", key.commandCorrelationId());
        fields.put("commandSessionId", key.commandSessionId());
        fields.put("commandConnectionGeneration", key.commandConnectionGeneration());
        fields.put("rootAssignmentId", key.rootAssignmentId());
        fields.put("rootGeneration", key.rootGeneration());
        fields.put("boundRootTaskInstanceId", key.boundRootTaskInstanceId());
        fields.put("associationStatus", association.decision().status().name());
        fields.put("associationEvidence", association.decision().reason());
        return fields;
    }

    private static void appendTuple(
            Map<String, Object> fields,
            CraftResourceTargetTuple tuple) {
        fields.put("resourceStage", tuple.resourceStage().name());
        fields.put("targetRole", tuple.targetRole().name());
        fields.put("targetPosition", tuple.targetPosition());
        fields.put("expectedBlockIds", tuple.expectedBlockIds());
        fields.put("expectedBlockIdsOmittedCount", tuple.expectedBlockIdsOmittedCount());
    }

    private static String taskClass(Task task) {
        return task == null ? "UNAVAILABLE" : task.getClass().getName();
    }

    private static String taskInstanceId(Task task) {
        return task == null
                ? "UNAVAILABLE"
                : task.getClass().getName()
                        + "@"
                        + Integer.toHexString(System.identityHashCode(task));
    }
}
