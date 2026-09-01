package lavi.minecraft.diagnostics.container.store.deposit.session;

//20260831_kpopmodder: Report diagnostics-only Store state invalidated at one mode boundary.
public record StoreDepositModeTransitionResult(
        int activeStoreOperationCount,
        int activeAutomaticRunCount,
        long invalidatedDiagnosticEntryCount) {

    public boolean invalidatedAnything() {
        return invalidatedDiagnosticEntryCount > 0;
    }
}
