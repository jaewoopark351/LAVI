package lavi.minecraft.task.container.deposit.auto.maintenance.child;

import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreOutcome;

import java.util.Optional;

//20260914_kpopmodder: Classify already-evaluated native child completion without evaluating or executing a Task again.
public final class AutoDepositChildCompletion {
    private AutoDepositChildCompletion() { }

    public static Optional<AutoDepositRunReason> generalFailure(
            boolean finished, boolean stopped, boolean storedTargetsSatisfied) {
        if (stopped) return Optional.of(AutoDepositRunReason.CHILD_STOPPED);
        if (finished && !storedTargetsSatisfied) return Optional.of(AutoDepositRunReason.GENERAL_CHILD_UNCONFIRMED);
        return Optional.empty();
    }

    public static Optional<AutoDepositRunReason> trustedFailure(
            AutoDepositTrustedStoreOutcome outcome, boolean stopped) {
        if (outcome == AutoDepositTrustedStoreOutcome.CONTEXT_CHANGED) {
            return Optional.of(AutoDepositRunReason.CONTEXT_CHANGED);
        }
        if (outcome == AutoDepositTrustedStoreOutcome.CANDIDATES_EXHAUSTED) {
            return Optional.of(AutoDepositRunReason.TRUSTED_CHILD_FAILED);
        }
        if (outcome == AutoDepositTrustedStoreOutcome.PENDING && stopped) {
            return Optional.of(AutoDepositRunReason.CHILD_STOPPED);
        }
        return Optional.empty();
    }
}
