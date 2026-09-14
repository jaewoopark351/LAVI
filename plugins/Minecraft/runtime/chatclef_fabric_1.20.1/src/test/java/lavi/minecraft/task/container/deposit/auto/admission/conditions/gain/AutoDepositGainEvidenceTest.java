package lavi.minecraft.task.container.deposit.auto.admission.conditions.gain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Real gains may reopen a safe-surplus evaluation; survival churn may not.
class AutoDepositGainEvidenceTest {
    private static final String FOOD = "minecraft:bread";
    private static final String SWORD = "minecraft:diamond_sword";

    @Test void eatingAndReplenishingPreviouslyObservedFoodDoesNotChangeEvidence() {
        AutoDepositGainEvidence evidence = new AutoDepositGainEvidence();
        Object world = new Object();
        String initial = evidence.observe(world, 1, Map.of(FOOD, 10), Set.of(FOOD));
        assertEquals(initial, evidence.observe(world, 1, Map.of(FOOD, 9), Set.of(FOOD)));
        assertEquals(initial, evidence.observe(world, 1, Map.of(FOOD, 10), Set.of(FOOD)));
        String gained = evidence.observe(world, 1, Map.of(FOOD, 15), Set.of(FOOD));
        assertNotEquals(initial, gained);
        assertTrue(gained.contains("10>15"));
        for (int repeat = 0; repeat < 20; repeat++) {
            assertEquals(gained, evidence.observe(world, 1, Map.of(FOOD, 14), Set.of(FOOD)));
            assertEquals(gained, evidence.observe(world, 1, Map.of(FOOD, 15), Set.of(FOOD)));
        }
    }

    @Test void duplicateWeaponSelectionCannotPublishAGainWithoutAnIncreasedTotal() {
        AutoDepositGainEvidence evidence = new AutoDepositGainEvidence();
        Object world = new Object();
        String initial = evidence.observe(world, 1, Map.of(SWORD, 2), Set.of());
        // The caller aggregates main, armor and offhand, so equip/slot changes retain this total.
        assertEquals(initial, evidence.observe(world, 1, Map.of(SWORD, 2), Set.of(SWORD)));
        assertEquals(initial, evidence.observe(world, 1, Map.of(SWORD, 2), Set.of()));
        String gained = evidence.observe(world, 1, Map.of(SWORD, 3), Set.of(SWORD));
        assertNotEquals(initial, gained);
        assertEquals(gained, evidence.observe(world, 1, Map.of(SWORD, 3), Set.of()));
        assertEquals(gained, evidence.observe(world, 1, Map.of(SWORD, 3), Set.of(SWORD)));
    }

    @Test void anObservedGainRequiresCurrentSafeSurplusBeforeItCanBePublished() {
        AutoDepositGainEvidence evidence = new AutoDepositGainEvidence();
        Object world = new Object();
        String initial = evidence.observe(world, 1, Map.of(SWORD, 1), Set.of());
        assertEquals(initial, evidence.observe(world, 1, Map.of(SWORD, 2), Set.of()));
        String gained = evidence.observe(world, 1, Map.of(SWORD, 2), Set.of(SWORD));
        assertNotEquals(initial, gained, "A fresh safe plan may validate an already observed real gain");
        assertEquals(gained, evidence.observe(world, 1, Map.of(SWORD, 2), Set.of(SWORD)));
    }

    @Test void consumedPendingGainCannotBePublishedByALaterPlanChange() {
        AutoDepositGainEvidence evidence = new AutoDepositGainEvidence();
        Object world = new Object();
        String initial = evidence.observe(world, 1, Map.of(FOOD, 10), Set.of());
        assertEquals(initial, evidence.observe(world, 1, Map.of(FOOD, 12), Set.of()));
        assertEquals(initial, evidence.observe(world, 1, Map.of(FOOD, 11), Set.of()));
        assertEquals(initial, evidence.observe(world, 1, Map.of(FOOD, 12), Set.of(FOOD)));
    }

    @Test void episodeResetDoesNotChangePublishedConditionButAllowsLaterNewGains() {
        AutoDepositGainEvidence evidence = new AutoDepositGainEvidence();
        Object world = new Object();
        evidence.observe(world, 1, Map.of(FOOD, 10), Set.of());
        String gained = evidence.observe(world, 1, Map.of(FOOD, 20), Set.of(FOOD));
        assertEquals(gained, evidence.observe(world, 2, Map.of(FOOD, 5), Set.of()));
        assertEquals(gained, evidence.observe(world, 2, Map.of(FOOD, 5), Set.of(FOOD)));
        assertNotEquals(gained, evidence.observe(world, 2, Map.of(FOOD, 8), Set.of(FOOD)));
        assertEquals("", evidence.observe(new Object(), 2, Map.of(FOOD, 8), Set.of(FOOD)));
    }

    @Test void itemChurnIsBoundedAndCannotEvictOldCountsToFabricateMoreGains() {
        AutoDepositGainEvidence evidence = new AutoDepositGainEvidence();
        Object world = new Object();
        Map<String, Integer> initial = new TreeMap<>();
        for (int item = 0; item < AutoDepositGainEvidence.MAX_TRACKED_ITEMS; item++) {
            initial.put("equipment:" + item, 1);
        }
        evidence.observe(world, 1, initial, initial.keySet());
        String gained = evidence.observe(world, 1, Map.of("equipment:0", 2), Set.of("equipment:0"));
        assertNotEquals("", gained);
        assertEquals(gained, evidence.observe(world, 1, Map.of("equipment:new", 2), Set.of("equipment:new")));
        assertEquals(gained, evidence.observe(world, 1, Map.of("equipment:0", 9999), Set.of("equipment:0")));
    }
}
