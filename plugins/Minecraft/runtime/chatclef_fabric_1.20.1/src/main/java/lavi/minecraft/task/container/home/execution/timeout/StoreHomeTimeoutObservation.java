package lavi.minecraft.task.container.home.execution.timeout;

import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutPhase;
import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutState;
import lavi.minecraft.task.container.home.execution.timeout.operation.StoreHomeOperationTimeoutState;

import java.util.Objects;

//20260828_kpopmodder: Expose immutable phase-clock evidence without giving diagnostics behavior control.
public record StoreHomeTimeoutObservation(
        int operationActiveTicks,
        int operationNoProgressTicks,
        int candidateActiveTicks,
        int candidateNavigationNoProgressTicks,
        int candidateLocalInteractionTicks,
        String candidateTimeoutPhase,
        boolean candidateLocalInteractionStarted,
        int maxCandidateNavigationNoProgressTicks,
        int maxCandidateLocalInteractionTicks,
        int maxOperationNoProgressTicks,
        int maxOperationEmergencyHardCapTicks,
        double movementJitterBlocks,
        double bestDistanceImprovementEpsilonBlocks) {

    public static StoreHomeTimeoutObservation initial(
            StoreHomeTimeoutPolicy policy) {
        Objects.requireNonNull(policy, "policy");
        return new StoreHomeTimeoutObservation(
                0,
                0,
                0,
                0,
                0,
                "NO_ACTIVE_CANDIDATE",
                false,
                policy.candidateNavigationNoProgressTicks(),
                policy.candidateLocalInteractionTicks(),
                policy.operationNoProgressTicks(),
                policy.operationEmergencyHardCapTicks(),
                policy.movementJitterBlocks(),
                policy.bestDistanceImprovementEpsilonBlocks()
        );
    }

    public static StoreHomeTimeoutObservation capture(
            StoreHomeTimeoutPolicy policy,
            StoreHomeOperationTimeoutState operation,
            StoreHomeCandidateTimeoutState candidate) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(operation, "operation");
        StoreHomeCandidateTimeoutPhase candidatePhase = candidate == null
                ? null
                : candidate.phase();
        return new StoreHomeTimeoutObservation(
                operation.activeTicks(),
                operation.noProgressTicks(),
                candidate == null ? 0 : candidate.activeTicks(),
                candidate == null ? 0 : candidate.navigationNoProgressTicks(),
                candidate == null ? 0 : candidate.localInteractionTicks(),
                candidatePhase == null ? "NO_ACTIVE_CANDIDATE" : candidatePhase.name(),
                candidate != null && candidate.localInteractionStarted(),
                policy.candidateNavigationNoProgressTicks(),
                policy.candidateLocalInteractionTicks(),
                policy.operationNoProgressTicks(),
                policy.operationEmergencyHardCapTicks(),
                policy.movementJitterBlocks(),
                policy.bestDistanceImprovementEpsilonBlocks()
        );
    }

    public int activeCandidateLimitTicks() {
        if (StoreHomeCandidateTimeoutPhase.NAVIGATE_TO_CANDIDATE.name()
                .equals(candidateTimeoutPhase)) {
            return maxCandidateNavigationNoProgressTicks;
        }
        if (StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE.name()
                .equals(candidateTimeoutPhase)) {
            return maxCandidateLocalInteractionTicks;
        }
        return 0;
    }

    public int activeCandidateTimeoutTicks() {
        if (StoreHomeCandidateTimeoutPhase.NAVIGATE_TO_CANDIDATE.name()
                .equals(candidateTimeoutPhase)) {
            return candidateNavigationNoProgressTicks;
        }
        if (StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE.name()
                .equals(candidateTimeoutPhase)) {
            return candidateLocalInteractionTicks;
        }
        return 0;
    }
}
