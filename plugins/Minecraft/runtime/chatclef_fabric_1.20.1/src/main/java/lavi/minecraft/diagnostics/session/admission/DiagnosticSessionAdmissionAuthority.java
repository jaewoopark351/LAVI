package lavi.minecraft.diagnostics.session.admission;

import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The sole mutable accounting owner for one diagnostic session.
 *
 * <p>All admission and settlement transitions are serialized here. Formatting and physical
 * output deliberately remain outside this monitor.</p>
 */
public final class DiagnosticSessionAdmissionAuthority {
    private final String diagnosticSessionId;
    private final EnumMap<DiagnosticEventFamily, MutableFamilyCounters> familyCounters =
            new EnumMap<>(DiagnosticEventFamily.class);
    private final IdentityHashMap<DiagnosticAdmissionToken, TokenEmissionState> pendingTokens =
            new IdentityHashMap<>();
    private final Set<DiagnosticAdmissionToken> settledTokens =
            Collections.newSetFromMap(new IdentityHashMap<>());

    private long admittedRequests;
    private long admittedSlots;
    private long ordinarySlotsUsed;
    private long criticalSlotsUsed;
    private long suppressedRequests;
    private long emissionPending;
    private long emissionInProgress;
    private long emissionCompleted;
    private long emissionFailedAfterAdmission;
    private long lastTokenSequence;
    private boolean tokenSequenceSaturated;
    private boolean counterSaturated;
    private boolean capEventClaimed;
    private boolean finalSnapshotAdmitted;
    private DiagnosticCapTrigger capTrigger = DiagnosticCapTrigger.NONE;

    public DiagnosticSessionAdmissionAuthority(String diagnosticSessionId) {
        this(diagnosticSessionId, 0L, 0L);
    }

    DiagnosticSessionAdmissionAuthority(String diagnosticSessionId,
                                        long initialSuppressedRequests,
                                        long initialLastTokenSequence) {
        this.diagnosticSessionId = requireSessionId(diagnosticSessionId);
        if (initialSuppressedRequests < 0L || initialLastTokenSequence < 0L) {
            throw new IllegalArgumentException("Diagnostic seed counters must be non-negative.");
        }
        for (DiagnosticEventFamily family : DiagnosticEventFamily.values()) {
            familyCounters.put(family, new MutableFamilyCounters());
        }
        suppressedRequests = initialSuppressedRequests;
        familyCounters.get(DiagnosticEventFamily.ORDINARY_DETAIL).suppressedRequests =
                initialSuppressedRequests;
        lastTokenSequence = initialLastTokenSequence;
    }

    public synchronized DiagnosticAdmissionDecision admit(DiagnosticAdmissionRequest request) {
        Objects.requireNonNull(request, "request");
        if (!request.modeEligible()) {
            return DiagnosticAdmissionDecision.rejected(
                    DiagnosticAdmissionDecision.RejectionReason.MODE_INELIGIBLE,
                    null,
                    DiagnosticCapTrigger.NONE,
                    snapshotUnsafe()
            );
        }

        DiagnosticEventFamily family = request.family();
        if (family.reservedInternalFamily()) {
            throw new IllegalArgumentException(
                    "Reserved diagnostic families require their dedicated admission method: " + family);
        }

        int slots = family.admissionUnitSlots();
        MutableFamilyCounters counters = familyCounters.get(family);
        if (family.tier() == DiagnosticAdmissionTier.ORDINARY) {
            if (wouldExceed(counters.admittedSlots, slots, family.slotQuota())) {
                return rejectAndMaybeClaimCap(
                        family,
                        DiagnosticAdmissionDecision.RejectionReason.ORDINARY_CEILING_REACHED,
                        DiagnosticCapTrigger.ORDINARY_CEILING_RESERVE_ACTIVE
                );
            }
        } else {
            if (wouldExceed(admittedSlots, slots, normalAdmissionCeiling())) {
                return rejectAndMaybeClaimCap(
                        family,
                        DiagnosticAdmissionDecision.RejectionReason.SHARED_HARD_CAP_REACHED,
                        DiagnosticCapTrigger.SHARED_HARD_CAP
                );
            }
            if (wouldExceed(counters.admittedSlots, slots, family.slotQuota())) {
                recordSuppression(family);
                return DiagnosticAdmissionDecision.rejected(
                        DiagnosticAdmissionDecision.RejectionReason.FAMILY_QUOTA_EXHAUSTED,
                        null,
                        DiagnosticCapTrigger.NONE,
                        snapshotUnsafe()
                );
            }
        }

        DiagnosticAdmissionToken token = grant(family, DiagnosticCapTrigger.NONE);
        return DiagnosticAdmissionDecision.granted(token, snapshotUnsafe());
    }

