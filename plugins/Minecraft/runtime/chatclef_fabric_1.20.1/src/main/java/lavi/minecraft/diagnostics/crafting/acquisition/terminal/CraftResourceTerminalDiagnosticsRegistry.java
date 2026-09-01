package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Bounded exact-key registry around the pure two-phase terminal ledger.
 */
public final class CraftResourceTerminalDiagnosticsRegistry {
    public static final int MAX_ACTIVE_LEDGERS = 8;
    public static final int MAX_FINALIZED_TOMBSTONES = 8;

    private final Map<CraftResourceTerminalKey, CraftResourceTerminalLedger> activeLedgers =
            new LinkedHashMap<>();
    private final Map<CraftResourceTerminalKey, CraftResourceTerminalTombstone> tombstones =
            new LinkedHashMap<>();

    private long nextRetirementSequence;
    private long totalExpiredTombstoneCount;
    private long totalEvictedTombstoneCount;
    private long activeCapacityRefusalCount;
    private boolean counterSaturated;

    public synchronized CraftResourceTerminalActivation activate(
            CraftResourceTerminalKey key,
            long clientTick,
            long monotonicNanos) {
        Objects.requireNonNull(key, "key");
        expireTombstones(clientTick, monotonicNanos);

        if (activeLedgers.containsKey(key)) {
            return activation(
                    CraftResourceTerminalActivationStatus.ALREADY_ACTIVE,
                    key,
                    "EXACT_TERMINAL_KEY_ALREADY_ACTIVE"
            );
        }
        CraftResourceTerminalTombstone existingTombstone = tombstones.get(key);
        if (existingTombstone != null) {
            existingTombstone.ledger().recordLateSignal("DUPLICATE_TERMINAL_KEY_ACTIVATION");
            return activation(
                    CraftResourceTerminalActivationStatus.FINALIZED_TOMBSTONE_PRESENT,
                    key,
                    "EXACT_TERMINAL_KEY_FINALIZED"
            );
        }
        if (activeLedgers.size() >= MAX_ACTIVE_LEDGERS) {
            activeCapacityRefusalCount = increment(activeCapacityRefusalCount);
            return activation(
                    CraftResourceTerminalActivationStatus.ACTIVE_CAPACITY_REACHED,
                    key,
                    "ACTIVE_TERMINAL_LEDGER_CAP_REACHED"
            );
        }

        activeLedgers.put(key, new CraftResourceTerminalLedger(key, clientTick, monotonicNanos));
        return activation(
                CraftResourceTerminalActivationStatus.ACTIVATED,
                key,
                "AUTHORITATIVE_TERMINAL_KEY_ACTIVATED"
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordDetach(
            CraftResourceTerminalKey key,
            String reason,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordDetach(reason, clientTick, monotonicNanos)
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordPrimaryCause(
            CraftResourceTerminalKey key,
            CraftResourcePrimaryTerminationCause cause,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordPrimaryCause(cause, clientTick, monotonicNanos)
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordOwnedRootCancellation(
            CraftResourceTerminalKey key,
            long invocationId,
            String terminationKind,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordOwnedRootCancellation(
                        invocationId,
                        terminationKind,
                        clientTick,
                        monotonicNanos
                )
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordTaskFinished(
            CraftResourceTerminalKey key,
            CraftResourceTaskFinishMatch match,
            String terminationKind,
            boolean thisOrChildTimedOut,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordTaskFinished(
                        match,
                        terminationKind,
                        thisOrChildTimedOut,
                        clientTick,
                        monotonicNanos
                )
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordClassification(
            CraftResourceTerminalKey key,
            String resultStatus,
            String resultReason,
            String resultFidelity,
            String conclusion,
            long clientTick,
            long monotonicNanos) {
        return recordClassification(
                key,
                "UNAVAILABLE",
                resultStatus,
                resultReason,
                resultFidelity,
                conclusion,
                clientTick,
                monotonicNanos
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordClassification(
            CraftResourceTerminalKey key,
            String terminalDecisionReason,
            String resultStatus,
            String resultReason,
            String resultFidelity,
            String conclusion,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordClassification(
                        terminalDecisionReason,
                        resultStatus,
                        resultReason,
                        resultFidelity,
                        conclusion
                )
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordSendOutcome(
            CraftResourceTerminalKey key,
            CraftResourceResultSendStatus sendStatus,
            CraftResourceResultDeliveryStatus deliveryStatus,
            boolean sent,
            boolean inFlight,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordSendOutcome(
                        sendStatus,
                        deliveryStatus,
                        sent,
                        inFlight
                )
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordLifecycleCleared(
            CraftResourceTerminalKey key,
            CraftResourceLifecycleClearKind clearKind,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordLifecycleCleared(clearKind)
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordQueueContextCleared(
            CraftResourceTerminalKey key,
            boolean mutationApplied,
            String unbindReason,
            long clientTick,
            long monotonicNanos) {
        return mutate(
                key,
                clientTick,
                monotonicNanos,
                ledger -> ledger.recordQueueContextCleared(mutationApplied, unbindReason)
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordAdmissionOutcome(
            CraftResourceTerminalKey key,
            boolean admitted,
            long clientTick,
            long monotonicNanos) {
        return recordAdmissionOutcome(
                key,
                admitted,
                admitted,
                clientTick,
                monotonicNanos
        );
    }

    public synchronized Optional<CraftResourceTerminalDecision> recordAdmissionOutcome(
            CraftResourceTerminalKey key,
            boolean admitted,
            boolean emissionCompleted,
            long clientTick,
            long monotonicNanos) {
        Objects.requireNonNull(key, "key");
        expireTombstones(clientTick, monotonicNanos);
        CraftResourceTerminalTombstone tombstone = tombstones.get(key);
        if (tombstone == null || !tombstone.ledger().recordAdmissionOutcome(
                admitted,
                emissionCompleted
        )) {
            return Optional.empty();
        }
        return Optional.of(new CraftResourceTerminalDecision(
                true,
                false,
                admitted
                        ? "TERMINAL_ADMISSION_RECORDED_ADMITTED"
                        : "TERMINAL_ADMISSION_RECORDED_DENIED",
                tombstone.ledger().snapshot()
        ));
    }

    public synchronized CraftResourceTerminalObservationBatch observeRetentionAll(
            long clientTick,
            long monotonicNanos) {
        List<CraftResourceTerminalKey> expired = expireTombstones(
                clientTick,
                monotonicNanos
        );
        List<CraftResourceTerminalDecision> decisions = new ArrayList<>();
        for (CraftResourceTerminalKey key : List.copyOf(activeLedgers.keySet())) {
            CraftResourceTerminalLedger ledger = activeLedgers.get(key);
            if (ledger == null) {
                continue;
            }
            CraftResourceTerminalDecision decision = ledger.observeRetention(
                    clientTick,
                    monotonicNanos
            );
            if (decision.summaryRequested()) {
                decisions.add(decision);
                retire(key, ledger, clientTick, monotonicNanos);
            }
        }
        return batch(decisions, expired, List.of(), List.of());
    }

    public synchronized CraftResourceTerminalObservationBatch clearForModeOff() {
        List<CraftResourceTerminalKey> droppedActive = List.copyOf(activeLedgers.keySet());
        List<CraftResourceTerminalKey> droppedTombstones = List.copyOf(tombstones.keySet());
        CraftResourceTerminalObservationBatch result = batch(
                List.of(),
                List.of(),
                droppedActive,
                droppedTombstones,
                0,
                0
        );

        activeLedgers.clear();
        tombstones.clear();
        nextRetirementSequence = 0L;
        totalExpiredTombstoneCount = 0L;
        totalEvictedTombstoneCount = 0L;
        activeCapacityRefusalCount = 0L;
        counterSaturated = false;
        return result;
    }

    public synchronized int activeLedgerCount() {
        return activeLedgers.size();
    }

    public synchronized int tombstoneCount() {
        return tombstones.size();
    }

    private Optional<CraftResourceTerminalDecision> mutate(
            CraftResourceTerminalKey key,
            long clientTick,
            long monotonicNanos,
            Function<CraftResourceTerminalLedger, CraftResourceTerminalDecision> mutation) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(mutation, "mutation");
        expireTombstones(clientTick, monotonicNanos);

        CraftResourceTerminalLedger active = activeLedgers.get(key);
        if (active != null) {
            CraftResourceTerminalDecision decision = mutation.apply(active);
            if (decision.finalized()) {
                retire(key, active, clientTick, monotonicNanos);
            }
            return Optional.of(decision);
        }

        CraftResourceTerminalTombstone tombstone = tombstones.get(key);
        if (tombstone == null) {
            return Optional.empty();
        }
        return Optional.of(mutation.apply(tombstone.ledger()));
    }

    private void retire(
            CraftResourceTerminalKey key,
            CraftResourceTerminalLedger ledger,
            long clientTick,
            long monotonicNanos) {
        activeLedgers.remove(key);
        if (!tombstones.containsKey(key)
                && tombstones.size() >= MAX_FINALIZED_TOMBSTONES) {
            CraftResourceTerminalTombstone oldest = oldestTombstone();
            if (oldest != null) {
                tombstones.remove(oldest.key());
                totalEvictedTombstoneCount = increment(totalEvictedTombstoneCount);
            }
        }

        long retirementSequence = nextRetirementSequence;
        nextRetirementSequence = increment(nextRetirementSequence);
        tombstones.put(
                key,
                new CraftResourceTerminalTombstone(
                        key,
                        ledger,
                        clientTick,
                        monotonicNanos,
                        retirementSequence
                )
        );
    }

    private CraftResourceTerminalTombstone oldestTombstone() {
        CraftResourceTerminalTombstone oldest = null;
        for (CraftResourceTerminalTombstone candidate : tombstones.values()) {
            if (oldest == null
                    || candidate.retirementSequence() < oldest.retirementSequence()) {
                oldest = candidate;
            }
        }
        return oldest;
    }

    private List<CraftResourceTerminalKey> expireTombstones(
            long clientTick,
            long monotonicNanos) {
        List<CraftResourceTerminalKey> expired = new ArrayList<>();
        for (Map.Entry<CraftResourceTerminalKey, CraftResourceTerminalTombstone> entry
                : tombstones.entrySet()) {
            if (entry.getValue().expired(clientTick, monotonicNanos)) {
                expired.add(entry.getKey());
            }
        }
        for (CraftResourceTerminalKey key : expired) {
            tombstones.remove(key);
            totalExpiredTombstoneCount = increment(totalExpiredTombstoneCount);
        }
        return List.copyOf(expired);
    }

    private CraftResourceTerminalActivation activation(
            CraftResourceTerminalActivationStatus status,
            CraftResourceTerminalKey key,
            String reason) {
        return new CraftResourceTerminalActivation(
                status,
                key,
                reason,
                activeLedgers.size(),
                tombstones.size(),
                activeCapacityRefusalCount,
                counterSaturated
        );
    }

    private CraftResourceTerminalObservationBatch batch(
            List<CraftResourceTerminalDecision> decisions,
            List<CraftResourceTerminalKey> expired,
            List<CraftResourceTerminalKey> droppedActive,
            List<CraftResourceTerminalKey> droppedTombstones) {
        return batch(
                decisions,
                expired,
                droppedActive,
                droppedTombstones,
                activeLedgers.size(),
                tombstones.size()
        );
    }

    private CraftResourceTerminalObservationBatch batch(
            List<CraftResourceTerminalDecision> decisions,
            List<CraftResourceTerminalKey> expired,
            List<CraftResourceTerminalKey> droppedActive,
            List<CraftResourceTerminalKey> droppedTombstones,
            int activeCount,
            int tombstoneCount) {
        return new CraftResourceTerminalObservationBatch(
                decisions,
                expired,
                droppedActive,
                droppedTombstones,
                activeCount,
                tombstoneCount,
                totalExpiredTombstoneCount,
                totalEvictedTombstoneCount,
                activeCapacityRefusalCount,
                counterSaturated
        );
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return value;
        }
        return value + 1L;
    }
}
