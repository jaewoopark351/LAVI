package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceAcquisitionEventEmitter;
import lavi.minecraft.diagnostics.crafting.acquisition.event.CraftResourceSourceEventName;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetAttemptSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationReader;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.reference.FabricChatClefCraftResourceInteractionAttemptReference;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.reference.FabricChatClefCraftResourceInteractionReferenceRegistry;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.matching.FabricChatClefCraftResourceInteractionTupleMatch;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.matching.FabricChatClefCraftResourceInteractionTupleMatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Join one source-emitted interaction pair to an existing attempt only.
public final class FabricChatClefCraftResourceInteractionObserver
        implements BlockInteractionObserver {
    private static final FabricChatClefCraftResourceInteractionTargetClassifier CLASSIFIER =
            new FabricChatClefCraftResourceInteractionTargetClassifier();
    private static final FabricChatClefCraftResourceInteractionTupleMatcher MATCHER =
            new FabricChatClefCraftResourceInteractionTupleMatcher();
    private static final CraftResourceAcquisitionEventEmitter EMITTER =
            new CraftResourceAcquisitionEventEmitter();
    private static final FabricChatClefCraftResourceInteractionReferenceRegistry REFERENCES =
            new FabricChatClefCraftResourceInteractionReferenceRegistry();

    @Override
    public boolean requiresCompletedSourceEmission() {
        return true;
    }

    @Override
    public void beforeBlockInteraction(
            BlockInteractionContext context,
            ClientPlayerEntity player,
            Object hand,
            BlockHitResult hitResult) {
        Optional<FabricChatClefCraftResourceInteractionTarget> actualTarget =
                CLASSIFIER.classify(context);
        if (actualTarget.isEmpty()) {
            return;
        }
        FabricChatClefCraftResourceAssociationSnapshot association =
                FabricChatClefCraftResourceAssociationReader.capture(
                        context.sourceTask()
                );
        if (association.binding().isEmpty()
                || association.decision().status()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            return;
        }

        IronPickaxeAcquisitionScopeBinding binding = association.binding().get();
        CraftResourceTargetDiagnosticsRegistry registry =
                FabricChatClefCraftResourceTargetScopeDiagnostics.registry();
        Optional<CraftResourceTargetAttemptSnapshot> targetSnapshot =
                registry.currentSnapshot(binding.key());
        if (targetSnapshot.isEmpty()
                || targetSnapshot.get().targetAttemptSequence() <= 0L
                || targetSnapshot.get().currentTuple().isEmpty()) {
            observeGap(binding.key(), "INTERACTION_CURRENT_TARGET_ATTEMPT_UNAVAILABLE");
            return;
        }
        CraftResourceTargetTuple tuple = targetSnapshot.get().currentTuple().get();
        FabricChatClefCraftResourceInteractionTarget target = actualTarget.get();
        FabricChatClefCraftResourceInteractionTupleMatch tupleMatch = MATCHER.evaluate(
                tuple,
                target.resourceStage(),
                target.targetRole(),
                target.targetPosition(),
                target.sourceTargetBlockId()
        );
        if (!tupleMatch.matched()) {
            observeGap(binding.key(), tupleMatch.observationGapReason());
            return;
        }

        FabricChatClefCraftResourceInteractionAttemptReference reference =
                new FabricChatClefCraftResourceInteractionAttemptReference(
                        context.interactionId(),
                        binding.key(),
                        targetSnapshot.get().targetAttemptSequence(),
                        tuple,
                        target.sourceTargetBlockId(),
                        target.sourceTargetKind(),
                        context.startClientTickId()
                );
        if (!REFERENCES.begin(reference)) {
            observeGap(binding.key(), "INTERACTION_REFERENCE_CAPACITY_OR_DUPLICATE");
            return;
        }
        emitReference(
                reference,
                CraftResourceSourceEventName.CONTAINER_OPEN_ATTEMPT_OBSERVED,
                "HEAD",
                Optional.of(association)
        );
    }

    @Override
    public void afterBlockInteraction(
            BlockInteractionContext context,
            ClientPlayerEntity player,
            Object hand,
            BlockHitResult hitResult,
            Object result) {
        Optional<FabricChatClefCraftResourceInteractionAttemptReference> reference =
                REFERENCES.complete(context.interactionId());
        if (reference.isEmpty()) {
            return;
        }
        emitReference(
                reference.get(),
                CraftResourceSourceEventName.CONTAINER_OPEN_RETURN_OBSERVED,
                "RETURN",
                Optional.empty()
        );
    }

    public static void retire(IronPickaxeAcquisitionScopeKey scopeKey) {
        REFERENCES.retireScope(scopeKey);
    }

    public static void clearForModeOff() {
        REFERENCES.clearForModeOff();
    }

    private static void emitReference(
            FabricChatClefCraftResourceInteractionAttemptReference reference,
            CraftResourceSourceEventName sourceEventName,
            String phase,
            Optional<FabricChatClefCraftResourceAssociationSnapshot> association) {
        EMITTER.emit(
                "CRAFT_RESOURCE_TARGET_INTERACTION_REFERENCE",
                "craft_resource_target_interaction_" + phase.toLowerCase(),
                FabricChatClefCraftResourceInteractionEventFields.attemptReference(
                        reference,
                        sourceEventName,
                        phase,
                        association
                ),
                Map.of(
                        "sourceResultAuthority",
                        sourceEventName
                                == CraftResourceSourceEventName.CONTAINER_OPEN_RETURN_OBSERVED
                                ? "CONTAINER_OPEN_RETURN_OBSERVED_NOT_DUPLICATED"
                                : "CONTAINER_OPEN_ATTEMPT_OBSERVED_NOT_DUPLICATED"
                )
        );
    }

    private static void observeGap(
            IronPickaxeAcquisitionScopeKey scopeKey,
            String reason) {
        FabricChatClefCraftResourceAssociationScopeDiagnostics.observeObservationGap(
                scopeKey,
                "CRAFT_RESOURCE_TARGET_INTERACTION_REFERENCE",
                reason
        );
    }
}
