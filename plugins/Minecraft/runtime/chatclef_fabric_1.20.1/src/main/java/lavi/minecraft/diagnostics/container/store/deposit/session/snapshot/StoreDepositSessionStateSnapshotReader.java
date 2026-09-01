package lavi.minecraft.diagnostics.container.store.deposit.session.snapshot;

import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.session.StoreDepositModeTransitionResult;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;

import java.util.Objects;

//20260831_kpopmodder: Read only bounded Store session state for final evidence projection.
public final class StoreDepositSessionStateSnapshotReader {
    private final StoreDepositBindingRegistry bindings;
    private final StoreDepositAutomaticLifecycleLedger automaticLedger;

    public StoreDepositSessionStateSnapshotReader(
            StoreDepositBindingRegistry bindings,
            StoreDepositAutomaticLifecycleLedger automaticLedger) {
        this.bindings = Objects.requireNonNull(bindings, "bindings");
        this.automaticLedger = Objects.requireNonNull(automaticLedger, "automaticLedger");
    }

    public StoreDepositModeTransitionResult snapshot() {
        int activeStoreOperations = bindings.activeOperationCount();
        int activeAutomaticRuns = automaticLedger.activeRunCount();
        return new StoreDepositModeTransitionResult(
                activeStoreOperations,
                activeAutomaticRuns,
                (long) activeStoreOperations + activeAutomaticRuns
        );
    }
}