    public synchronized DiagnosticAdmissionDecision admitFinalSnapshot(boolean modeEligible) {
        if (!modeEligible) {
            return DiagnosticAdmissionDecision.rejected(
                    DiagnosticAdmissionDecision.RejectionReason.MODE_INELIGIBLE,
                    null,
                    DiagnosticCapTrigger.NONE,
                    snapshotUnsafe()
            );
        }
        if (finalSnapshotAdmitted) {
            recordSuppression(DiagnosticEventFamily.FINAL_SNAPSHOT);
            return DiagnosticAdmissionDecision.rejected(
                    DiagnosticAdmissionDecision.RejectionReason.FINAL_SNAPSHOT_ALREADY_ADMITTED,
                    null,
                    DiagnosticCapTrigger.NONE,
                    snapshotUnsafe()
            );
        }
        if (wouldExceed(admittedSlots, DiagnosticSessionLimits.FINAL_SNAPSHOT_SLOTS,
                DiagnosticSessionLimits.HARD_CAP)) {
            recordSuppression(DiagnosticEventFamily.FINAL_SNAPSHOT);
            return DiagnosticAdmissionDecision.rejected(
                    DiagnosticAdmissionDecision.RejectionReason.SHARED_HARD_CAP_REACHED,
                    null,
                    DiagnosticCapTrigger.NONE,
                    snapshotUnsafe()
            );
        }

        finalSnapshotAdmitted = true;
        DiagnosticAdmissionToken token = grant(DiagnosticEventFamily.FINAL_SNAPSHOT, DiagnosticCapTrigger.NONE);
        return DiagnosticAdmissionDecision.granted(token, snapshotUnsafe());
    }

    public synchronized DiagnosticEmissionLease beginEmission(DiagnosticAdmissionToken token) {
        if (!owns(token)) {
            return new DiagnosticEmissionLease(DiagnosticEmissionLease.Status.FOREIGN_TOKEN, snapshotUnsafe());
        }
        if (settledTokens.contains(token)) {
            return new DiagnosticEmissionLease(DiagnosticEmissionLease.Status.ALREADY_SETTLED, snapshotUnsafe());
        }
        TokenEmissionState state = pendingTokens.get(token);
        if (state == null) {
            return new DiagnosticEmissionLease(DiagnosticEmissionLease.Status.FOREIGN_TOKEN, snapshotUnsafe());
        }
        if (state == TokenEmissionState.IN_PROGRESS) {
            return new DiagnosticEmissionLease(DiagnosticEmissionLease.Status.ALREADY_STARTED, snapshotUnsafe());
        }

        pendingTokens.put(token, TokenEmissionState.IN_PROGRESS);
        emissionInProgress = addCounter(emissionInProgress, 1L);
        MutableFamilyCounters counters = familyCounters.get(token.family());
        counters.emissionInProgress = addCounter(counters.emissionInProgress, 1L);
        return new DiagnosticEmissionLease(DiagnosticEmissionLease.Status.STARTED, snapshotUnsafe());
    }

    public synchronized DiagnosticSettlementResult completeEmission(DiagnosticAdmissionToken token) {
        return settle(token, true);
    }

