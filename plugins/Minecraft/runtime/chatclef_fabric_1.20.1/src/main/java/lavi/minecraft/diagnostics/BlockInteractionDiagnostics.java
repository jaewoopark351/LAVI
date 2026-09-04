package lavi.minecraft.diagnostics;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionDiagnosticFields;
import lavi.minecraft.diagnostics.interaction.BlockInteractionEmissionDecision;
import lavi.minecraft.diagnostics.interaction.BlockInteractionEmissionLimiter;
import lavi.minecraft.diagnostics.interaction.BlockInteractionInvocationTracker;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserverRegistry;
import lavi.minecraft.diagnostics.interaction.BlockInteractionScreenSnapshot;
import lavi.minecraft.diagnostics.interaction.BlockInteractionTargetClassifier;
import lavi.minecraft.diagnostics.interaction.BlockInteractionTargetInfo;
import lavi.minecraft.diagnostics.interaction.ownership.BlockInteractionOwnerTokenRegistry;
import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.function.LongSupplier;

//20260805_kpopmodder: Observe container-like interactBlock boundaries without changing task or input behavior.
final class BlockInteractionDiagnostics {
    private final DiagnosticModeController mode;
    private final DiagnosticEventEmitter events;
    private final LongSupplier currentClientTickId;
    private final LongSupplier nextOperationId;
    private final DiagnosticCommandContextRegistry commandContextRegistry;
    private final BlockInteractionTargetClassifier targetClassifier = new BlockInteractionTargetClassifier();
    private final BlockInteractionInvocationTracker invocationTracker = new BlockInteractionInvocationTracker();
    private final BlockInteractionObserverRegistry observerRegistry = new BlockInteractionObserverRegistry();
    private final BlockInteractionEmissionLimiter emissionLimiter = new BlockInteractionEmissionLimiter();
    private final BlockInteractionOwnerTokenRegistry ownerTokens =
            new BlockInteractionOwnerTokenRegistry();

    BlockInteractionDiagnostics(DiagnosticModeController mode,
                                DiagnosticEventEmitter events,
                                LongSupplier currentClientTickId,
                                LongSupplier nextOperationId,
                                DiagnosticCommandContextRegistry commandContextRegistry) {
        this.mode = mode;
        this.events = events;
        this.currentClientTickId = currentClientTickId;
        this.nextOperationId = nextOperationId;
        this.commandContextRegistry = commandContextRegistry;
    }

    void registerObserver(BlockInteractionObserver observer) {
        observerRegistry.register(observer);
    }

    void clearForSessionTransition() {
        invocationTracker.clearForModeTransition();
        emissionLimiter.clearForModeTransition();
        ownerTokens.clearForModeTransition();
    }

    void noteOwner(Task sourceTask, BlockPos targetPosition) {
        if (!mode.isBoundaryEnabled()) {
            return;
        }
        ownerTokens.publish(
                sourceTask,
                targetPosition,
                currentClientTickId.getAsLong()
        );
    }

    void logInteract(String phase,
                     ClientPlayerEntity player,
                     Object hand,
                     BlockHitResult hitResult,
                     Object result) {
        if (mode.isOff() || hitResult == null) {
            return;
        }
        String normalizedPhase = phase == null ? "" : phase.toUpperCase(Locale.ROOT);
        if ("HEAD".equals(normalizedPhase)) {
            logHead(player, hand, hitResult);
            return;
        }
        if ("RETURN".equals(normalizedPhase)) {
            logReturn(player, hand, hitResult, result);
        }
    }

    private void logHead(ClientPlayerEntity player, Object hand, BlockHitResult hitResult) {
        MinecraftClient client = MinecraftClient.getInstance();
        BlockInteractionTargetInfo target = targetClassifier.classify(client, hitResult);
        if (!target.screenOpeningTarget()) {
            return;
        }

        long startClientTickId = currentClientTickId.getAsLong();
        Task sourceTask = ownerTokens.consume(
                hitResult.getBlockPos(),
                startClientTickId
        );
        BlockInteractionContext context = new BlockInteractionContext(
                nextOperationId.getAsLong(),
                startClientTickId,
                target,
                hand,
                hitResult,
                BlockInteractionScreenSnapshot.current(client, player),
                true,
                sourceTask
        );
        invocationTracker.begin(context);
        boolean sourceEmissionCompleted = emitInteractionEvent(
                "CONTAINER_OPEN_ATTEMPT_OBSERVED",
                "container_open_interact_head",
                "HEAD",
                context,
                "unavailable",
                null
        );
        observerRegistry.notifyBefore(context, player, hand, hitResult, sourceEmissionCompleted);
    }

