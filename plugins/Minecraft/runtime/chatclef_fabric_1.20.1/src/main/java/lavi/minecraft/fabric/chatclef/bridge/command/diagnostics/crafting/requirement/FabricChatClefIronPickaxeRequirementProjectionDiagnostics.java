package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement;

import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventEmitter;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionActivation;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.progress.FabricChatClefCraftResourceRequirementProgressDiagnostics;
import adris.altoclef.tasksystem.Task;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;

//20260901_kpopmodder: Project captured quantity values without becoming a second decision authority.
public final class FabricChatClefIronPickaxeRequirementProjectionDiagnostics {
    private static final CraftResourceAcquisitionEventEmitter EMITTER =
            new CraftResourceAcquisitionEventEmitter();
    private static final Map<IronPickaxeAcquisitionScopeKey, IronPickaxeAcquisitionActivation>
            PENDING = new LinkedHashMap<>();

    private FabricChatClefIronPickaxeRequirementProjectionDiagnostics() {
    }

    public static synchronized void observeActivation(IronPickaxeAcquisitionActivation activation) {
        if (activation == null || activation.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = activation.binding().get();
        PENDING.put(binding.key(), activation);
        FabricChatClefCraftResourceRequirementProgressDiagnostics.observeActivation(binding);
    }

    public static void observeVisibleTaskReturn(Object sourceTask) {
        if (!(sourceTask instanceof Task task)) {
            return;
        }
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(task);
        if (association.binding().isEmpty()
                || !"COMMAND_ROOT_DESCENDANT".equals(association.decision().status().name())) {
            return;
        }
        IronPickaxeAcquisitionActivation activation;
        synchronized (FabricChatClefIronPickaxeRequirementProjectionDiagnostics.class) {
            activation = PENDING.remove(association.binding().get().key());
        }
        if (activation == null || activation.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = activation.binding().get();
        boolean sourceEmissionCompleted = emit(
                activation,
                binding,
                binding.requirement()
        );
        FabricChatClefCraftResourceRequirementProgressDiagnostics.observeInitialDecision(
                binding,
                sourceEmissionCompleted
        );
    }

    public static synchronized void retire(IronPickaxeAcquisitionScopeKey key) {
        PENDING.remove(key);
        FabricChatClefCraftResourceRequirementProgressDiagnostics.retire(key);
    }

    public static synchronized void clearForModeOff() {
        PENDING.clear();
        FabricChatClefCraftResourceRequirementProgressDiagnostics.clearForModeOff();
    }

    private static boolean emit(
            IronPickaxeAcquisitionActivation activation,
            IronPickaxeAcquisitionScopeBinding binding,
            CraftResourceRequirementDecision requirement
    ) {
        Map<String, Object> required = new LinkedHashMap<>();
        required.put("sourceEventName", CraftResourceSourceEventName.VISIBLE_TASK_RETURN.name());
        required.put(
                "sourceEventSequence",
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE
        );
        required.put("commandRequestId", binding.key().commandRequestId());
        required.put("commandCorrelationId", binding.key().commandCorrelationId());
        required.put("commandSessionId", binding.key().commandSessionId());
        required.put("commandConnectionGeneration", binding.key().commandConnectionGeneration());
        required.put("rootAssignmentId", binding.key().rootAssignmentId());
        required.put("rootGeneration", binding.key().rootGeneration());
        required.put("rootTaskClass", binding.rootTaskClass());
        required.put(
                "boundRootTaskInstanceId",
                binding.key().boundRootTaskInstanceId()
        );
        required.put("requestedItem", requirement.requestedItem());
        required.put("requestedCount", value(requirement.requestedCount()));
        required.put("currentItemCount", value(requirement.currentItemCount()));
        required.put("targetItemCount", value(requirement.targetItemCount()));
        required.put("deltaNeeded", value(requirement.deltaNeeded()));
        required.put("quantityContract", requirement.quantityContract().name());
        required.put("runtimeArtifactProof", requirement.artifactProof().name());
        required.put("recipeOutputCount", value(requirement.recipeOutputCount()));
        required.put("recipeCraftCount", value(requirement.recipeCraftCount()));
        required.put("activeRequirementItem", requirement.activeRequirementItem());
        required.put("activeRequirementCount", value(requirement.activeRequirementCount()));
        required.put("requirementCaptureAvailable", activation.requirementCaptureAvailable());
        required.put("requirementSource", "AGENT_COMMAND_UTILS_EXISTING_QUANTITY_DECISION");
        required.put("quantityDecisionBoundary", "AgentCommandUtils.addPresentItemsToTargets");
        required.put("associationStatus", "COMMAND_ROOT_DESCENDANT");
        required.put("diagnosticCaptureStatus", "complete");
        return EMITTER.emit(
                "CRAFT_RESOURCE_REQUIREMENT_DECISION",
                "craft_resource_requirement_decision",
                required,
                Map.of()
        );
    }

    private static Object value(OptionalInt optional) {
        return optional.isPresent() ? optional.getAsInt() : "UNAVAILABLE";
    }
}
