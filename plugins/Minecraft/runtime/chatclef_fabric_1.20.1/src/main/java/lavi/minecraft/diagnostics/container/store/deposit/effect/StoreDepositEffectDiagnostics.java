package lavi.minecraft.diagnostics.container.store.deposit.effect;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields.PredicateSnapshot;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositSlotActionDiagnostics;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

//20260829_kpopmodder: Keep predicate-to-effect correlation in one thread-local diagnostics collaborator.
public final class StoreDepositEffectDiagnostics {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositEmissionGate emissionGate;
    private final ThreadLocal<EpochPredicateSnapshot> lastPredicateSnapshot = new ThreadLocal<>();
    private final AtomicReference<Object> modeEpoch = new AtomicReference<>(new Object());
    private final StoreDepositSlotActionDiagnostics slotActions;

    public StoreDepositEffectDiagnostics(StoreDepositBindingRegistry bindings,
                                         StoreDepositEmissionGate emissionGate) {
        this(bindings, emissionGate, null);
    }

    public StoreDepositEffectDiagnostics(StoreDepositBindingRegistry bindings,
                                         StoreDepositEmissionGate emissionGate,
                                         StoreDepositSlotActionDiagnostics slotActions) {
        this.bindings = bindings;
        this.emissionGate = emissionGate;
        this.slotActions = slotActions;
    }

    public void clearPredicateSnapshot() {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        lastPredicateSnapshot.remove();
    }

    public int clearForModeTransition() {
        int invalidated = activePredicateSnapshot() == null ? 0 : 1;
        modeEpoch.set(new Object());
        lastPredicateSnapshot.remove();
        return invalidated;
    }

