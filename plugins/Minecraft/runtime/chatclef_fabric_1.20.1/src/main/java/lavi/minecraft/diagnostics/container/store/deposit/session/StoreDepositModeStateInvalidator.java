package lavi.minecraft.diagnostics.container.store.deposit.session;

import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerCandidateCollector;
import lavi.minecraft.diagnostics.container.store.deposit.effect.StoreDepositEffectDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositSlotActionDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferAttemptRegistry;

import java.util.Objects;

//20260831_kpopmodder: Invalidate only Store diagnostics state, never gameplay or Task ownership.
public final class StoreDepositModeStateInvalidator {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositInteractionDiagnostics interactions;
    private final StoreDepositTransferAttemptRegistry transferAttempts;
    private final StoreDepositSlotActionDiagnostics slotActions;
    private final StoreDepositEffectDiagnostics effects;
    private final StoreDepositEmissionGate emissionGate;
    private final StoreDepositAutomaticLifecycleLedger automaticLedger;

    public StoreDepositModeStateInvalidator(
            StoreDepositBindingRegistry bindings,
            StoreDepositInteractionDiagnostics interactions,
            StoreDepositTransferAttemptRegistry transferAttempts,
            StoreDepositSlotActionDiagnostics slotActions,
            StoreDepositEffectDiagnostics effects,
            StoreDepositEmissionGate emissionGate,
            StoreDepositAutomaticLifecycleLedger automaticLedger) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.interactions = Objects.requireNonNull(interactions, "interactions");
        this.transferAttempts = Objects.requireNonNull(transferAttempts, "transferAttempts");
        this.slotActions = Objects.requireNonNull(slotActions, "slotActions");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.emissionGate = Objects.requireNonNull(emissionGate, "emissionGate");
        this.automaticLedger = Objects.requireNonNull(automaticLedger, "automaticLedger");
    }

    public StoreDepositModeTransitionResult clear() {
        int activeStoreOperations = bindings.activeOperationCount();
        int activeAutomaticRuns = automaticLedger.activeRunCount();
        long invalidated = 0L;
        invalidated = add(invalidated, interactions.clearForModeTransition());
        invalidated = add(invalidated, transferAttempts.clearForModeTransition());
        invalidated = add(invalidated, slotActions.clearForModeTransition());
        invalidated = add(invalidated, effects.clearForModeTransition());
        invalidated = add(invalidated, StoreContainerCandidateCollector.clearForModeTransition());
        invalidated = add(invalidated, emissionGate.clearForModeTransition());
        invalidated = add(invalidated, automaticLedger.clearForModeTransition().totalEntryCount());
        invalidated = add(invalidated, bindings.clearForModeTransition().totalEntryCount());
        return new StoreDepositModeTransitionResult(
                activeStoreOperations,
                activeAutomaticRuns,
                invalidated
        );
    }

    private static long add(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }
}