    private void logReturn(ClientPlayerEntity player, Object hand, BlockHitResult hitResult, Object result) {
        BlockPos targetPosition = hitResult.getBlockPos();
        BlockInteractionContext context = invocationTracker.end(targetPosition);
        if (context == null) {
            MinecraftClient client = MinecraftClient.getInstance();
            BlockInteractionTargetInfo target = targetClassifier.classify(client, hitResult);
            if (!target.screenOpeningTarget()) {
                return;
            }
            context = new BlockInteractionContext(
                    nextOperationId.getAsLong(),
                    currentClientTickId.getAsLong(),
                    target,
                    hand,
                    hitResult,
                    BlockInteractionScreenSnapshot.current(client, player),
                    false,
                    null
            );
        }

        BlockInteractionScreenSnapshot screenAfter = BlockInteractionScreenSnapshot.current(MinecraftClient.getInstance(), player);
        boolean sourceEmissionCompleted = emitInteractionEvent(
                "CONTAINER_OPEN_RETURN_OBSERVED",
                "container_open_interact_return",
                "RETURN",
                context,
                result,
                screenAfter
        );
        observerRegistry.notifyAfter(
                context,
                player,
                hand,
                hitResult,
                result,
                sourceEmissionCompleted
        );
    }

    private boolean emitInteractionEvent(String eventName,
                                         String reason,
                                         String phase,
                                         BlockInteractionContext context,
                                         Object result,
                                         BlockInteractionScreenSnapshot screenAfter) {
        if (!mode.isBoundaryEnabled()) {
            return false;
        }

        String repeatKey = eventName + "|"
                + context.targetKind() + "|"
                + context.targetBlockId() + "|"
                + blockPos(context.targetPosition()) + "|"
                + String.valueOf(result) + "|"
                + StoreDepositDiagnostics.interactionScopeKey(context);
        BlockInteractionEmissionDecision decision = emissionLimiter.evaluate(repeatKey, currentClientTickId.getAsLong());
        if (decision.emitCap()) {
            emitBoundary("BLOCK_INTERACTION_DIAGNOSTIC_FAMILY_CAP_REACHED", "block_interaction_diagnostic_cap_reached", null,
                    "capScope", "block_interaction",
                    "cap", 5000);
            return false;
        }
        if (decision.emitSummary()) {
            emitBoundary("DIAGNOSTIC_REPEAT_SUMMARY", "block_interaction_repeat_summary", null,
                    BlockInteractionDiagnosticFields.repeatSummaryFields(repeatKey, decision.suppressedRepeatCount()));
            return false;
        }
        if (!decision.emitEvent()) {
            return false;
        }
        if (!StoreDepositDiagnostics.shouldEmitInteractionDetail(context, eventName, repeatKey)) {
            return false;
        }

        return emitBoundary(eventName, reason, null,
                DiagnosticEventEmitter.mergeFields(
                        DiagnosticEventEmitter.mergeFields(
                                DiagnosticEventEmitter.mergeFields(
                                        BlockInteractionDiagnosticFields.interactionFields(
                                                context,
                                                phase,
                                                result,
                                                screenAfter,
                                                decision.suppressedRepeatCount()
                                        ),
                                        DiagnosticInputState.currentStateFields()
                                ),
                                StoreDepositDiagnostics.interactionFields(context)
                        ),
                        commandContextRegistry.fields()
                ));
    }

    private boolean emitBoundary(String eventName, String reason, Task task, Object... fields) {
        return events.emitEventWithOutcome(
                "BOUNDARY",
                "[LAVI ChatClefBoundary]",
                eventName,
                reason,
                task,
                fields,
                false
        ).emissionCompleted();
    }

    private static String blockPos(BlockPos pos) {
        if (pos == null) {
            return "unavailable";
        }
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
