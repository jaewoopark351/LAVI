package lavi.minecraft.task.container.deposit.auto.recovery;

import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static lavi.minecraft.testsupport.TestItems.item;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutoDepositRecoveryTransferPlanTest {
    @Test
    void confirmedDestinationNeverWithdrawsBeyondItsManifestEvidence() {
        Item confirmed = item();
        Item unconfirmed = item();
        AutoDepositRecoveryCandidate candidate = new AutoDepositRecoveryCandidate(
                new BlockPos(1, 64, 2),
                AutoDepositRecoveryCandidate.Tier.CONFIRMED_DESTINATION,
                Map.of(confirmed, 2)
        );

        AutoDepositRecoveryTransferPlan plan = AutoDepositRecoveryTransferPlan.create(
                Map.of(confirmed, 4, unconfirmed, 1),
                Map.of(confirmed, 5, unconfirmed, 3),
                candidate
        );

        assertFalse(plan.isEmpty());
        assertEquals(Map.of(confirmed, 6), plan.targetInventoryCounts());
        assertEquals(Map.of(confirmed, 2), plan.withdrawalLimits());
    }
}
