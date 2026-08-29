package lavi.minecraft.task.container.home.execution.state.lifecycle;

import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.StoreHomeResult;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only STORE_HOME root lifecycle state.
public final class StoreHomeTaskLifecycleState {
    private StoreHomePhase phase = StoreHomePhase.ACCEPT_REQUEST;
    private StoreHomeResult result = StoreHomeResult.PENDING;
    private boolean initialized;
    private String terminalReason = "";

    public StoreHomePhase phase() {
        return phase;
    }

    public void transitionTo(StoreHomePhase phase) {
        StoreHomePhase validatedPhase = Objects.requireNonNull(phase, "phase");
        if (!pending()) {
            return;
        }
        if (validatedPhase == StoreHomePhase.TERMINAL) {
            throw new IllegalArgumentException(
                    "TERMINAL phase is owned by finish"
            );
        }
        this.phase = validatedPhase;
    }

    public StoreHomeResult result() {
        return result;
    }

    public boolean pending() {
        return result == StoreHomeResult.PENDING;
    }

    public boolean initialized() {
        return initialized;
    }

    public void markInitialized() {
        initialized = true;
    }

    public String terminalReason() {
        return terminalReason;
    }

    public void finish(StoreHomeResult result, String terminalReason) {
        if (!pending()) {
            return;
        }
        StoreHomeResult validatedResult = Objects.requireNonNull(result, "result");
        String validatedReason = Objects.requireNonNull(
                terminalReason, "terminalReason"
        );
        if (validatedResult == StoreHomeResult.PENDING) {
            throw new IllegalArgumentException("terminal result must not be PENDING");
        }
        if (validatedReason.isBlank()) {
            throw new IllegalArgumentException("terminalReason must not be blank");
        }
        this.result = validatedResult;
        this.terminalReason = validatedReason;
        phase = StoreHomePhase.TERMINAL;
    }
}
