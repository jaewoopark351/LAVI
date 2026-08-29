package lavi.minecraft.task.container.home.execution.timeout.decision;

import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutReason;

import java.util.Objects;

//20260828_kpopmodder: Carry one resolved timeout reason and its behavior boundary.
public record StoreHomeTimeoutDecision(
        StoreHomeTimeoutReason reason,
        StoreHomeTimeoutAction action) {

    public StoreHomeTimeoutDecision {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(action, "action");
    }

    public String terminalReason() {
        return action == StoreHomeTimeoutAction.FINISH_TRANSFER_UNCONFIRMED
                ? reason.stableReason() + "_with_pending_transfer"
                : reason.stableReason();
    }
}
