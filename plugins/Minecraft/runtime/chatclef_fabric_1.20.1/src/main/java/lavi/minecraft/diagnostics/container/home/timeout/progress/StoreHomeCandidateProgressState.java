package lavi.minecraft.diagnostics.container.home.timeout.progress;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeDiagnosticLimits;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260828_kpopmodder: Track diagnostic-only progress for exactly one STORE_HOME candidate attempt.
public final class StoreHomeCandidateProgressState {
    private static final String PROGRESS_EVENT_FAMILY =
            "STORE_HOME_CANDIDATE_PROGRESS_SUMMARY";

    private final long operationId;
    private final AutoDepositTrustedDestinationCandidate candidate;
    private final int candidateOrdinal;
    private final int candidateAttemptOrdinal;
    private final int totalCandidateCount;
    private final long candidateStartClientTickId;
    private final boolean candidateStartClientTickKnown;
    private final String playerStartPosition;
    private final Long initialDistanceSquared3d;

    private BlockPos lastPlayerPosition;
    private Long currentDistanceSquared3d;
    private Long bestDistanceSquared3d;
    private long lastMovementClientTickId;
    private long lastDistanceImprovementClientTickId;
    private boolean playerMovementObserved;
    private boolean bestDistanceImprovementObserved;
    private Task childTaskReference;
    private String childTaskClass = "none";
    private String childTaskRunId = "none";
    private int childTaskRunOrdinal;
    private String lastPathingStateKey;
    private String lastGoalStateKey;
    private String lastBindingStateKey;
    private String lastEmittedFingerprint;
    private long lastProgressEmissionClientTickId;
    private int suppressedRepeatCount;
    private boolean exactBindingEverObserved;
    private boolean supportedHandlerEverObserved;
    private int pathingObservationUnavailableCount;

    public StoreHomeCandidateProgressState(
            long operationId,
            AutoDepositTrustedDestinationCandidate candidate,
            int candidateOrdinal,
            int candidateAttemptOrdinal,
            int totalCandidateCount,
            long candidateStartClientTickId,
            boolean candidateStartClientTickKnown,
            StoreHomePhase phase,
            StoreHomeProgressSnapshot initial,
            Task initialChildTask) {
        this.operationId = operationId;
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.candidateOrdinal = Math.max(1, candidateOrdinal);
        this.candidateAttemptOrdinal = Math.max(1, candidateAttemptOrdinal);
        this.totalCandidateCount = Math.max(0, totalCandidateCount);
        this.candidateStartClientTickId = candidateStartClientTickId;
        this.candidateStartClientTickKnown = candidateStartClientTickKnown;
        this.playerStartPosition = initial.playerPosition();
        this.initialDistanceSquared3d = initial.currentDistanceSquared3d();
        this.lastPlayerPosition = initial.playerBlockPosition();
        this.currentDistanceSquared3d = initial.currentDistanceSquared3d();
        this.bestDistanceSquared3d = initial.currentDistanceSquared3d();
        this.lastMovementClientTickId = candidateStartClientTickId;
        this.lastDistanceImprovementClientTickId = candidateStartClientTickId;
        updateChild(initialChildTask);
        this.lastPathingStateKey = initial.pathingStateKey();
        this.lastGoalStateKey = initial.goalStateKey();
        this.lastBindingStateKey = initial.bindingStateKey();
        updateEverObserved(initial);
        this.lastEmittedFingerprint = StoreHomeProgressFingerprint.create(
                PROGRESS_EVENT_FAMILY,
                operationId,
                this.candidateAttemptOrdinal,
                candidate.destinationId(),
                phase,
                "NO_NEW_PROGRESS",
                "candidate_progress_observation",
                childTaskClass,
                childTaskRunOrdinal,
                initial
        );
        this.lastProgressEmissionClientTickId = candidateStartClientTickId;
    }

