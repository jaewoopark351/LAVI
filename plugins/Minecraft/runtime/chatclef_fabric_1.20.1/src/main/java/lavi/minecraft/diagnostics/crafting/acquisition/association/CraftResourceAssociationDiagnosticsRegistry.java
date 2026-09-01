package lavi.minecraft.diagnostics.crafting.acquisition.association;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Bound association ledgers to the same eight exact command scopes.
public final class CraftResourceAssociationDiagnosticsRegistry {
    private static final int ACTIVE_SCOPE_LIMIT = 8;

    private final Map<IronPickaxeAcquisitionScopeKey, CraftResourceAssociationLedger> ledgers =
            new LinkedHashMap<>();
    private long activationRefusalCount;
    private boolean counterSaturated;

    public synchronized boolean activate(IronPickaxeAcquisitionScopeKey key) {
        if (key == null) {
            return false;
        }
        if (ledgers.containsKey(key)) {
            return true;
        }
        if (ledgers.size() >= ACTIVE_SCOPE_LIMIT) {
            activationRefusalCount = increment(activationRefusalCount);
            return false;
        }
        ledgers.put(key, new CraftResourceAssociationLedger());
        return true;
    }

    public synchronized boolean observe(
            IronPickaxeAcquisitionScopeKey key,
            CraftResourceAssociationStatus status
    ) {
        CraftResourceAssociationLedger ledger = ledgers.get(key);
        if (ledger == null || status == null) {
            return false;
        }
        ledger.observe(status);
        return true;
    }

    public synchronized Optional<CraftResourceAssociationLedgerSnapshot> snapshot(
            IronPickaxeAcquisitionScopeKey key
    ) {
        CraftResourceAssociationLedger ledger = ledgers.get(key);
        return ledger == null ? Optional.empty() : Optional.of(ledger.snapshot());
    }

    public synchronized boolean observeObservationGap(
            IronPickaxeAcquisitionScopeKey key,
            String boundary,
            String reason) {
        CraftResourceAssociationLedger ledger = ledgers.get(key);
        if (ledger == null) {
            return false;
        }
        ledger.observeObservationGap(boundary, reason);
        return true;
    }

    public synchronized boolean retire(IronPickaxeAcquisitionScopeKey key) {
        return ledgers.remove(key) != null;
    }

    public synchronized void clearForModeOff() {
        ledgers.clear();
        activationRefusalCount = 0L;
        counterSaturated = false;
    }

    public synchronized int activeScopeCount() {
        return ledgers.size();
    }

    public synchronized long activationRefusalCount() {
        return activationRefusalCount;
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return value;
        }
        return value + 1L;
    }
}
