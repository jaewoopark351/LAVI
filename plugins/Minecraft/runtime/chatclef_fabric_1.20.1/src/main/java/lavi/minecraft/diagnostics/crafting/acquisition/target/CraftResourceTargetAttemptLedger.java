package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.closure.CraftResourceTargetClosureSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

//20260901_kpopmodder: Count only ownership-proven semantic target attempts with bounded retention.
public final class CraftResourceTargetAttemptLedger {
    private static final int DETAIL_ELIGIBILITY_LIMIT = 64;
    private static final int HISTORY_FIRST_LIMIT = 4;
    private static final int HISTORY_RECENT_LIMIT = 4;

    private final List<CraftResourceTargetHistorySample> firstHistory = new ArrayList<>(
            HISTORY_FIRST_LIMIT
    );
    private final List<CraftResourceTargetHistorySample> recentHistory = new ArrayList<>(
            HISTORY_RECENT_LIMIT
    );

    private CraftResourceTargetTuple currentTuple;
    private CraftResourceTargetClosureSnapshot lastClosure;
    private long targetAttemptSequence;
    private long attemptTransitionCount;
    private long detailEligibleTransitionCount;
    private long omittedTargetSampleCount;
    private long unreachableRequestCount;
    private long blacklistTransitionCount;
    private long unownedObservationCount;
    private long unknownAssociationCount;
    private boolean counterSaturated;

    public synchronized CraftResourceTargetAttemptDecision observe(
            CraftResourceTargetObservation observation) {
        Objects.requireNonNull(observation, "observation");
        if (observation.associationStatus()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            accountUnowned(observation.associationStatus());
            return new CraftResourceTargetAttemptDecision(
                    false,
                    OptionalLong.empty(),
                    false
            );
        }

        boolean startedNewAttempt = false;
        boolean detailEligible = false;
        switch (observation.observationKind()) {
            case ACTIVE_TARGET_PROVEN -> {
                if (observation.targetTuple().isPresent()) {
                    CraftResourceTargetTuple observedTuple = observation.targetTuple().get();
                    if (!observedTuple.equals(currentTuple)) {
                        currentTuple = observedTuple;
                        targetAttemptSequence = increment(targetAttemptSequence);
                        attemptTransitionCount = increment(attemptTransitionCount);
                        startedNewAttempt = true;
                        detailEligible = attemptTransitionCount <= DETAIL_ELIGIBILITY_LIMIT;
                        if (detailEligible) {
                            detailEligibleTransitionCount = increment(
                                    detailEligibleTransitionCount
                            );
                        }
                        retainHistory(new CraftResourceTargetHistorySample(
                                targetAttemptSequence,
                                observedTuple
                        ));
                    }
                }
            }
            case TARGET_ABANDONED, OWNER_STOP, OWNER_INTERRUPT, COMMAND_TERMINAL -> {
                if (currentTuple != null) {
                    lastClosure = new CraftResourceTargetClosureSnapshot(
                            targetAttemptSequence,
                            currentTuple,
                            observation.observationKind()
                    );
                    currentTuple = null;
                }
            }
            case UNREACHABLE_REQUEST -> unreachableRequestCount = increment(
                    unreachableRequestCount
            );
            case BLACKLIST_STATE_CHANGED -> blacklistTransitionCount = increment(
                    blacklistTransitionCount
            );
            case GOAL_SUBMISSION, CANDIDATE_RETURN, EQUAL_RECONCILIATION,
                    CHAIN_SWITCH, DETAIL_SUPPRESSED, ADMISSION_DENIED -> {
                // These observations do not prove a new active semantic target.
            }
        }

        return new CraftResourceTargetAttemptDecision(
                startedNewAttempt,
                targetAttemptSequence == 0L
                        ? OptionalLong.empty()
                        : OptionalLong.of(targetAttemptSequence),
                detailEligible
        );
    }

    public synchronized CraftResourceTargetAttemptSnapshot snapshot() {
        ArrayList<CraftResourceTargetHistorySample> history = new ArrayList<>(
                firstHistory.size() + recentHistory.size()
        );
        history.addAll(firstHistory);
        history.addAll(recentHistory);
        return new CraftResourceTargetAttemptSnapshot(
                targetAttemptSequence,
                attemptTransitionCount,
                detailEligibleTransitionCount,
                history,
                omittedTargetSampleCount,
                Optional.ofNullable(currentTuple),
                Optional.ofNullable(lastClosure),
                unreachableRequestCount,
                blacklistTransitionCount,
                unownedObservationCount,
                unknownAssociationCount,
                counterSaturated
        );
    }

    public synchronized Optional<CraftResourceTargetTuple> currentTuple() {
        return Optional.ofNullable(currentTuple);
    }

    private void accountUnowned(CraftResourceAssociationStatus associationStatus) {
        if (associationStatus == CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED) {
            unownedObservationCount = increment(unownedObservationCount);
        } else {
            unknownAssociationCount = increment(unknownAssociationCount);
        }
    }

    private void retainHistory(CraftResourceTargetHistorySample sample) {
        if (firstHistory.size() < HISTORY_FIRST_LIMIT) {
            firstHistory.add(sample);
            return;
        }
        if (recentHistory.size() < HISTORY_RECENT_LIMIT) {
            recentHistory.add(sample);
            return;
        }
        recentHistory.remove(0);
        recentHistory.add(sample);
        omittedTargetSampleCount = increment(omittedTargetSampleCount);
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return Long.MAX_VALUE;
        }
        return value + 1L;
    }
}
