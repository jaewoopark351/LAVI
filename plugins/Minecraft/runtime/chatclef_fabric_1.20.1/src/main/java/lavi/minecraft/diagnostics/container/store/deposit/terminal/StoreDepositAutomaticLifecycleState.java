package lavi.minecraft.diagnostics.container.store.deposit.terminal;

import adris.altoclef.tasksystem.Task;

//20260830_kpopmodder: Share one diagnostics-only automatic-run identity ledger across projections.
public final class StoreDepositAutomaticLifecycleState {
    private static final StoreDepositAutomaticLifecycleLedger LEDGER =
            new StoreDepositAutomaticLifecycleLedger();

    private StoreDepositAutomaticLifecycleState() {
    }

    public static StoreDepositAutomaticLifecycleLedger ledger() {
        return LEDGER;
    }

    public static StoreDepositAutomaticContext contextForMaintenance(Task maintenanceTask) {
        return LEDGER.contextForMaintenance(maintenanceTask);
    }
}
