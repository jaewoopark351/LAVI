package lavi.minecraft.task.container.home.execution.timeout.candidate;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutLifecycle;

import java.util.Objects;

//20260829_kpopmodder: Added this observer to feed one candidate timeout sample per active root tick.
public final class StoreHomeCandidateTimeoutObserver {
    private final StoreHomeExecutionState state;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;
    private final StoreHomeTimeoutLifecycle timeoutLifecycle;

    public StoreHomeCandidateTimeoutObserver(
            StoreHomeExecutionState state,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            StoreHomeTimeoutLifecycle timeoutLifecycle) {
        this.state = Objects.requireNonNull(state, "state");
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding, "exactOpenContainerBinding"
        );
        this.timeoutLifecycle = Objects.requireNonNull(
                timeoutLifecycle, "timeoutLifecycle"
        );
    }

    public void observe(AltoClef mod) {
        boolean playerAvailable = mod != null && mod.getPlayer() != null;
        double playerX = playerAvailable ? mod.getPlayer().getX() : 0.0;
        double playerY = playerAvailable ? mod.getPlayer().getY() : 0.0;
        double playerZ = playerAvailable ? mod.getPlayer().getZ() : 0.0;
        boolean localHandoff = state.session().current() != null
                || exactOpenContainerBinding.matches(
                state.candidateAttempt().current().candidate().position()
        )
                || state.candidateAttempt().current().interactionNeighborhoodReached();
        timeoutLifecycle.observeCandidate(
                playerAvailable,
                playerX,
                playerY,
                playerZ,
                localHandoff
        );
    }
}
