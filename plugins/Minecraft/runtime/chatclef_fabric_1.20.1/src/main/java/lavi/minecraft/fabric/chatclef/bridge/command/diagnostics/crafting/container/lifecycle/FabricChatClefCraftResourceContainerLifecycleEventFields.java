package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation.FabricChatClefCraftResourceContainerActiveTarget;

import java.util.LinkedHashMap;
import java.util.Map;

//20260902_kpopmodder: Format a sealed exact-owner closure without reading ambient command state.
final class FabricChatClefCraftResourceContainerLifecycleEventFields {
    private FabricChatClefCraftResourceContainerLifecycleEventFields() {
    }

    static Map<String, Object> closed(
            IronPickaxeAcquisitionScopeBinding binding,
            FabricChatClefCraftResourceContainerActiveTarget active,
            Task owner,
            Task interruptTask,
            CraftResourceTargetObservationKind terminationKind,
            boolean sourceEmissionCompleted) {
        IronPickaxeAcquisitionScopeKey key = binding.key();
        CraftResourceTargetTuple tuple = active.targetTuple();
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("commandRequestId", key.commandRequestId());
        fields.put("commandCorrelationId", key.commandCorrelationId());
        fields.put("commandSessionId", key.commandSessionId());
        fields.put("commandConnectionGeneration", key.commandConnectionGeneration());
        fields.put("rootAssignmentId", key.rootAssignmentId());
        fields.put("rootGeneration", key.rootGeneration());
        fields.put("boundRootTaskInstanceId", key.boundRootTaskInstanceId());
        fields.put("associationStatus", "COMMAND_ROOT_DESCENDANT");
        fields.put(
                "associationEvidence",
                "SEALED_EXACT_OWNER_FROM_APPLIED_RECONCILIATION"
        );
        fields.put("sourceEventName", CraftResourceSourceEventName.CONTAINER_TASK_OWNER_STOP.name());
        fields.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        fields.put("targetAttemptSequence", active.targetAttemptSequence());
        fields.put("targetAttemptStarted", false);
        fields.put("targetLedgerMutation", terminationKind.name());
        fields.put("resourceStage", tuple.resourceStage().name());
        fields.put("targetRole", tuple.targetRole().name());
        fields.put("targetPosition", tuple.targetPosition());
        fields.put("expectedBlockIds", tuple.expectedBlockIds());
        fields.put("expectedBlockIdsOmittedCount", tuple.expectedBlockIdsOmittedCount());
        fields.put("ownerTaskClass", taskClass(owner));
        fields.put("ownerTaskInstanceId", taskInstanceId(owner));
        fields.put("activeChildClass", taskClass(active.activeChildTask()));
        fields.put("activeChildInstanceId", taskInstanceId(active.activeChildTask()));
        fields.put("interruptTaskClass", taskClass(interruptTask));
        fields.put("interruptTaskInstanceId", taskInstanceId(interruptTask));
        fields.put("ownerTerminationKind", terminationKind.name());
        fields.put("targetClosureKind", terminationKind.name());
        fields.put("targetClosureReason", "CONTAINER_OWNER_ON_STOP_CALLBACK");
        fields.put("sourceEmissionCompleted", sourceEmissionCompleted);
        fields.put("currentContextNotAssociationAuthority", true);
        fields.put("diagnosticCaptureStatus", "partial_source_sequence_unavailable");
        return fields;
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
