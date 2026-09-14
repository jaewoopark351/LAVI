package lavi.minecraft.task.container.deposit.auto.admission.conditions;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.budget.AutoDepositBudgetStatus;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmPolicy;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmDecision;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.gain.AutoDepositGainEvidence;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: A displayed limit must not silently change the family's gameplay comparison key.
class AutoDepositConditionRearmIntegrationTest {
    @Test void realSafeFoodGainsPermitOnlyTheBoundedRelatedRechecks() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositGainEvidence gains = new AutoDepositGainEvidence();
        Object world = new Object();
        String item = "minecraft:bread";
        AutoDepositConditions initial = gainedConditions(gains.observe(world, 1, Map.of(item, 10), Set.of(item)));
        String reason = "no_safe_surplus";
        policy.markPlanBlocked(reason, initial.scope(), initial.conditionKey(reason));
        for (int count = 11; count <= 12; count++) {
            AutoDepositConditions now = gainedConditions(gains.observe(world, 1, Map.of(item, count), Set.of(item)));
            assertEquals(AutoDepositRearmDecision.RELATED_CHANGE,
                    policy.observe(new DepositAllInventoryPressureSnapshot(36, 36), now.conditionKey(reason), now.scope()));
            policy.markPlanBlocked(reason, now.scope(), now.conditionKey(reason));
            assertEquals(AutoDepositRearmDecision.SAME_FAILURE,
                    policy.observe(new DepositAllInventoryPressureSnapshot(36, 36), now.conditionKey(reason), now.scope()));
        }
        AutoDepositConditions more = gainedConditions(gains.observe(world, 1, Map.of(item, 13), Set.of(item)));
        assertEquals(AutoDepositRearmDecision.BUDGET_EXHAUSTED,
                policy.observe(new DepositAllInventoryPressureSnapshot(36, 36), more.conditionKey(reason), more.scope()));
        assertEquals(2, policy.activeBudget().recoveryGrants());
        assertEquals(0, policy.activeBudget().completedUnits());
    }

    @Test void gainAndEpisodeResetCannotRecoverAnUnchangedDestinationFailure() {
        AutoDepositGainEvidence gains = new AutoDepositGainEvidence();
        Object world = new Object();
        String item = "minecraft:diamond_sword";
        AutoDepositConditions before = gainedConditions(gains.observe(world, 1, Map.of(item, 1), Set.of(item)));
        AutoDepositConditions after = gainedConditions(gains.observe(world, 1, Map.of(item, 2), Set.of(item)));
        assertNotEquals(before.conditionKey("no_safe_surplus"), after.conditionKey("no_safe_surplus"));
        assertEquals(before.conditionKey("GENERAL_CHILD_UNCONFIRMED"), after.conditionKey("GENERAL_CHILD_UNCONFIRMED"));
        AutoDepositConditions nextEpisode = gainedConditions(gains.observe(world, 2, Map.of(item, 2), Set.of(item)));
        assertEquals(after.conditionKey("no_safe_surplus"), nextEpisode.conditionKey("no_safe_surplus"));
    }

    private static AutoDepositConditions gainedConditions(String gains) {
        return new AutoDepositConditions("automatic", Map.of(), Map.of(), Map.of(),
                List.of("general:A:empty=0"), gains);
    }

    @Test
    void exhaustedPartialFollowupsDoNotInventAConditionChangeByRenamingReason() {
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        AutoDepositConditions conditions = new AutoDepositConditions("automatic",
                Map.of("minecraft:cobblestone", 128), Map.of(), Map.of(), List.of("general:A:empty=6"));
        for (int unit = 0; unit < 3; unit++) {
            String admissionKey = conditions.conditionKey(policy.lastReason());
            policy.beginUnit(new DepositAllInventoryPressureSnapshot(36, 36), admissionKey, conditions.scope());
            policy.finishUnit(true, new DepositAllInventoryPressureSnapshot(34, 36), "NORMAL:OBSERVED",
                    conditions.scope(), conditions.conditionKey("NORMAL:OBSERVED"));
        }
        assertEquals(AutoDepositBudgetStatus.FOLLOWUP_LIMIT, policy.activeBudget().status());
        assertFalse(policy.observe(new DepositAllInventoryPressureSnapshot(36, 36),
                conditions.conditionKey(policy.lastReason()), conditions.scope()).canEvaluate());
        assertEquals(0, policy.activeBudget().recoveryGrants());
    }
}
