package lavi.minecraft.task.container.home.execution.candidate;

import adris.altoclef.tasks.InteractWithBlockTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;

import java.util.Objects;

//20260828_kpopmodder: Own only one candidate's existing open-child lifecycle.
public final class StoreHomeCandidateAttempt {
    private final AutoDepositTrustedDestinationCandidate candidate;
    private InteractWithBlockTask openTask;

    public StoreHomeCandidateAttempt(AutoDepositTrustedDestinationCandidate candidate) {
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.openTask = new InteractWithBlockTask(candidate.position());
    }

    public Task openTask() {
        if (openTask == null || openTask.isFinished() || openTask.stopped()) {
            openTask = new InteractWithBlockTask(candidate.position());
        }
        return openTask;
    }

    //20260828_kpopmodder: Expose the existing child for diagnostics without recreating it.
    public Task currentOpenTask() {
        return openTask;
    }

    //20260828_kpopmodder: Reuse the child's side-effect-free reach predicate for local handoff.
    public boolean interactionNeighborhoodReached() {
        return openTask.getCurrentReach().isPresent();
    }

    public AutoDepositTrustedDestinationCandidate candidate() {
        return candidate;
    }

}