    public synchronized DiagnosticSettlementResult failEmission(DiagnosticAdmissionToken token) {
        return settle(token, false);
    }

    public synchronized DiagnosticSessionSnapshot snapshot() {
        return snapshotUnsafe();
    }

    private DiagnosticAdmissionDecision rejectAndMaybeClaimCap(
            DiagnosticEventFamily rejectedFamily,
            DiagnosticAdmissionDecision.RejectionReason reason,
            DiagnosticCapTrigger trigger) {
        recordSuppression(rejectedFamily);
        DiagnosticAdmissionToken token = claimCanonicalCap(trigger);
        DiagnosticCapTrigger newlyClaimedTrigger = token == null ? DiagnosticCapTrigger.NONE : trigger;
        return DiagnosticAdmissionDecision.rejected(reason, token, newlyClaimedTrigger, snapshotUnsafe());
    }

    private DiagnosticAdmissionToken claimCanonicalCap(DiagnosticCapTrigger trigger) {
        if (capEventClaimed || admittedSlots >= DiagnosticSessionLimits.HARD_CAP) {
            return null;
        }
        capEventClaimed = true;
        capTrigger = trigger;
        return grant(DiagnosticEventFamily.CANONICAL_CAP, trigger);
    }

    private DiagnosticAdmissionToken grant(DiagnosticEventFamily family, DiagnosticCapTrigger tokenCapTrigger) {
        int slots = family.admissionUnitSlots();
        Sequence sequence = nextTokenSequence();
        DiagnosticAdmissionToken token = new DiagnosticAdmissionToken(
                diagnosticSessionId,
                sequence.value,
                sequence.available,
                family,
                slots,
                tokenCapTrigger
        );

        MutableFamilyCounters counters = familyCounters.get(family);
        counters.admittedRequests = addCounter(counters.admittedRequests, 1L);
        counters.admittedSlots = addCounter(counters.admittedSlots, slots);
        counters.emissionPending = addCounter(counters.emissionPending, 1L);
        admittedRequests = addCounter(admittedRequests, 1L);
        admittedSlots = addCounter(admittedSlots, slots);
        emissionPending = addCounter(emissionPending, 1L);
        if (family.tier() == DiagnosticAdmissionTier.ORDINARY) {
            ordinarySlotsUsed = addCounter(ordinarySlotsUsed, slots);
        } else {
            criticalSlotsUsed = addCounter(criticalSlotsUsed, slots);
        }
        pendingTokens.put(token, TokenEmissionState.PENDING);
        return token;
    }

    private DiagnosticSettlementResult settle(DiagnosticAdmissionToken token, boolean completed) {
        if (!owns(token)) {
            return new DiagnosticSettlementResult(
                    DiagnosticSettlementResult.Status.FOREIGN_TOKEN,
                    snapshotUnsafe()
            );
        }
        if (settledTokens.contains(token)) {
            return new DiagnosticSettlementResult(
                    DiagnosticSettlementResult.Status.ALREADY_SETTLED,
                    snapshotUnsafe()
            );
        }
        TokenEmissionState state = pendingTokens.get(token);
        if (state == null) {
            return new DiagnosticSettlementResult(
                    DiagnosticSettlementResult.Status.FOREIGN_TOKEN,
                    snapshotUnsafe()
            );
        }
        if (state != TokenEmissionState.IN_PROGRESS) {
            return new DiagnosticSettlementResult(
                    DiagnosticSettlementResult.Status.NOT_STARTED,
                    snapshotUnsafe()
            );
        }

        pendingTokens.remove(token);
        settledTokens.add(token);
        emissionPending--;
        emissionInProgress--;
        MutableFamilyCounters counters = familyCounters.get(token.family());
        counters.emissionPending--;
        counters.emissionInProgress--;
        if (completed) {
            emissionCompleted = addCounter(emissionCompleted, 1L);
            counters.emissionCompleted = addCounter(counters.emissionCompleted, 1L);
            return new DiagnosticSettlementResult(
                    DiagnosticSettlementResult.Status.COMPLETED,
                    snapshotUnsafe()
            );
        }

        emissionFailedAfterAdmission = addCounter(emissionFailedAfterAdmission, 1L);
        counters.emissionFailedAfterAdmission =
                addCounter(counters.emissionFailedAfterAdmission, 1L);
        return new DiagnosticSettlementResult(
                DiagnosticSettlementResult.Status.FAILED_AFTER_ADMISSION,
                snapshotUnsafe()
        );
    }

