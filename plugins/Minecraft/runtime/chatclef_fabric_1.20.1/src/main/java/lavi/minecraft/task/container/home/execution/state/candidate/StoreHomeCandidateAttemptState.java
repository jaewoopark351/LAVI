package lavi.minecraft.task.container.home.execution.state.candidate;

import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only the active candidate attempt reference.
public final class StoreHomeCandidateAttemptState {
    private StoreHomeCandidateAttempt current;

    public StoreHomeCandidateAttempt current() {
        return current;
    }

    public void start(StoreHomeCandidateAttempt attempt) {
        current = Objects.requireNonNull(attempt, "attempt");
    }

    public void clear() {
        current = null;
    }
}
