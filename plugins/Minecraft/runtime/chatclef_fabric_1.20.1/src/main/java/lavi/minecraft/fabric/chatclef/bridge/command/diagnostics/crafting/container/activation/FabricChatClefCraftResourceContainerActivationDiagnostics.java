package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventEmitter;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.source.container.reconciliation.CraftResourceContainerReconciliationSourceEventListener;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;

import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Project only physically emitted, identity-proven container installations.
public final class FabricChatClefCraftResourceContainerActivationDiagnostics
        implements CraftResourceContainerReconciliationSourceEventListener {
    private static final FabricChatClefCraftResourceContainerActivationDiagnostics INSTANCE =
            new FabricChatClefCraftResourceContainerActivationDiagnostics();
    private static final FabricChatClefCraftResourceContainerActivationRegistry ACTIVATIONS =
            new FabricChatClefCraftResourceContainerActivationRegistry();
    private static final CraftResourceAcquisitionEventEmitter EMITTER =
            new CraftResourceAcquisitionEventEmitter();

    private FabricChatClefCraftResourceContainerActivationDiagnostics() {
    }

    public static FabricChatClefCraftResourceContainerActivationDiagnostics instance() {
        return INSTANCE;
    }

    public static void observeCandidate(
            FabricChatClefCraftResourceAssociationSnapshot association,
            Task parentTask,
            Task candidateTask,
            CraftResourceTargetTuple targetTuple,
            CraftResourceSourceEventName candidateSourceEventName) {
        if (association == null
                || association.binding().isEmpty()
                || association.decision().status()
                        != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT
                || parentTask == null
                || candidateTask == null
                || !eligibleInteractionTuple(targetTuple)) {
            return;
        }
        IronPickaxeAcquisitionScopeKey scopeKey = association.binding().get().key();
        if (!ACTIVATIONS.observeCandidate(
                new FabricChatClefCraftResourceContainerActivationCandidate(
                        scopeKey,
                        parentTask,
                        candidateTask,
                        targetTuple,
                        candidateSourceEventName
                ))) {
            observeGap(scopeKey, "CONTAINER_ACTIVATION_CANDIDATE_CAPACITY_UNAVAILABLE");
        }
    }

    @Override
    public void onChildReconciliation(
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean isEqualResult,
            boolean canInterruptEvaluated,
            boolean canInterruptPreviousChild,
            boolean replacementApplied,
            boolean previousChildStopCalled,
            Task activeChildAfter,
            boolean candidateDiscardedBecauseEqual,
            boolean childCleared,
            boolean sourceEmissionCompleted) {
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(parent);
        if (association.binding().isEmpty()
                || association.decision().status()
                        != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        FabricChatClefCraftResourceContainerActivationDecision activation =
                ACTIVATIONS.reconcile(
                        binding.key(),
                        parent,
                        activeChildBefore,
                        candidateChild,
                        replacementApplied,
                        activeChildAfter,
                        childCleared,
                        sourceEmissionCompleted
                );
        if (activation.sourceTransitionSuppressed()) {
            observeGap(
                    binding.key(),
                    "CONTAINER_RECONCILIATION_SOURCE_DETAIL_NOT_EMITTED"
            );
        }
        if (activation.identityMismatchObserved()) {
            observeGap(
                    binding.key(),
                    "CONTAINER_RECONCILIATION_EXACT_TASK_IDENTITY_MISMATCH"
            );
        }
        closeInstalledTarget(
                association,
                activation.closedActiveTarget(),
                activation.sourceEmissionCompleted()
        );
        if (activation.installedCandidate().isPresent()) {
            activateInstalledTarget(
                    association,
                    activation.installedCandidate().get(),
                    activation.sourceEmissionCompleted()
            );
        }
    }

    public static void retire(IronPickaxeAcquisitionScopeKey scopeKey) {
        ACTIVATIONS.retire(scopeKey);
    }

    public static FabricChatClefCraftResourceContainerOwnerExitDecision observeOwnerExit(
            Task owner,
            boolean sourceEmissionCompleted) {
        return ACTIVATIONS.observeOwnerExit(owner, sourceEmissionCompleted);
    }

    public static boolean hasOwner(Task owner) {
        return ACTIVATIONS.hasOwner(owner);
    }

    public static void clearForModeOff() {
        ACTIVATIONS.clearForModeOff();
    }

    private static void closeInstalledTarget(
            FabricChatClefCraftResourceAssociationSnapshot association,
            Optional<FabricChatClefCraftResourceContainerActiveTarget> closed,
            boolean sourceEmissionCompleted) {
        if (closed.isEmpty() || association.binding().isEmpty()) {
            return;
        }
        IronPickaxeAcquisitionScopeKey key = association.binding().get().key();
        CraftResourceTargetDiagnosticsRegistry targets = targetRegistry();
        FabricChatClefCraftResourceContainerActiveTarget active = closed.get();
        Optional<CraftResourceTargetAttemptSnapshot> current =
                targets.currentSnapshot(key);
        if (current.isEmpty()
                || current.get().currentTuple().isEmpty()
                || !current.get().currentTuple().get().equals(active.targetTuple())
                || current.get().targetAttemptSequence()
                        != active.targetAttemptSequence()) {
            observeGap(key, "CONTAINER_ACTIVE_CHILD_TARGET_TUPLE_DIVERGED");
            return;
        }
        targets.observeTarget(
                key,
                new CraftResourceTargetObservation(
                        CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                        CraftResourceTargetObservationKind.TARGET_ABANDONED,
                        current.get().currentTuple()
                )
        );
        EMITTER.emit(
                "CRAFT_RESOURCE_TARGET_ATTEMPT_CLOSED",
                "craft_resource_target_attempt_closed",
                FabricChatClefCraftResourceContainerActivationEventFields.closed(
                        association,
                        active,
                        sourceEmissionCompleted,
                        "CONTAINER_CHILD_REPLACED_OR_CLEARED"
                ),
                Map.of()
        );
    }

    private static void activateInstalledTarget(
            FabricChatClefCraftResourceAssociationSnapshot association,
            FabricChatClefCraftResourceContainerActivationCandidate candidate,
            boolean sourceEmissionCompleted) {
        IronPickaxeAcquisitionScopeKey key = association.binding().orElseThrow().key();
        CraftResourceTargetDiagnosticsRegistry targets = targetRegistry();
        if (!ACTIVATIONS.canActivateCandidate(key, candidate)) {
            observeGap(key, "CONTAINER_ACTIVE_TARGET_OWNER_SLOT_NOT_AVAILABLE");
            return;
        }
        Optional<CraftResourceTargetAttemptSnapshot> before = targets.currentSnapshot(key);
        Optional<CraftResourceTargetTuple> previousTuple = before.flatMap(
                CraftResourceTargetAttemptSnapshot::currentTuple
        );
        if (previousTuple.isPresent()
                && !previousTuple.get().equals(candidate.targetTuple())) {
            targets.observeTarget(
                    key,
                    new CraftResourceTargetObservation(
                            CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                            CraftResourceTargetObservationKind.TARGET_ABANDONED,
                            previousTuple
                    )
            );
        }
        CraftResourceTargetAttemptDecision targetDecision = targets.observeTarget(
                key,
                new CraftResourceTargetObservation(
                        CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                        CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN,
                        Optional.of(candidate.targetTuple())
                )
        );
        if (targetDecision.targetAttemptSequence().isEmpty()) {
            observeGap(key, "CONTAINER_ACTIVE_TARGET_SEQUENCE_UNAVAILABLE");
            return;
        }
        long attemptSequence = targetDecision.targetAttemptSequence().getAsLong();
        if (!ACTIVATIONS.markActive(key, candidate, attemptSequence)) {
            targets.observeTarget(
                    key,
                    new CraftResourceTargetObservation(
                            CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                            CraftResourceTargetObservationKind.TARGET_ABANDONED,
                            Optional.of(candidate.targetTuple())
                    )
            );
            observeGap(key, "CONTAINER_ACTIVE_TARGET_OWNER_BINDING_FAILED");
            return;
        }
        if (!targetDecision.startedNewAttempt() || !targetDecision.detailEligible()) {
            return;
        }
        EMITTER.emit(
                "CRAFT_RESOURCE_TARGET_ROLE_TRANSITION",
                "craft_resource_target_role_transition",
                FabricChatClefCraftResourceContainerActivationEventFields.activated(
                        association,
                        candidate,
                        previousTuple,
                        attemptSequence,
                        sourceEmissionCompleted
                ),
                Map.of(
                        "activationAuthority",
                        "EXACT_CHILD_IDENTITY_AND_APPLIED_RECONCILIATION"
                )
        );
    }

    private static boolean eligibleInteractionTuple(CraftResourceTargetTuple tuple) {
        if (tuple == null
                || "UNAVAILABLE".equals(tuple.targetPosition())
                || tuple.expectedBlockIds().isEmpty()) {
            return false;
        }
        return tuple.targetRole() == CraftResourceTargetRole.FURNACE_INTERACTION
                || tuple.targetRole()
                        == CraftResourceTargetRole.CRAFTING_TABLE_INTERACTION;
    }

    private static CraftResourceTargetDiagnosticsRegistry targetRegistry() {
        return FabricChatClefCraftResourceTargetScopeDiagnostics.registry();
    }

    private static void observeGap(
            IronPickaxeAcquisitionScopeKey key,
            String reason) {
        FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                key,
                "CONTAINER_TARGET_ACTIVATION",
                reason
        );
    }
}
