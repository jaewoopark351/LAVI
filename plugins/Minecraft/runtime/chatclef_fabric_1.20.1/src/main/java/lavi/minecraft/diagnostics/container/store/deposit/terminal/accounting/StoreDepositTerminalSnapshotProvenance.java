package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

//20260831_kpopmodder: Own monotonic cumulative-snapshot identity separately from terminal counters.
// Every access is serialized by the owning StoreDepositTerminalLedger monitor.
final class StoreDepositTerminalSnapshotProvenance {
    private long snapshotSequence;
    private boolean sequenceAvailable = true;
    private long previousCreatedSnapshotSequence;
    private long previousEmissionCallsReturnedSnapshotSequence;
    private long coveredThroughTerminalSequence;

    StoreDepositTerminalSnapshotProvenance() {
    }

    StoreDepositTerminalSnapshotProvenance(long snapshotSequence) {
        this.snapshotSequence = Math.max(0L, snapshotSequence);
        this.sequenceAvailable = this.snapshotSequence != Long.MAX_VALUE;
        this.previousCreatedSnapshotSequence = this.snapshotSequence <= 0L
                ? 0L
                : this.snapshotSequence - 1L;
    }

    View observation(long currentTerminalSequence) {
        return new View(
                0L,
                sequenceAvailable,
                snapshotSequence,
                previousEmissionCallsReturnedSnapshotSequence,
                currentTerminalSequence
        );
    }

    Capture capture(long currentTerminalSequence) {
        if (!sequenceAvailable) {
            return new Capture(
                    false,
                    false,
                    observation(currentTerminalSequence)
            );
        }
        long priorCreated = snapshotSequence;
        snapshotSequence++;
        previousCreatedSnapshotSequence = priorCreated;
        coveredThroughTerminalSequence = currentTerminalSequence;
        boolean saturatedNow = snapshotSequence == Long.MAX_VALUE;
        if (saturatedNow) {
            sequenceAvailable = false;
        }
        return new Capture(
                true,
                saturatedNow,
                new View(
                        snapshotSequence,
                        sequenceAvailable,
                        previousCreatedSnapshotSequence,
                        previousEmissionCallsReturnedSnapshotSequence,
                        coveredThroughTerminalSequence
                )
        );
    }

    boolean recordEmissionCallsReturned(long emittedSnapshotSequence) {
        if (emittedSnapshotSequence <= previousEmissionCallsReturnedSnapshotSequence
                || emittedSnapshotSequence <= 0L
                || emittedSnapshotSequence > snapshotSequence) {
            return false;
        }
        previousEmissionCallsReturnedSnapshotSequence = emittedSnapshotSequence;
        return true;
    }

    record View(
            long snapshotSequence,
            boolean snapshotSequenceAvailable,
            long previousCreatedSnapshotSequence,
            long previousEmissionCallsReturnedSnapshotSequence,
            long coveredThroughTerminalSequence) {
    }

    record Capture(boolean created, boolean saturatedNow, View view) {
    }
}