    public boolean hasAutomaticContext(ContainerStoredTracker tracker) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return false;
        }
        StoreDepositOperationState state = bindings.stateFor(tracker);
        return isAutomatic(state);
    }

    public void observeTargetContainerPredicate(ContainerStoredTracker tracker,
                                                Slot slot,
                                                BlockPos targetContainer,
                                                Optional<BlockPos> lastInteraction,
                                                boolean accepted) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            PredicateSnapshot snapshot = PredicateSnapshot.from(lastInteraction, targetContainer, accepted);
            lastPredicateSnapshot.set(new EpochPredicateSnapshot(modeEpoch.get(), snapshot));
        } catch (RuntimeException | LinkageError ignored) {
            lastPredicateSnapshot.set(new EpochPredicateSnapshot(
                    modeEpoch.get(),
                    PredicateSnapshot.unavailable()
            ));
        }
    }

    public void logEffectObservation(ContainerStoredTracker tracker,
                                     Slot slot,
                                     ItemStack before,
                                     ItemStack after,
                                     boolean playerInventorySlot,
                                     boolean acceptPredicateEvaluated,
                                     boolean acceptPredicateResult) {
        logEffectObservation(
                tracker,
                slot,
                before,
                after,
                playerInventorySlot,
                acceptPredicateEvaluated,
                acceptPredicateResult,
                "UNAVAILABLE",
                "UNAVAILABLE"
        );
    }

    public void logEffectObservation(ContainerStoredTracker tracker,
                                     Slot slot,
                                     ItemStack before,
                                     ItemStack after,
                                     boolean playerInventorySlot,
                                     boolean acceptPredicateEvaluated,
                                     boolean acceptPredicateResult,
                                     String trackerTotalBefore,
                                     String trackerTotalAfter) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            StoreDepositOperationState state = bindings.stateFor(tracker);
            TrackerBinding binding = bindings.trackerBinding(tracker);
            if (state == null) {
                lastPredicateSnapshot.remove();
                return;
            }
            boolean automatic = isAutomatic(state);
            PredicateSnapshot snapshot = activePredicateSnapshot();
            if (snapshot == null) {
                snapshot = PredicateSnapshot.unavailable();
            }
            if (automatic && slotActions != null) {
                slotActions.observeTrackerMutation(
                        binding,
                        slot,
                        before,
                        playerInventorySlot
                );
            }
            String outcome = effectOutcome(before, after, playerInventorySlot, acceptPredicateEvaluated, acceptPredicateResult);
            boolean expectedPositiveEffect = "TARGET_CONTAINER".equals(binding == null ? "UNBOUND" : binding.trackerRole())
                    && acceptPredicateResult
                    && positiveDeltaMatchesRequested(state, before, after);
            state.recordEffectObservation(outcome, expectedPositiveEffect);
            String operationId = StoreDepositEventFields.operationId(state);
            String key = operationId + "|" + (binding == null ? "UNBOUND" : binding.trackerRole()) + "|" + outcome + "|" + ChatClefDiagnostics.slotSummary(slot);
            if (automatic && slotActions != null) {
                key += "|" + slotActions.currentSlotMutationId();
            }
            if (emissionGate.shouldEmitDetail(operationId, "STORE_CONTAINER_EFFECT_OBSERVATION", key)) {
                Object[] effectFields = StoreDepositEventFields.effectObservationFields(
                        state,
                        binding,
                        tracker,
                        slot,
                        before,
                        after,
                        playerInventorySlot,
                        acceptPredicateEvaluated,
                        acceptPredicateResult,
                        snapshot.matchReason(),
                        snapshot.lastInteractionPresent(),
                        snapshot.lastInteractionPosition(),
                        outcome,
                        automatic
                );
                if (automatic) {
                    effectFields = StoreDepositEventFields.merge(
                            effectFields,
                            new Object[]{
                                    "trackerTotalBefore", trackerTotalBefore,
                                    "trackerTotalAfter", trackerTotalAfter
                            }
                    );
                    if (slotActions != null) {
                        effectFields = StoreDepositEventFields.merge(
                                effectFields,
                                slotActions.currentMutationFields()
                        );
                    }
                }
                StoreDepositBoundedEventLogger.log("STORE_CONTAINER_EFFECT_OBSERVATION",
                        "store_container_effect_observation",
                        null,
                        ChatClefDiagnostics.withCommandContextFields(
                                effectFields
                        ));
            }
        } catch (RuntimeException | LinkageError ignored) {
        } finally {
            lastPredicateSnapshot.remove();
        }
    }

    private static String effectOutcome(ItemStack before,
                                        ItemStack after,
                                        boolean playerInventorySlot,
                                        boolean acceptPredicateEvaluated,
                                        boolean acceptPredicateResult) {
        if (playerInventorySlot) {
            return "REJECT_PLAYER_INVENTORY_SLOT";
        }
        if (acceptPredicateEvaluated && !acceptPredicateResult) {
            return "REJECT_TARGET_PREDICATE";
        }
        if (!acceptPredicateResult) {
            return "ACCEPT_ZERO_DELTA";
        }
        if (before != null && after != null && before.getItem() != after.getItem()) {
            return "ACCEPT_ITEM_REPLACEMENT";
        }
        int delta = (after == null ? 0 : after.getCount()) - (before == null ? 0 : before.getCount());
        if (delta > 0) {
            return "ACCEPT_POSITIVE_DELTA";
        }
        if (delta < 0) {
            return "ACCEPT_NEGATIVE_DELTA";
        }
        return "ACCEPT_ZERO_DELTA";
    }

    private static boolean positiveDeltaMatchesRequested(StoreDepositOperationState state,
                                                         ItemStack before,
                                                         ItemStack after) {
        if (state == null || after == null || after.isEmpty()) {
            return false;
        }
        if (before != null && !before.isEmpty() && before.getItem() == after.getItem()) {
            return after.getCount() - before.getCount() > 0 && state.matchesRequestedItem(after.getItem());
        }
        return after.getCount() > 0 && state.matchesRequestedItem(after.getItem());
    }

    private static boolean isAutomatic(StoreDepositOperationState state) {
        return state != null
                && state.context() != null
                && state.context().isAutomaticDepositOperation()
                && state.automaticContext().available();
    }

    private PredicateSnapshot activePredicateSnapshot() {
        EpochPredicateSnapshot candidate = lastPredicateSnapshot.get();
        if (candidate == null) {
            return null;
        }
        if (candidate.modeEpoch() != modeEpoch.get()) {
            lastPredicateSnapshot.remove();
            return null;
        }
        return candidate.snapshot();
    }

    private record EpochPredicateSnapshot(Object modeEpoch,
                                          PredicateSnapshot snapshot) {
    }
}
