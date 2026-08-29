package lavi.minecraft.task.container.home.execution.timeout;

import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutPhase;
import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutState;
import lavi.minecraft.task.container.home.execution.timeout.candidate.StoreHomeCandidateTimeoutTickObservation;
import lavi.minecraft.task.container.home.execution.timeout.operation.StoreHomeOperationTimeoutState;

import java.util.Objects;
import java.util.Optional;

//20260828_kpopmodder: Own the composed phase-clock lifecycle for one STORE_HOME operation.
public final class StoreHomeTimeoutLifecycle {
    private final StoreHomeTimeoutPolicy policy;
    private final StoreHomeOperationTimeoutState operation =
            new StoreHomeOperationTimeoutState();
    private StoreHomeCandidateTimeoutState candidate;

    public StoreHomeTimeoutLifecycle(StoreHomeTimeoutPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public static StoreHomeTimeoutLifecycle standard() {
        return new StoreHomeTimeoutLifecycle(StoreHomeTimeoutPolicy.standard());
    }

    public void onActiveRootTick() {
        operation.tick();
    }

    public void startCandidate(double targetX, double targetY, double targetZ) {
        if (candidate != null) {
            throw new IllegalStateException("candidate timeout state is already active");
        }
        candidate = new StoreHomeCandidateTimeoutState(
                targetX, targetY, targetZ
        );
    }

    public void observeCandidate(
            boolean playerPositionAvailable,
            double playerX,
            double playerY,
            double playerZ,
            boolean localInteractionHandoffObserved) {
        StoreHomeCandidateTimeoutState current = requireCandidate();
        StoreHomeCandidateTimeoutTickObservation observed = current.tick(
                playerPositionAvailable,
                playerX,
                playerY,
                playerZ,
                localInteractionHandoffObserved,
                policy
        );
        if (observed.semanticNavigationProgress()) {
            operation.recordSemanticProgress();
        }
    }

    public void markCandidateActivated() {
        requireCandidate().markActivated();
        operation.recordSemanticProgress();
    }

    public void recordConfirmedTransfer() {
        operation.recordSemanticProgress();
    }

    public void clearCandidate() {
        candidate = null;
    }

    public Optional<StoreHomeTimeoutReason> operationTimeoutReason() {
        return operation.timeoutReason(policy);
    }

    public Optional<StoreHomeTimeoutReason> operationEmergencyTimeoutReason() {
        return operation.emergencyTimeoutReason(policy);
    }

    public Optional<StoreHomeTimeoutReason> operationNoProgressTimeoutReason() {
        return operation.noProgressTimeoutReason(policy);
    }

    public Optional<StoreHomeTimeoutReason> candidateTimeoutReason() {
        return requireCandidate().timeoutReason(policy);
    }

    public StoreHomeCandidateTimeoutPhase candidatePhase() {
        return requireCandidate().phase();
    }

    public boolean candidateLocalInteractionStarted() {
        return requireCandidate().localInteractionStarted();
    }

    public int candidateActiveTicks() {
        return candidate == null ? 0 : candidate.activeTicks();
    }

    public StoreHomeTimeoutObservation observation() {
        return StoreHomeTimeoutObservation.capture(policy, operation, candidate);
    }

    public StoreHomeTimeoutPolicy policy() {
        return policy;
    }

    private StoreHomeCandidateTimeoutState requireCandidate() {
        if (candidate == null) {
            throw new IllegalStateException("candidate timeout state is not active");
        }
        return candidate;
    }
}
