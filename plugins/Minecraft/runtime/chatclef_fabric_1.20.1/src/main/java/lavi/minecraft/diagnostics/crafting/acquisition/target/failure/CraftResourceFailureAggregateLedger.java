package lavi.minecraft.diagnostics.crafting.acquisition.target.failure;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.Objects;
import java.util.Optional;

//20260901_kpopmodder: Aggregate only ownership-proven failure facts without gameplay reads.
public final class CraftResourceFailureAggregateLedger {
    private String ownerTaskClass = "UNAVAILABLE";
    private long targetAttemptSequence = -1L;
    private CraftResourceAssociationStatus associationStatus =
            CraftResourceAssociationStatus.UNKNOWN;
    private CraftResourceStage resourceStage = CraftResourceStage.UNKNOWN;
    private CraftResourceTargetRole targetRole = CraftResourceTargetRole.UNKNOWN;
    private String targetPosition = "UNAVAILABLE";
    private long unreachableRequestCount;
    private long firstFailureCount = -1L;
    private long lastFailureCount = -1L;
    private long allowedFailures = -1L;
    private Optional<Boolean> unreachableBefore = Optional.empty();
    private Optional<Boolean> unreachableAfter = Optional.empty();
    private long blacklistTransitionCount;
    private long firstObservedTick = -1L;
    private long lastObservedTick = -1L;
    private long suppressedDetailCount;
    private long unownedObservationCount;
    private long unknownAssociationCount;
    private boolean counterSaturated;

    public synchronized void observe(
            CraftResourceFailureObservation observation,
            long currentTargetAttemptSequence,
            Optional<CraftResourceTargetTuple> currentTargetTuple) {
        Objects.requireNonNull(observation, "observation");
        Optional<CraftResourceTargetTuple> tuple = currentTargetTuple == null
                ? Optional.empty()
                : currentTargetTuple;
        if (observation.associationStatus()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            accountUnowned(observation.associationStatus());
            return;
        }

        associationStatus = CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT;
        ownerTaskClass = observation.ownerTaskClass();
        retainTargetIdentity(observation, currentTargetAttemptSequence, tuple);
        if (observation.kind() == CraftResourceFailureKind.UNREACHABLE_REQUEST) {
            unreachableRequestCount = increment(unreachableRequestCount);
        } else {
            blacklistTransitionCount = increment(blacklistTransitionCount);
        }
        retainFailureCounts(observation);
        retainTicks(observation.observedTick());
        if (!observation.sourceEmissionCompleted()) {
            suppressedDetailCount = increment(suppressedDetailCount);
        }
    }

    public synchronized CraftResourceFailureAggregateSnapshot snapshot() {
        return new CraftResourceFailureAggregateSnapshot(
                ownerTaskClass,
                targetAttemptSequence,
                associationStatus,
                resourceStage,
                targetRole,
                targetPosition,
                unreachableRequestCount,
                firstFailureCount,
                lastFailureCount,
                allowedFailures,
                unreachableBefore,
                unreachableAfter,
                blacklistTransitionCount,
                firstObservedTick,
                lastObservedTick,
                suppressedDetailCount,
                unownedObservationCount,
                unknownAssociationCount,
                counterSaturated
        );
    }

    private void retainTargetIdentity(
            CraftResourceFailureObservation observation,
            long currentTargetAttemptSequence,
            Optional<CraftResourceTargetTuple> currentTargetTuple) {
        targetPosition = observation.targetPosition();
        if (currentTargetAttemptSequence <= 0L || currentTargetTuple.isEmpty()) {
            targetAttemptSequence = -1L;
            resourceStage = CraftResourceStage.UNKNOWN;
            targetRole = CraftResourceTargetRole.UNKNOWN;
            return;
        }
        CraftResourceTargetTuple tuple = currentTargetTuple.get();
        if (!tuple.targetPosition().equals(observation.targetPosition())) {
            targetAttemptSequence = -1L;
            resourceStage = CraftResourceStage.UNKNOWN;
            targetRole = CraftResourceTargetRole.UNKNOWN;
            return;
        }
        targetAttemptSequence = currentTargetAttemptSequence;
        resourceStage = tuple.resourceStage();
        targetRole = tuple.targetRole();
        targetPosition = tuple.targetPosition();
    }

    private void retainFailureCounts(CraftResourceFailureObservation observation) {
        if (firstFailureCount < 0L && observation.failureCountBefore() >= 0L) {
            firstFailureCount = observation.failureCountBefore();
        }
        if (observation.failureCountAfter() >= 0L) {
            if (firstFailureCount < 0L) {
                firstFailureCount = observation.failureCountAfter();
            }
            lastFailureCount = observation.failureCountAfter();
        }
        if (observation.allowedFailures() >= 0L) {
            allowedFailures = observation.allowedFailures();
        }
        if (observation.unreachableBefore().isPresent()) {
            if (unreachableBefore.isEmpty()) {
                unreachableBefore = observation.unreachableBefore();
            }
            unreachableAfter = observation.unreachableBefore();
        }
        if (observation.unreachableAfter().isPresent()) {
            unreachableAfter = observation.unreachableAfter();
        }
    }

    private void retainTicks(long observedTick) {
        if (observedTick < 0L) {
            return;
        }
        if (firstObservedTick < 0L) {
            firstObservedTick = observedTick;
        }
        lastObservedTick = observedTick;
    }

    private void accountUnowned(CraftResourceAssociationStatus status) {
        if (status == CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED) {
            unownedObservationCount = increment(unownedObservationCount);
        } else {
            unknownAssociationCount = increment(unknownAssociationCount);
        }
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return Long.MAX_VALUE;
        }
        return value + 1L;
    }
}
