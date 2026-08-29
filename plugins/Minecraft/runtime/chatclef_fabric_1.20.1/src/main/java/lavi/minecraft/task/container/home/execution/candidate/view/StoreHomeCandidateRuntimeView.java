package lavi.minecraft.task.container.home.execution.candidate.view;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to expose read-only candidate runtime facts.
public final class StoreHomeCandidateRuntimeView {
    private final StoreHomeExecutionState state;

    public StoreHomeCandidateRuntimeView(StoreHomeExecutionState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public int remainingCandidateCount() {
        return state.candidateQueue().current() == null
                ? 0
                : state.candidateQueue().current().remainingCandidateCount();
    }

    public Task activeDiagnosticChild() {
        return state.session().current() == null
                && state.candidateAttempt().current() != null
                ? state.candidateAttempt().current().currentOpenTask()
                : null;
    }
}
