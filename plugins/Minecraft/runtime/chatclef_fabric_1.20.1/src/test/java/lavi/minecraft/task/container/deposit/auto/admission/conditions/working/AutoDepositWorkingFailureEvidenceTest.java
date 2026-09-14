package lavi.minecraft.task.container.deposit.auto.admission.conditions.working;

import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.AutoDepositConditions;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmDecision;
import lavi.minecraft.task.container.deposit.auto.rearm.AutoDepositRearmPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Retain original failed requirements independently of replanning and survival consumption.
class AutoDepositWorkingFailureEvidenceTest {
    private static final String SWORD = "minecraft:diamond_sword";
    private static final String FOOD = "minecraft:bread";
    private static final String REASON = "WORKING_SET_DEFICIT";

    @Test void onlyCompleteRestorationOfOriginalFoodAndEquipmentChangesTheFailureKey() {
        AutoDepositWorkingFailureEvidence evidence = new AutoDepositWorkingFailureEvidence();
        Object world = new Object(), dimension = new Object(), root = new Object();
        String failed = evidence.capture(world, dimension, root, Map.of(SWORD, 1, FOOD, 4));
        assertEquals(failed, evidence.observe(world, dimension, root, Map.of(SWORD, 1, FOOD, 3)));
        assertEquals(failed, evidence.observe(world, dimension, root, Map.of(FOOD, 4)));
        assertEquals(failed, evidence.observe(world, dimension, root, Map.of(FOOD, 2)));
        String restored = evidence.observe(world, dimension, root, Map.of(SWORD, 1, FOOD, 4));
        assertNotEquals(failed, restored);
        assertEquals(restored, evidence.observe(world, dimension, root, Map.of(SWORD, 5, FOOD, 64)));
    }

    @Test void freshPlanRemovingTheOldRequiredItemCannotEraseItsFailedReservation() {
        AutoDepositWorkingFailureEvidence evidence = new AutoDepositWorkingFailureEvidence();
        Object world = new Object(), dimension = new Object(), root = new Object();
        String failed = evidence.capture(world, dimension, root, Map.of(SWORD, 1));
        AutoDepositConditions original = conditions(Map.of(SWORD, 1), failed);
        AutoDepositConditions clippedPlan = conditions(Map.of(), evidence.observe(world, dimension, root, Map.of()));
        assertEquals(original.conditionKey(REASON), clippedPlan.conditionKey(REASON));
        AutoDepositConditions restoredButProtected = conditions(Map.of(SWORD, 1),
                evidence.observe(world, dimension, root, Map.of(SWORD, 1)));
        assertNotEquals(original.conditionKey(REASON), restoredButProtected.conditionKey(REASON));
    }

    @Test void differentWorldDimensionOrUserRootCannotSupplyRestorationEvidence() {
        AutoDepositWorkingFailureEvidence evidence = new AutoDepositWorkingFailureEvidence();
        Object world = new Object(), dimension = new Object(), root = new Object();
        evidence.capture(world, dimension, root, Map.of(SWORD, 1));
        Map<String, Integer> held = Map.of(SWORD, 1);
        assertNull(evidence.observe(new Object(), dimension, root, held));
        assertNull(evidence.observe(world, new Object(), root, held));
        assertNull(evidence.observe(world, dimension, new Object(), held));
        assertNull(evidence.observe(world, dimension, root, null));
    }

    @Test void restorationIsBoundedAndRepeatedConsumptionCannotRechargeIt() {
        AutoDepositWorkingFailureEvidence evidence = new AutoDepositWorkingFailureEvidence();
        Object world = new Object(), dimension = new Object(), root = new Object();
        String failed = evidence.capture(world, dimension, root, Map.of(FOOD, 4));
        AutoDepositRearmPolicy policy = new AutoDepositRearmPolicy();
        DepositAllInventoryPressureSnapshot pressure = new DepositAllInventoryPressureSnapshot(36, 36);
        policy.beginUnit(pressure, "initial_plan", "automatic");
        policy.finishUnit(false, pressure, REASON, "automatic", failed);
        String restored = evidence.observe(world, dimension, root, Map.of(FOOD, 4));
        assertEquals(AutoDepositRearmDecision.RELATED_CHANGE, policy.observe(pressure, restored, "automatic"));
        policy.beginUnit(pressure, restored, "automatic");
        policy.finishUnit(false, pressure, REASON, "automatic", evidence.capture(world, dimension, root, Map.of(FOOD, 4)));
        assertEquals(failed, evidence.observe(world, dimension, root, Map.of(FOOD, 3)));
        assertEquals(AutoDepositRearmDecision.SAME_FAILURE,
                policy.observe(pressure, evidence.observe(world, dimension, root, Map.of(FOOD, 4)), "automatic"));
        assertEquals(1, policy.activeBudget().recoveryGrants());
    }

    @Test void workingRestorationDoesNotChangeOtherFailureFamilies() {
        AutoDepositConditions failed = conditions(Map.of(), "original-reservation:deficit");
        AutoDepositConditions restored = conditions(Map.of(), "original-reservation:restored");
        for (String reason : List.of("no_safe_surplus", "no_slot_relief", "NORMAL", "GENERAL_CHILD_UNCONFIRMED",
                "TRUSTED_CHILD_FAILED", "BUDGET_EXHAUSTED")) {
            assertEquals(failed.conditionKey(reason), restored.conditionKey(reason), reason);
        }
        assertNull(conditions(Map.of(), null).conditionKey(REASON));
    }

    @Test void captureDoesNotExposeContextIdentitiesAndRejectsUnboundedReservations() {
        AutoDepositWorkingFailureEvidence a = new AutoDepositWorkingFailureEvidence();
        AutoDepositWorkingFailureEvidence b = new AutoDepositWorkingFailureEvidence();
        Map<String, Integer> reserved = Map.of(SWORD, 1);
        assertEquals(a.capture(new Object(), new Object(), new Object(), reserved),
                b.capture(new Object(), new Object(), new Object(), reserved));
        Map<String, Integer> tooMany = new TreeMap<>();
        for (int index = 0; index <= 64; index++) tooMany.put("item:" + index, 1);
        assertNull(a.capture(new Object(), new Object(), new Object(), tooMany));
    }

    private static AutoDepositConditions conditions(Map<String, Integer> currentRequired, String workingFailure) {
        return new AutoDepositConditions("automatic", Map.of(), currentRequired, Map.of(),
                List.of("general:A:empty=3"), "", workingFailure);
    }
}
