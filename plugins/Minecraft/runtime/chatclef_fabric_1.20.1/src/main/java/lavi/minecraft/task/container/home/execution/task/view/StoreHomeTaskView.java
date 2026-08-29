package lavi.minecraft.task.container.home.execution.task.view;

import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;
import lavi.minecraft.task.container.home.execution.state.StoreHomeExecutionState;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.result.StoreHomeOutcome;

import java.util.Objects;
import java.util.Optional;

//20260829_kpopmodder: Project StoreHomeTask state into its stable public and debug views.
public final class StoreHomeTaskView {
    private final StoreHomeExecutionState state;

    public StoreHomeTaskView(StoreHomeExecutionState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public boolean finished() {
        return !state.lifecycle().pending();
    }

    public String debugString() {
        String destination = state.candidateAttempt().current() == null
                ? "none"
                : state.candidateAttempt().current().candidate().destinationId();
        int sessionOrdinal = state.session().current() == null
                ? -1
                : state.session().current().ordinal();
        return "Store home: phase=" + state.lifecycle().phase()
                + ", result=" + state.lifecycle().result()
                + ", destination=" + destination
                + ", session=" + sessionOrdinal;
    }

    public StoreHomeResult result() {
        return state.lifecycle().result();
    }

    public StoreHomePhase phase() {
        return state.lifecycle().phase();
    }

    public HomeStoragePlan plan() {
        return state.publishedPlan().current();
    }

    public Optional<StoreHomeOutcome> outcome() {
        if (state.lifecycle().pending()
                || state.lifecycle().terminalReason().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(StoreHomeOutcome.terminal(
                state.lifecycle().result(),
                state.operation().current().storedItems(),
                state.operation().current().latestRemainingStacks(),
                state.lifecycle().terminalReason()
        ));
    }
}