    public StoreHomeCandidateProgressObservation observe(
            long clientTickId,
            StoreHomePhase phase,
            StoreHomeProgressSnapshot snapshot,
            Task activeChildTask) {
        MovementDelta movement = updatePlayer(
                clientTickId,
                snapshot.playerBlockPosition(),
                snapshot.currentDistanceSquared3d()
        );

        updateChild(activeChildTask);
        boolean pathChanged = !snapshot.pathingStateKey().equals(lastPathingStateKey);
        boolean goalChanged = !snapshot.goalStateKey().equals(lastGoalStateKey);
        boolean bindingChanged = !snapshot.bindingStateKey().equals(lastBindingStateKey);
        lastPathingStateKey = snapshot.pathingStateKey();
        lastGoalStateKey = snapshot.goalStateKey();
        lastBindingStateKey = snapshot.bindingStateKey();
        updateEverObserved(snapshot);

        String progressKind = progressKind(
                bindingChanged,
                goalChanged,
                pathChanged,
                movement.improved(),
                movement.moved()
        );
        String fingerprint = StoreHomeProgressFingerprint.create(
                PROGRESS_EVENT_FAMILY,
                operationId,
                candidateAttemptOrdinal,
                candidate.destinationId(),
                phase,
                progressKind,
                "candidate_progress_observation",
                childTaskClass,
                childTaskRunOrdinal,
                snapshot
        );
        boolean semanticStateChanged = !fingerprint.equals(lastEmittedFingerprint);
        boolean sampleDue = elapsed(
                clientTickId,
                lastProgressEmissionClientTickId
        ) >= StoreHomeDiagnosticLimits.PROGRESS_SAMPLE_INTERVAL_CLIENT_TICKS;
        return new StoreHomeCandidateProgressObservation(
                progressKind,
                fingerprint,
                semanticStateChanged,
                sampleDue
        );
    }

    public void observePlayerOnly(
            long clientTickId,
            StoreHomePlayerPositionSnapshot player,
            Task activeChildTask) {
        updatePlayer(clientTickId, player.position(), player.distanceSquared3d());
        updateChild(activeChildTask);
    }

    public void recordSuppressedRepeat() {
        if (suppressedRepeatCount < Integer.MAX_VALUE) {
            suppressedRepeatCount++;
        }
    }

    public int markProgressEmitted(long clientTickId, String fingerprint) {
        int suppressedBefore = suppressedRepeatCount;
        suppressedRepeatCount = 0;
        lastProgressEmissionClientTickId = clientTickId;
        lastEmittedFingerprint = fingerprint;
        return suppressedBefore;
    }

    public int suppressedRepeatCount() {
        return suppressedRepeatCount;
    }

    private boolean updateChild(Task childTask) {
        if (childTask == childTaskReference) {
            return false;
        }
        childTaskReference = childTask;
        if (childTask == null) {
            childTaskClass = "none";
            childTaskRunId = "none";
            return true;
        }
        if (childTaskRunOrdinal < Integer.MAX_VALUE) {
            childTaskRunOrdinal++;
        }
        childTaskClass = ChatClefDiagnostics.className(childTask);
        childTaskRunId = "store-home-operation-" + operationId
                + "-candidate-attempt-" + candidateAttemptOrdinal
                + "-child-run-" + childTaskRunOrdinal;
        return true;
    }

    private MovementDelta updatePlayer(
            long clientTickId,
            BlockPos playerPosition,
            Long distanceSquared3d) {
        boolean moved = playerPosition != null
                && lastPlayerPosition != null
                && !playerPosition.equals(lastPlayerPosition);
        if (playerPosition != null && lastPlayerPosition == null) {
            moved = true;
        }
        boolean improved = distanceSquared3d != null
                && (bestDistanceSquared3d == null
                || distanceSquared3d < bestDistanceSquared3d);
        if (moved) {
            lastMovementClientTickId = clientTickId;
            playerMovementObserved = true;
        }
        if (improved) {
            bestDistanceSquared3d = distanceSquared3d;
            lastDistanceImprovementClientTickId = clientTickId;
            bestDistanceImprovementObserved = true;
        }
        if (playerPosition != null) {
            lastPlayerPosition = playerPosition;
        }
        currentDistanceSquared3d = distanceSquared3d;
        return new MovementDelta(moved, improved);
    }

    private static String progressKind(
            boolean bindingChanged,
            boolean goalChanged,
            boolean pathChanged,
            boolean improved,
            boolean moved) {
        if (bindingChanged) {
            return "BINDING_STATE_CHANGED";
        }
        if (goalChanged) {
            return "GOAL_STATE_CHANGED";
        }
        if (pathChanged) {
            return "PATHING_STATE_CHANGED";
        }
        if (improved) {
            return "BEST_DISTANCE_IMPROVED";
        }
        if (moved) {
            return "PLAYER_MOVED";
        }
        return "NO_NEW_PROGRESS";
    }

    public String candidateId() {
        return "store-home-operation-" + operationId
                + "-candidate-" + candidateOrdinal;
    }

