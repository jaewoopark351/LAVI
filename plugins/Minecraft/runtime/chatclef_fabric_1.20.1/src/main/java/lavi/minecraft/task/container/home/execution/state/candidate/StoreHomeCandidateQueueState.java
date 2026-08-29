package lavi.minecraft.task.container.home.execution.state.candidate;

import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedCandidateQueue;

import java.util.Objects;

//20260829_kpopmodder: Added this type file to own only the current trusted candidate queue.
public final class StoreHomeCandidateQueueState {
    private AutoDepositTrustedCandidateQueue current;

    public AutoDepositTrustedCandidateQueue current() {
        return current;
    }

    public void install(AutoDepositTrustedCandidateQueue queue) {
        current = Objects.requireNonNull(queue, "queue");
    }
}
