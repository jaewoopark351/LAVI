package lavi.minecraft.diagnostics.crafting.acquisition.scope;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

//20260901_kpopmodder: Bound exact iron-pickaxe diagnostic scopes without owning gameplay lifecycle.
public final class IronPickaxeAcquisitionScopeRegistry {
    private static final int MAX_ACTIVE = 8;
    private static final int MAX_TOMBSTONES = 8;
    private static final int MAX_OPAQUE_ID_UTF8_BYTES = 360;
    private static final String IRON_PICKAXE_ID = "minecraft:iron_pickaxe";

    private final Map<IronPickaxeAcquisitionScopeKey, Long> active = new LinkedHashMap<>();
    private final Map<IronPickaxeAcquisitionScopeKey, IronPickaxeAcquisitionTombstone> tombstones =
            new LinkedHashMap<>();
    private long nextRetirementSequence;
    private long activationRefusalCount;
    private long replacedTombstoneCount;
    private long lateEventCount;
    private boolean counterSaturated;

    public synchronized IronPickaxeAcquisitionScopeDecision activate(
            boolean boundaryEnabled,
            String requestedItem,
            IronPickaxeAcquisitionScopeKey key,
            long clientTick,
            long monotonicMs
    ) {
        expireTombstones(clientTick, monotonicMs);
        if (!boundaryEnabled) {
            return decision(false, "DIAGNOSTICS_OFF", key);
        }
        if (!IRON_PICKAXE_ID.equals(requestedItem)) {
            return decision(false, "ITEM_OUT_OF_SCOPE", key);
        }
        String invalidReason = invalidIdentityReason(key);
        if (!invalidReason.isEmpty()) {
            return decision(false, invalidReason, key);
        }
        if (active.containsKey(key)) {
            return decision(true, "ALREADY_ACTIVE", key);
        }
        if (tombstones.containsKey(key)) {
            lateEventCount = increment(lateEventCount);
            return decision(false, "LATE_TOMBSTONE_CANNOT_REOPEN", key);
        }
        if (active.size() >= MAX_ACTIVE) {
            activationRefusalCount = increment(activationRefusalCount);
            return decision(false, "ACTIVE_LEDGER_LIMIT_REACHED", key);
        }
        active.put(key, clientTick);
        return decision(true, "ACTIVATED", key);
    }

    public synchronized boolean retire(
            IronPickaxeAcquisitionScopeKey key,
            long clientTick,
            long monotonicMs
    ) {
        if (key == null || active.remove(key) == null) {
            return false;
        }
        if (tombstones.size() >= MAX_TOMBSTONES) {
            IronPickaxeAcquisitionScopeKey oldest = tombstones.entrySet().stream()
                    .min(Map.Entry.comparingByValue(
                            java.util.Comparator.comparingLong(
                                    IronPickaxeAcquisitionTombstone::retirementSequence
                            )
                    ))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest != null) {
                tombstones.remove(oldest);
                replacedTombstoneCount = increment(replacedTombstoneCount);
            }
        }
        nextRetirementSequence = increment(nextRetirementSequence);
        tombstones.put(
                key,
                new IronPickaxeAcquisitionTombstone(
                        nextRetirementSequence,
                        clientTick,
                        monotonicMs
                )
        );
        return true;
    }

    public synchronized IronPickaxeAcquisitionObservationDisposition observe(
            IronPickaxeAcquisitionScopeKey key,
            long clientTick,
            long monotonicMs
    ) {
        if (key != null && active.containsKey(key)) {
            return IronPickaxeAcquisitionObservationDisposition.ACTIVE;
        }
        IronPickaxeAcquisitionTombstone tombstone = key == null ? null : tombstones.get(key);
        if (tombstone == null) {
            return IronPickaxeAcquisitionObservationDisposition.UNKNOWN_RETIRED;
        }
        if (tombstone.expired(clientTick, monotonicMs)) {
            tombstones.remove(key);
            return IronPickaxeAcquisitionObservationDisposition.UNKNOWN_RETIRED;
        }
        lateEventCount = increment(lateEventCount);
        return IronPickaxeAcquisitionObservationDisposition.LATE_TOMBSTONE;
    }

    public synchronized void expireTombstones(long clientTick, long monotonicMs) {
        tombstones.entrySet().removeIf(entry ->
                entry.getValue().expired(clientTick, monotonicMs));
    }

    public synchronized void clearForModeOff() {
        active.clear();
        tombstones.clear();
        nextRetirementSequence = 0L;
        activationRefusalCount = 0L;
        replacedTombstoneCount = 0L;
        lateEventCount = 0L;
        counterSaturated = false;
    }

    public synchronized IronPickaxeAcquisitionScopeSnapshot snapshot() {
        return new IronPickaxeAcquisitionScopeSnapshot(
                active.size(),
                tombstones.size(),
                active.keySet(),
                tombstones.keySet(),
                activationRefusalCount,
                replacedTombstoneCount,
                lateEventCount,
                counterSaturated
        );
    }

    private String invalidIdentityReason(IronPickaxeAcquisitionScopeKey key) {
        if (key == null) {
            return "UNAVAILABLE_BLANK_ID";
        }
        String[] opaqueIds = {
                key.commandSessionId(),
                key.commandRequestId(),
                key.commandCorrelationId(),
                key.rootAssignmentId(),
                key.boundRootTaskInstanceId()
        };
        for (String opaqueId : opaqueIds) {
            if (opaqueId == null || opaqueId.isBlank()) {
                return "UNAVAILABLE_BLANK_ID";
            }
        }
        for (String opaqueId : opaqueIds) {
            if (opaqueId.getBytes(StandardCharsets.UTF_8).length > MAX_OPAQUE_ID_UTF8_BYTES) {
                return "UNAVAILABLE_OVERSIZE";
            }
        }
        return "";
    }

    private IronPickaxeAcquisitionScopeDecision decision(
            boolean activated,
            String reason,
            IronPickaxeAcquisitionScopeKey key
    ) {
        return new IronPickaxeAcquisitionScopeDecision(activated, reason, key);
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return value;
        }
        return value + 1L;
    }
}
