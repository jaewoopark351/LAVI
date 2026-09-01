package lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Bound command-scoped requirement ledgers to the eight-scope limit.
public final class CraftResourceRequirementProgressRegistry {
    private static final int ACTIVE_SCOPE_LIMIT = 8;
    private final Map<IronPickaxeAcquisitionScopeKey, CraftResourceRequirementProgressLedger>
            ledgers = new LinkedHashMap<>();

    public synchronized boolean activate(IronPickaxeAcquisitionScopeKey key) {
        if (key == null) {
            return false;
        }
        if (ledgers.containsKey(key)) {
            return true;
        }
        if (ledgers.size() >= ACTIVE_SCOPE_LIMIT) {
            return false;
        }
        ledgers.put(key, new CraftResourceRequirementProgressLedger());
        return true;
    }

    public synchronized boolean observe(
            IronPickaxeAcquisitionScopeKey key,
            CraftResourceRequirementProgressObservation observation) {
        CraftResourceRequirementProgressLedger ledger = ledgers.get(key);
        return ledger != null && ledger.observe(observation);
    }

    public synchronized Optional<CraftResourceRequirementProgressSnapshot> snapshot(
            IronPickaxeAcquisitionScopeKey key) {
        CraftResourceRequirementProgressLedger ledger = ledgers.get(key);
        return ledger == null ? Optional.empty() : Optional.of(ledger.snapshot());
    }

    public synchronized void retire(IronPickaxeAcquisitionScopeKey key) {
        ledgers.remove(key);
    }

    public synchronized void clearForModeOff() {
        ledgers.clear();
    }
}