    public String candidateAttemptId() {
        return "store-home-operation-" + operationId
                + "-candidate-attempt-" + candidateAttemptOrdinal;
    }

    public AutoDepositTrustedDestinationCandidate candidate() {
        return candidate;
    }

    public int candidateOrdinal() {
        return candidateOrdinal;
    }

    public int candidateAttemptOrdinal() {
        return candidateAttemptOrdinal;
    }

    public int totalCandidateCount() {
        return totalCandidateCount;
    }

    public long candidateStartClientTickId() {
        return candidateStartClientTickId;
    }

    public Object candidateStartClientTickIdValue() {
        return candidateStartClientTickKnown
                ? candidateStartClientTickId
                : "unavailable_first_visible_boundary_is_not_actual_start";
    }

    public Object elapsedCandidateClientTicks(long clientTickId) {
        return candidateStartClientTickKnown
                ? elapsed(clientTickId, candidateStartClientTickId)
                : "unavailable_first_visible_boundary_is_not_actual_start";
    }

    public String candidateStartClientTickSource() {
        return candidateStartClientTickKnown
                ? "CANDIDATE_ATTEMPT_CREATED"
                : "FIRST_VISIBLE_BOUNDARY_NOT_ACTUAL_START";
    }

    public String playerStartPosition() {
        return playerStartPosition;
    }

    public String candidateStartObservationSource() {
        return candidateStartClientTickKnown
                ? "CANDIDATE_ATTEMPT_CREATED"
                : "FIRST_VISIBLE_BOUNDARY_NOT_ACTUAL_START";
    }

    public Object initialDistanceSquared3dValue() {
        return value(initialDistanceSquared3d);
    }

    public Object currentDistanceSquared3dValue() {
        return value(currentDistanceSquared3d);
    }

    public Object bestDistanceSquared3dValue() {
        return value(bestDistanceSquared3d);
    }

    public Object lastMovementClientTickId() {
        return playerMovementObserved
                ? lastMovementClientTickId
                : "unavailable_no_player_move_observed";
    }

    public Object lastDistanceImprovementClientTickId() {
        return bestDistanceImprovementObserved
                ? lastDistanceImprovementClientTickId
                : "unavailable_no_best_distance_improvement_observed";
    }

    public Object ticksSinceLastMovement(long clientTickId) {
        return playerMovementObserved
                ? elapsed(clientTickId, lastMovementClientTickId)
                : "unavailable_no_player_move_observed";
    }

    public Object ticksSinceLastDistanceImprovement(long clientTickId) {
        return bestDistanceImprovementObserved
                ? elapsed(clientTickId, lastDistanceImprovementClientTickId)
                : "unavailable_no_best_distance_improvement_observed";
    }

    public Object noMovementTicks(long clientTickId) {
        if (playerMovementObserved) {
            return elapsed(clientTickId, lastMovementClientTickId);
        }
        return candidateStartClientTickKnown
                ? elapsed(clientTickId, candidateStartClientTickId)
                : "unavailable_first_visible_boundary_is_not_actual_start";
    }

    public String childTaskClass() {
        return childTaskClass;
    }

    public String childTaskRunId() {
        return childTaskRunId;
    }

    public int childTaskRunOrdinal() {
        return childTaskRunOrdinal;
    }

    public boolean exactBindingEverObserved() {
        return exactBindingEverObserved;
    }

    public boolean supportedHandlerEverObserved() {
        return supportedHandlerEverObserved;
    }

    public int pathingObservationUnavailableCount() {
        return pathingObservationUnavailableCount;
    }

    private void updateEverObserved(StoreHomeProgressSnapshot snapshot) {
        if ("true".equals(snapshot.exactBindingMatched())) {
            exactBindingEverObserved = true;
            supportedHandlerEverObserved = true;
        }
        if ("ACTIVE_EXACT_SESSION".equals(snapshot.containerSessionState())) {
            supportedHandlerEverObserved = true;
        }
        int unavailableCount = Math.max(
                0, snapshot.pathingObservationUnavailableCount()
        );
        if (unavailableCount > 0) {
            long total = (long) pathingObservationUnavailableCount
                    + unavailableCount;
            pathingObservationUnavailableCount = total >= Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) total;
        }
    }

    private static long elapsed(long current, long start) {
        return current >= start ? current - start : 0L;
    }

    private static Object value(Long value) {
        return value == null ? "unavailable" : value;
    }

    private record MovementDelta(boolean moved, boolean improved) {
    }
}
