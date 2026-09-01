package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventEmitter;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.lifecycle.CraftResourceContainerLifecycleSourceEventListener;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation.FabricChatClefCraftResourceContainerActivationDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation.FabricChatClefCraftResourceContainerActiveTarget;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation.FabricChatClefCraftResourceContainerOwnerExitDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;

import java.util.Map;
import java.util.Optional;

//20260902_kpopmodder: Close only the sealed exact owner reference taken from the activation registry.
public final class FabricChatClefCraftResourceContainerLifecycleDiagnostics
        implements CraftResourceContainerLifecycleSourceEventListener {
    private static final FabricChatClefCraftResourceContainerLifecycleDiagnostics INSTANCE =
            new FabricChatClefCraftResourceContainerLifecycleDiagnostics();
    private static final CraftResourceAcquisitionEventEmitter EMITTER =
            new CraftResourceAcquisitionEventEmitter();

    private FabricChatClefCraftResourceContainerLifecycleDiagnostics() {
    }

    public static FabricChatClefCraftResourceContainerLifecycleDiagnostics instance() {
        return INSTANCE;
    }

    @Override
    public boolean isOwnerTracked(Task owner) {
        return FabricChatClefCraftResourceContainerActivationDiagnostics.hasOwner(owner);
    }

    @Override
    public void onOwnerStop(
            Task owner,
            Task interruptTask,
            CraftResourceTargetObservationKind terminationKind,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceContainerOwnerExitDecision ownerExit =
                FabricChatClefCraftResourceContainerActivationDiagnostics
                        .observeOwnerExit(owner, sourceEmissionCompleted);
        if (ownerExit.ownerIdentityAmbiguous()) {
            for (IronPickaxeAcquisitionScopeKey ambiguousScopeKey
                    : ownerExit.ambiguousScopeKeys()) {
                if (IronPickaxeAcquisitionScopeDiagnostics
                        .activeBinding(ambiguousScopeKey).isPresent()) {
                    observeGap(
                            ambiguousScopeKey,
                            "CONTAINER_OWNER_IDENTITY_AMBIGUOUS_ACROSS_SCOPES"
                    );
                }
            }
            return;
        }
        if (ownerExit.scopeKey().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeKey scopeKey = ownerExit.scopeKey().get();
        Optional<IronPickaxeAcquisitionScopeBinding> activeBinding =
                IronPickaxeAcquisitionScopeDiagnostics.activeBinding(scopeKey);
        if (activeBinding.isEmpty()) {
            return;
        }
        if (ownerExit.sourceTransitionSuppressed()) {
            observeGap(
                    scopeKey,
                    "CONTAINER_OWNER_STOP_SOURCE_DETAIL_NOT_EMITTED"
            );
        }
        if (ownerExit.closedActiveTarget().isEmpty()
                || !isOwnerTermination(terminationKind)) {
            return;
        }
        FabricChatClefCraftResourceContainerActiveTarget active =
                ownerExit.closedActiveTarget().get();
        CraftResourceTargetDiagnosticsRegistry targets =
                FabricChatClefCraftResourceTargetScopeDiagnostics.registry();
        Optional<CraftResourceTargetAttemptSnapshot> current =
                targets.currentSnapshot(scopeKey);
        if (current.isEmpty()
                || current.get().currentTuple().isEmpty()
                || !current.get().currentTuple().get().equals(active.targetTuple())
                || current.get().targetAttemptSequence()
                        != active.targetAttemptSequence()) {
            observeGap(scopeKey, "CONTAINER_OWNER_STOP_ACTIVE_TUPLE_DIVERGED");
            return;
        }
        targets.observeTarget(
                scopeKey,
                new CraftResourceTargetObservation(
                        CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                        terminationKind,
                        current.get().currentTuple()
                )
        );
        EMITTER.emit(
                "CRAFT_RESOURCE_TARGET_ATTEMPT_CLOSED",
                "craft_resource_target_attempt_closed",
                FabricChatClefCraftResourceContainerLifecycleEventFields.closed(
                        activeBinding.get(),
                        active,
                        owner,
                        interruptTask,
                        terminationKind,
                        ownerExit.sourceEmissionCompleted()
                ),
                Map.of()
        );
    }

    private static boolean isOwnerTermination(
            CraftResourceTargetObservationKind terminationKind) {
        return terminationKind == CraftResourceTargetObservationKind.OWNER_STOP
                || terminationKind == CraftResourceTargetObservationKind.OWNER_INTERRUPT;
    }

    private static void observeGap(
            IronPickaxeAcquisitionScopeKey scopeKey,
            String reason) {
        FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                scopeKey,
                "CONTAINER_TASK_OWNER_STOP",
                reason
        );
    }
}
