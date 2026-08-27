package lavi.minecraft.task.container.deposit.auto.trusted.execution;

//20260827_kpopmodder: Report trusted-only completion without fabricating a general fallback.
public enum AutoDepositTrustedStoreOutcome {
    PENDING,
    ALL_STORED,
    CANDIDATES_EXHAUSTED,
    CONTEXT_CHANGED
}
