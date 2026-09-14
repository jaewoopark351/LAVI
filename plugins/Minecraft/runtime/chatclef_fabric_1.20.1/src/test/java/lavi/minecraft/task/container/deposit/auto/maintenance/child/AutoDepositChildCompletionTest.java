package lavi.minecraft.task.container.deposit.auto.maintenance.child;

import lavi.minecraft.task.container.deposit.auto.maintenance.result.AutoDepositRunReason;
import lavi.minecraft.task.container.deposit.auto.trusted.execution.AutoDepositTrustedStoreOutcome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Native finished-with-missing-items and stopped are not substitutes for child success.
class AutoDepositChildCompletionTest {
    @Test
    void generalMissingSourceAndStoppedDoNotBecomeSuccess() {
        assertEquals(AutoDepositRunReason.GENERAL_CHILD_UNCONFIRMED,
                AutoDepositChildCompletion.generalFailure(true, false, false).orElseThrow());
        assertEquals(AutoDepositRunReason.CHILD_STOPPED,
                AutoDepositChildCompletion.generalFailure(false, true, false).orElseThrow());
        assertTrue(AutoDepositChildCompletion.generalFailure(true, false, true).isEmpty());
    }

    @Test
    void trustedFailureUsesTheActualTypedOutcomeAndRetainsItAcrossSafety() {
        assertEquals(AutoDepositRunReason.TRUSTED_CHILD_FAILED,
                AutoDepositChildCompletion.trustedFailure(AutoDepositTrustedStoreOutcome.CANDIDATES_EXHAUSTED, false).orElseThrow());
        assertEquals(AutoDepositRunReason.CONTEXT_CHANGED,
                AutoDepositChildCompletion.trustedFailure(AutoDepositTrustedStoreOutcome.CONTEXT_CHANGED, true).orElseThrow());
        assertEquals(AutoDepositRunReason.CHILD_STOPPED,
                AutoDepositChildCompletion.trustedFailure(AutoDepositTrustedStoreOutcome.PENDING, true).orElseThrow());
        assertTrue(AutoDepositChildCompletion.trustedFailure(AutoDepositTrustedStoreOutcome.ALL_STORED, false).isEmpty());
    }
}
