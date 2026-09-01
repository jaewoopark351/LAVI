package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

import java.util.List;
import java.util.Objects;

/**
 * Immutable bounded result from retention observation or diagnostics-mode cleanup.
 */
public record CraftResourceTerminalObservationBatch(
        List<CraftResourceTerminalDecision> decisions,
        List<CraftResourceTerminalKey> expiredTombstoneKeys,
        List<CraftResourceTerminalKey> droppedActiveKeys,
        List<CraftResourceTerminalKey> droppedTombstoneKeys,
        int activeLedgerCount,
        int tombstoneCount,
        long totalExpiredTombstoneCount,
        long totalEvictedTombstoneCount,
        long activeCapacityRefusalCount,
        boolean counterSaturated) {

    public CraftResourceTerminalObservationBatch {
        decisions = List.copyOf(Objects.requireNonNull(decisions, "decisions"));
        expiredTombstoneKeys = List.copyOf(Objects.requireNonNull(
                expiredTombstoneKeys,
                "expiredTombstoneKeys"
        ));
        droppedActiveKeys = List.copyOf(Objects.requireNonNull(
                droppedActiveKeys,
                "droppedActiveKeys"
        ));
        droppedTombstoneKeys = List.copyOf(Objects.requireNonNull(
                droppedTombstoneKeys,
                "droppedTombstoneKeys"
        ));
    }

    public int droppedActiveCount() {
        return droppedActiveKeys.size();
    }

    public int droppedTombstoneCount() {
        return droppedTombstoneKeys.size();
    }
}