    private void recordSuppression(DiagnosticEventFamily family) {
        suppressedRequests = addCounter(suppressedRequests, 1L);
        MutableFamilyCounters counters = familyCounters.get(family);
        counters.suppressedRequests = addCounter(counters.suppressedRequests, 1L);
    }

    private Sequence nextTokenSequence() {
        SaturatingLong.Result result = SaturatingLong.increment(lastTokenSequence);
        if (result.saturated()) {
            lastTokenSequence = Long.MAX_VALUE;
            tokenSequenceSaturated = true;
            counterSaturated = true;
            return new Sequence(0L, false);
        }
        lastTokenSequence = result.value();
        return new Sequence(lastTokenSequence, true);
    }

    private long addCounter(long current, long delta) {
        SaturatingLong.Result result = SaturatingLong.add(current, delta);
        if (result.saturated()) {
            counterSaturated = true;
        }
        return result.value();
    }

    private long normalAdmissionCeiling() {
        int reserved = (capEventClaimed ? 0 : DiagnosticSessionLimits.CANONICAL_CAP_SLOTS)
                + (finalSnapshotAdmitted ? 0 : DiagnosticSessionLimits.FINAL_SNAPSHOT_SLOTS);
        return DiagnosticSessionLimits.HARD_CAP - reserved;
    }

    private boolean owns(DiagnosticAdmissionToken token) {
        return token != null && diagnosticSessionId.equals(token.diagnosticSessionId());
    }

    private DiagnosticSessionSnapshot snapshotUnsafe() {
        EnumMap<DiagnosticEventFamily, DiagnosticFamilySnapshot> snapshots =
                new EnumMap<>(DiagnosticEventFamily.class);
        for (Map.Entry<DiagnosticEventFamily, MutableFamilyCounters> entry : familyCounters.entrySet()) {
            MutableFamilyCounters counters = entry.getValue();
            snapshots.put(entry.getKey(), new DiagnosticFamilySnapshot(
                    counters.admittedRequests,
                    counters.admittedSlots,
                    counters.suppressedRequests,
                    counters.emissionPending,
                    counters.emissionInProgress,
                    counters.emissionCompleted,
                    counters.emissionFailedAfterAdmission
            ));
        }
        return new DiagnosticSessionSnapshot(
                diagnosticSessionId,
                admittedRequests,
                admittedSlots,
                ordinarySlotsUsed,
                criticalSlotsUsed,
                suppressedRequests,
                emissionPending,
                emissionInProgress,
                emissionCompleted,
                emissionFailedAfterAdmission,
                capEventClaimed,
                finalSnapshotAdmitted,
                capTrigger,
                lastTokenSequence,
                !tokenSequenceSaturated,
                counterSaturated,
                snapshots
        );
    }

    private static boolean wouldExceed(long used, int requested, long ceiling) {
        return used > ceiling - requested;
    }

    private static String requireSessionId(String value) {
        String normalized = Objects.requireNonNull(value, "diagnosticSessionId").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("diagnosticSessionId must not be blank.");
        }
        return normalized;
    }

    private enum TokenEmissionState {
        PENDING,
        IN_PROGRESS
    }

    private static final class MutableFamilyCounters {
        private long admittedRequests;
        private long admittedSlots;
        private long suppressedRequests;
        private long emissionPending;
        private long emissionInProgress;
        private long emissionCompleted;
        private long emissionFailedAfterAdmission;
    }

    private record Sequence(long value, boolean available) {
    }
}
