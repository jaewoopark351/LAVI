package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventEmitter;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation.FabricChatClefCraftResourceContainerActivationDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.reference.FabricChatClefCraftResourceReferenceDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.reference.FabricChatClefCraftResourceReferenceRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;

import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Bind candidate references fail-closed without starting target attempts.
public final class FabricChatClefCraftResourceContainerProjectionSupport {
    private static final CraftResourceAcquisitionEventEmitter EMITTER =
            new CraftResourceAcquisitionEventEmitter();
    private static final FabricChatClefCraftResourceReferenceRegistry REFERENCES =
            new FabricChatClefCraftResourceReferenceRegistry();

    private FabricChatClefCraftResourceContainerProjectionSupport() {
    }

    public static void observeReference(
            Task emittingTask,
            Task candidateTask,
            CraftResourceTargetTuple referenceTuple,
            CraftResourceSourceEventName sourceEventName,
            String observationBoundary,
            String semanticAuthority,
            String parentTaskClass,
            String childTaskClass,
            Map<String, Object> optionalFields) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(emittingTask);
        if (association.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        FabricChatClefCraftResourceTargetScopeDiagnostics.registry().observeTarget(
                binding.key(),
                new CraftResourceTargetObservation(
                        association.decision().status(),
                        CraftResourceTargetObservationKind.CANDIDATE_RETURN,
                        Optional.of(referenceTuple)
                )
        );
        FabricChatClefCraftResourceReferenceDecision reference = REFERENCES.observe(
                binding.key(),
                association.decision().status(),
                referenceTuple
        );
        FabricChatClefCraftResourceContainerActivationDiagnostics.observeCandidate(
                association,
                emittingTask,
                candidateTask,
                referenceTuple,
                sourceEventName
        );
        if (!reference.changed() || !reference.retained() || !reference.detailEligible()) {
            return;
        }
        EMITTER.emit(
                "CRAFT_RESOURCE_TARGET_ROLE_TRANSITION",
                "craft_resource_target_role_transition",
                FabricChatClefCraftResourceContainerEventFields.targetRoleTransition(
                        association,
                        sourceEventName,
                        reference,
                        observationBoundary,
                        semanticAuthority,
                        parentTaskClass,
                        childTaskClass
                ),
                optionalFields == null ? Map.of() : optionalFields
        );
    }

    public static void retire(IronPickaxeAcquisitionScopeBinding binding) {
        if (binding != null) {
            retire(binding.key());
        }
    }

    public static void retire(IronPickaxeAcquisitionScopeKey key) {
        if (key != null) {
            REFERENCES.retire(key);
            FabricChatClefCraftResourceContainerActivationDiagnostics.retire(key);
        }
    }

    public static void clearForModeOff() {
        REFERENCES.clearForModeOff();
        FabricChatClefCraftResourceContainerActivationDiagnostics.clearForModeOff();
    }
}
