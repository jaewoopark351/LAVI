package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.progress;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventContract;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventEmitter;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.CraftResourceRequirementDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress.CraftResourceRequirementProgressObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress.CraftResourceRequirementProgressRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress.CraftResourceRequirementProgressSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Project semantic requirement changes from existing source arguments only.
public final class FabricChatClefCraftResourceRequirementProgressDiagnostics {
    private static final CraftResourceRequirementProgressRegistry REGISTRY =
            new CraftResourceRequirementProgressRegistry();
    private static final CraftResourceAcquisitionEventEmitter EMITTER =
            new CraftResourceAcquisitionEventEmitter();

    private FabricChatClefCraftResourceRequirementProgressDiagnostics() {
    }

    public static void observeActivation(IronPickaxeAcquisitionScopeBinding binding) {
        if (binding != null) {
            REGISTRY.activate(binding.key());
        }
    }

    public static void observeInitialDecision(
            IronPickaxeAcquisitionScopeBinding binding,
            boolean sourceEmissionCompleted) {
        if (binding == null) {
            return;
        }
        CraftResourceRequirementDecision requirement = binding.requirement();
        observe(
                binding,
                null,
                CraftResourceStage.RECIPE_PLANNING,
                requirement.requestedItem(),
                requirement.requestedCount().isPresent()
                        ? requirement.requestedCount().getAsInt()
                        : -1L,
                CraftResourceSourceEventName.VISIBLE_TASK_RETURN,
                binding.activatedAtClientTick(),
                sourceEmissionCompleted,
                false
        );
    }

    public static void observeMaterialProgress(
            Task task,
            ItemTarget materialTarget,
            String currentGate,
            int materialsNeeded,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(task);
        if (association.binding().isEmpty()) {
            return;
        }
        observe(
                association.binding().get(),
                association,
                stageForGate(currentGate),
                materialTarget == null ? "UNAVAILABLE" : String.valueOf(materialTarget),
                materialsNeeded,
                CraftResourceSourceEventName.SMELT_MATERIAL_PROGRESS_SNAPSHOT,
                association.captureClientTick(),
                sourceEmissionCompleted,
                true
        );
    }

    public static void observeOperationGate(
            Task task,
            String currentGate,
            int materialsNeeded,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(task);
        if (association.binding().isEmpty()) {
            return;
        }
        observe(
                association.binding().get(),
                association,
                stageForGate(currentGate),
                "UNAVAILABLE",
                materialsNeeded,
                CraftResourceSourceEventName.FURNACE_OPERATION_GATE_TRANSITION,
                association.captureClientTick(),
                sourceEmissionCompleted,
                true
        );
    }

    public static Optional<CraftResourceRequirementProgressSnapshot> snapshot(
            IronPickaxeAcquisitionScopeKey key) {
        return REGISTRY.snapshot(key);
    }

    public static void retire(IronPickaxeAcquisitionScopeKey key) {
        REGISTRY.retire(key);
    }

    public static void clearForModeOff() {
        REGISTRY.clearForModeOff();
    }

    private static void observe(
            IronPickaxeAcquisitionScopeBinding binding,
            FabricChatClefCraftResourceAssociationSnapshot association,
            CraftResourceStage stage,
            String item,
            long count,
            CraftResourceSourceEventName sourceEventName,
            long observedTick,
            boolean sourceEmissionCompleted,
            boolean emitTransitionDetail) {
        CraftResourceRequirementProgressObservation observation =
                new CraftResourceRequirementProgressObservation(
                        association == null
                                ? CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT
                                : association.decision().status(),
                        stage,
                        item,
                        count,
                        sourceEventName.name(),
                        observedTick,
                        sourceEmissionCompleted
                );
        boolean changed = REGISTRY.observe(binding.key(), observation);
        if (!changed || !sourceEmissionCompleted || !emitTransitionDetail) {
            return;
        }
        CraftResourceRequirementProgressSnapshot progress = REGISTRY.snapshot(
                binding.key()
        ).orElseThrow();
        Map<String, Object> required = new LinkedHashMap<>();
        required.put("sourceEventName", sourceEventName.name());
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
        required.put("boundRootTaskInstanceId", binding.key().boundRootTaskInstanceId());
        required.put("resourceStage", progress.resourceStage().name());
        required.put("activeRequirementItem", progress.activeRequirementItem());
        required.put("activeRequirementCount", progress.activeRequirementCount());
        required.put("requirementTransitionCount", progress.requirementTransitionCount());
        required.put("sourceEventEmissionStatus", "EMISSION_CALLS_RETURNED");
        required.put("diagnosticCaptureStatus", "complete");
        EMITTER.emit(
                "CRAFT_RESOURCE_REQUIREMENT_DECISION",
                "craft_resource_requirement_transition",
                required,
                Map.of()
        );
    }

    private static CraftResourceStage stageForGate(String gate) {
        return "GET_MATERIAL".equals(gate)
                ? CraftResourceStage.IRON_INPUT_ACQUISITION
                : CraftResourceStage.RAW_IRON_SMELTING;
    }
}
