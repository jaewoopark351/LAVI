package lavi.minecraft.diagnostics.container.home.timeout.state;

import lavi.minecraft.diagnostics.container.home.timeout.progress.StoreHomeCandidateProgressState;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;

//20260829_kpopmodder: Own only the active diagnostic candidate progress lifecycle.
public final class StoreHomeDiagnosticCandidateProgressLifecycle {
    private StoreHomeCandidateProgressState activeCandidate;
    private StoreHomeCandidateAttempt activeAttemptReference;

    public StoreHomeCandidateProgressState activeCandidate() {
        return activeCandidate;
    }

    public void activate(
            StoreHomeCandidateProgressState activeCandidate,
            StoreHomeCandidateAttempt activeAttemptReference) {
        this.activeCandidate = activeCandidate;
        this.activeAttemptReference = activeAttemptReference;
    }

    public void clearActive() {
        activeCandidate = null;
        activeAttemptReference = null;
    }

    public boolean activeMatches(StoreHomeCandidateAttempt attempt) {
        return activeCandidate != null && activeAttemptReference == attempt;
    }
}
