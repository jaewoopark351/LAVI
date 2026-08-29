package lavi.minecraft.diagnostics.container.home.timeout.progress;

import lavi.minecraft.task.container.home.execution.StoreHomePhase;

//20260828_kpopmodder: Build dedupe keys only from bounded semantic STORE_HOME state.
public final class StoreHomeProgressFingerprint {
    private static final int MAX_COMPONENT_LENGTH = 96;
    private static final int MAX_FINGERPRINT_LENGTH = 768;

    private StoreHomeProgressFingerprint() {
    }

    public static String create(
            String eventFamily,
            long operationId,
            int candidateAttemptOrdinal,
            String destinationId,
            StoreHomePhase phase,
            String progressKind,
            String semanticReason,
            String childTaskClass,
            int childTaskRunOrdinal,
            StoreHomeProgressSnapshot snapshot) {
        String fingerprint = String.join(
                "|",
                bounded(eventFamily),
                "operation=" + Math.max(1L, operationId),
                "candidateAttempt=" + Math.max(1, candidateAttemptOrdinal),
                bounded(destinationId),
                bounded(phase == null ? "unavailable" : phase.name()),
                bounded(progressKind),
                bounded(semanticReason),
                bounded(childTaskClass),
                "childRun=" + Math.max(0, childTaskRunOrdinal),
                bounded(snapshot.baritonePathingActive()),
                bounded(snapshot.baritoneCalculationState()),
                bounded(snapshot.baritonePathPresent()),
                bounded(snapshot.customGoalActive()),
                bounded(snapshot.normalizedGoalType()),
                bounded(snapshot.goalMatchesCandidatePosition()),
                bounded(snapshot.exactBindingMatched()),
                bounded(snapshot.screenClass()),
                bounded(snapshot.handlerClass()),
                bounded(snapshot.containerSessionState()),
                bounded(snapshot.sessionPendingTransfer()),
                bounded(snapshot.executorPendingTransfer()),
                coarseDistanceBucket(snapshot.currentDistanceSquared3d())
        );
        return fingerprint.length() <= MAX_FINGERPRINT_LENGTH
                ? fingerprint
                : fingerprint.substring(0, MAX_FINGERPRINT_LENGTH);
    }

    public static String coarseDistanceBucket(Long distanceSquared3d) {
        if (distanceSquared3d == null) {
            return "DISTANCE_UNAVAILABLE";
        }
        if (distanceSquared3d <= 4L) {
            return "DISTANCE_AT_TARGET";
        }
        if (distanceSquared3d <= 64L) {
            return "DISTANCE_NEAR";
        }
        if (distanceSquared3d <= 1_024L) {
            return "DISTANCE_LOCAL";
        }
        if (distanceSquared3d <= 16_384L) {
            return "DISTANCE_MID";
        }
        if (distanceSquared3d <= 262_144L) {
            return "DISTANCE_FAR";
        }
        return "DISTANCE_VERY_FAR";
    }

    private static String bounded(String value) {
        if (value == null || value.isBlank()) {
            return "unavailable";
        }
        String flattened = value
                .replace('|', '_')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
        return flattened.length() <= MAX_COMPONENT_LENGTH
                ? flattened
                : flattened.substring(0, MAX_COMPONENT_LENGTH);
    }
}
