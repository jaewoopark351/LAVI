package lavi.minecraft.task.container.deposit.auto.admission.conditions;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Check semantic comparison boundaries instead of identity/fingerprint churn.
class AutoDepositConditionsTest {
    @Test
    void inventoryAndCandidateOrderingDoesNotCreateAnotherOpportunity() {
        Map<String, Integer> firstOrder = new LinkedHashMap<>();
        firstOrder.put("minecraft:cobblestone", 64);
        firstOrder.put("minecraft:iron_ingot", 12);
        Map<String, Integer> secondOrder = new LinkedHashMap<>();
        secondOrder.put("minecraft:iron_ingot", 12);
        secondOrder.put("minecraft:cobblestone", 64);
        AutoDepositConditions a = conditions(firstOrder, Map.of(), Map.of(),
                List.of("general:A:empty=3", "general:B:empty=5"));
        AutoDepositConditions b = conditions(secondOrder, Map.of(), Map.of(),
                List.of("general:B:empty=5", "general:A:empty=3", "general:A:empty=3"));
        assertEquals(a.conditionKey("no_slot_relief"), b.conditionKey("no_slot_relief"));
        assertEquals(a.scope(), b.scope());
    }

    @Test
    void selectedHandProtectionAndTruncatedPlanChangesDoNotRechargeFailure() {
        Map<String, Integer> inventory = Map.of("minecraft:cobblestone", 128, "minecraft:dirt", 64);
        AutoDepositConditions a = conditions(inventory, Map.of(),
                Map.of("general:minecraft:cobblestone", 64, "protected:minecraft:dirt", 64), List.of());
        AutoDepositConditions b = conditions(inventory, Map.of(),
                Map.of("general:minecraft:dirt", 64, "protected:minecraft:cobblestone", 128), List.of());
        assertNotEquals(a.planKey(), b.planKey());
        assertEquals(a.conditionKey("no_safe_surplus"), b.conditionKey("no_safe_surplus"));
    }

    @Test
    void destinationFailureIgnoresUnrelatedInventoryAndWorkingChangesDoNotRechargeFailure() {
        AutoDepositConditions a = conditions(Map.of("minecraft:dirt", 64), Map.of(), Map.of(),
                List.of("general:A:empty=0"));
        AutoDepositConditions b = conditions(Map.of("minecraft:diamond", 10),
                Map.of("minecraft:cobblestone", 128), Map.of(), List.of("general:A:empty=0"));
        for (String reason : List.of("GENERAL_CHILD_UNCONFIRMED", "access_failed", "container_full",
                "BUDGET_EXHAUSTED", "NO_PROGRESS_LIMIT", "FOLLOWUP_LIMIT")) {
            assertEquals(a.conditionKey(reason), b.conditionKey(reason), reason);
        }
    }

    @Test
    void capacityAndNewDestinationAreRelevantButUncachedIsNotEmpty() {
        AutoDepositConditions uncached = conditions(Map.of(), Map.of(), Map.of(), List.of("general:A:empty=-1"));
        AutoDepositConditions full = conditions(Map.of(), Map.of(), Map.of(), List.of("general:A:empty=0"));
        AutoDepositConditions space = conditions(Map.of(), Map.of(), Map.of(), List.of("general:A:empty=4"));
        AutoDepositConditions other = conditions(Map.of(), Map.of(), Map.of(), List.of("general:B:empty=4"));
        assertNotEquals(uncached.conditionKey("container_full"), full.conditionKey("container_full"));
        assertNotEquals(full.conditionKey("container_full"), space.conditionKey("container_full"));
        assertNotEquals(space.conditionKey("container_full"), other.conditionKey("container_full"));
    }

    @Test
    void ordinaryChestChangesDoNotRecoverTrustedChildFailure() {
        AutoDepositConditions a = conditions(Map.of(), Map.of(), Map.of(),
                List.of("trusted:A:empty=0", "general:B:empty=0"));
        AutoDepositConditions b = conditions(Map.of(), Map.of(), Map.of(),
                List.of("trusted:A:empty=0", "general:B:empty=8"));
        assertEquals(a.conditionKey("TRUSTED_CHILD_FAILED"), b.conditionKey("TRUSTED_CHILD_FAILED"));
        assertNotEquals(a.conditionKey("GENERAL_CHILD_UNCONFIRMED"), b.conditionKey("GENERAL_CHILD_UNCONFIRMED"));
    }

    @Test
    void noSafeSurplusUsesActualInventoryAndRequirementsRatherThanPlanIdentity() {
        AutoDepositConditions a = conditions(Map.of("minecraft:dirt", 32), Map.of("minecraft:dirt", 32), Map.of(), List.of());
        AutoDepositConditions more = conditions(Map.of("minecraft:dirt", 64), Map.of("minecraft:dirt", 32), Map.of(), List.of());
        AutoDepositConditions released = conditions(Map.of("minecraft:dirt", 32), Map.of(), Map.of(), List.of());
        assertNotEquals(a.conditionKey("no_safe_surplus"), more.conditionKey("no_safe_surplus"));
        assertNotEquals(a.conditionKey("no_safe_surplus"), released.conditionKey("no_safe_surplus"));
        assertNotEquals(a.conditionKey("no_trusted_destination_or_safe_surplus"),
                more.conditionKey("no_trusted_destination_or_safe_surplus"));
    }

    @Test
    void unavailableNeverReturnsARecoverableZeroState() {
        AutoDepositConditions missing = AutoDepositConditions.unavailable("read_failed");
        assertFalse(missing.available());
        assertNull(missing.conditionKey("no_slot_relief"));
        assertNull(missing.planKey());
        assertEquals("read_failed", missing.readReason());
        assertThrows(IllegalArgumentException.class,
                () -> conditions(Map.of("minecraft:dirt", -1), Map.of(), Map.of(), List.of()));
    }

    private static AutoDepositConditions conditions(Map<String, Integer> inventory,
                                                      Map<String, Integer> working,
                                                      Map<String, Integer> plan,
                                                      List<String> destinations) {
        return new AutoDepositConditions("automatic", inventory, working, plan, destinations);
    }
}
