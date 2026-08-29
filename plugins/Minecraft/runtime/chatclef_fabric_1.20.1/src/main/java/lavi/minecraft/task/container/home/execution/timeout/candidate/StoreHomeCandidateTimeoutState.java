package lavi.minecraft.task.container.home.execution.timeout.candidate;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;
import lavi.minecraft.task.container.home.execution.timeout.navigation.StoreHomeNavigationNoProgressClock;
import lavi.minecraft.task.container.home.execution.timeout.navigation.StoreHomeNavigationProgressObservation;
import lavi.minecraft.task.container.home.execution.timeout.navigation.StoreHomeNavigationProgressTracker;

import java.util.Optional;

//20260828_kpopmodder: Own only one candidate attempt's phase clocks and sticky local handoff.
public final class StoreHomeCandidateTimeoutState {
    private final StoreHomeNavigationProgressTracker navigationProgress;
    private final StoreHomeNavigationNoProgressClock navigationNoProgressClock =
            new StoreHomeNavigationNoProgressClock();
    private final StoreHomeLocalInteractionClock localInteractionClock =
            new StoreHomeLocalInteractionClock();
    private final StoreHomeCandidateActiveTickClock activeTickClock =
            new StoreHomeCandidateActiveTickClock();

    private StoreHomeCandidateTimeoutPhase phase =
            StoreHomeCandidateTimeoutPhase.NAVIGATE_TO_CANDIDATE;

    public StoreHomeCandidateTimeoutState(
            double targetX,
            double targetY,
            double targetZ) {
        navigationProgress = new StoreHomeNavigationProgressTracker(
                targetX, targetY, targetZ
        );
    }

    public StoreHomeCandidateTimeoutTickObservation tick(
            boolean playerPositionAvailable,
            double playerX,
            double playerY,
            double playerZ,
            boolean localInteractionHandoffObserved,
            StoreHomeTimeoutPolicy policy) {
        activeTickClock.tick();
        if (phase == StoreHomeCandidateTimeoutPhase.ACTIVATED) {
            return new StoreHomeCandidateTimeoutTickObservation(
                    false, false, false, false
            );
        }

        StoreHomeNavigationProgressObservation progress =
                navigationProgress.observe(
                        playerPositionAvailable,
                        playerX,
                        playerY,
                        playerZ,
                        policy
                );
        boolean enteredLocalInteraction = false;
        if (phase == StoreHomeCandidateTimeoutPhase.NAVIGATE_TO_CANDIDATE
                && localInteractionHandoffObserved) {
            phase = StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE;
            enteredLocalInteraction = localInteractionClock.startOnce();
        }

        if (phase == StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE) {
            localInteractionClock.tickIfStarted();
        } else {
            navigationNoProgressClock.tick(progress.semanticProgress());
        }
        return new StoreHomeCandidateTimeoutTickObservation(
                progress.semanticProgress(),
                progress.playerMovementProgress(),
                progress.bestDistanceProgress(),
                enteredLocalInteraction
        );
    }

    public void markActivated() {
        phase = StoreHomeCandidateTimeoutPhase.ACTIVATED;
    }

    public Optional<StoreHomeTimeoutReason> timeoutReason(
            StoreHomeTimeoutPolicy policy) {
        if (phase == StoreHomeCandidateTimeoutPhase.NAVIGATE_TO_CANDIDATE
                && navigationNoProgressClock.elapsedTicks()
                >= policy.candidateNavigationNoProgressTicks()) {
            return Optional.of(
                    StoreHomeTimeoutReason.CANDIDATE_NAVIGATION_NO_PROGRESS
            );
        }
        if (phase == StoreHomeCandidateTimeoutPhase.OPEN_AND_BIND_CANDIDATE
                && localInteractionClock.elapsedTicks()
                >= policy.candidateLocalInteractionTicks()) {
            return Optional.of(
                    StoreHomeTimeoutReason.CANDIDATE_LOCAL_INTERACTION_TIMEOUT
            );
        }
        return Optional.empty();
    }

    public StoreHomeCandidateTimeoutPhase phase() {
        return phase;
    }

    public int activeTicks() {
        return activeTickClock.elapsedTicks();
    }

    public int navigationNoProgressTicks() {
        return navigationNoProgressClock.elapsedTicks();
    }

    public int localInteractionTicks() {
        return localInteractionClock.elapsedTicks();
    }

    public boolean localInteractionStarted() {
        return localInteractionClock.started();
    }

}
