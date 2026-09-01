package lavi.minecraft.diagnostics.crafting.acquisition.scope;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Define the bounded incident-scope registry contract before implementation.
class IronPickaxeAcquisitionScopeRegistryTest {
    @Test
    void onlyBoundaryEnabledIronPickaxeActivationCreatesAnActiveLedger() {
        IronPickaxeAcquisitionScopeRegistry registry = new IronPickaxeAcquisitionScopeRegistry();
        IronPickaxeAcquisitionScopeKey ironKey = key(1);

        IronPickaxeAcquisitionScopeDecision nonIron = registry.activate(
                true,
                "minecraft:diamond_pickaxe",
                ironKey,
                10L,
                1_000L
        );

        assertFalse(nonIron.activated());
        assertFalse(nonIron.reason().isBlank());
        assertEquals(0, registry.snapshot().activeCount());

        IronPickaxeAcquisitionScopeDecision iron = registry.activate(
                true,
                "minecraft:iron_pickaxe",
                ironKey,
                11L,
                2_000L
        );

        assertTrue(iron.activated());
        assertFalse(iron.reason().isBlank());
        assertEquals(1, registry.snapshot().activeCount());
        assertTrue(registry.snapshot().activeKeys().contains(ironKey));
    }

    @Test
    void offModeCreatesNoStateAndClearRemovesActiveAndRetiredState() {
        IronPickaxeAcquisitionScopeRegistry registry = new IronPickaxeAcquisitionScopeRegistry();
        IronPickaxeAcquisitionScopeKey active = key(1);
        IronPickaxeAcquisitionScopeKey retired = key(2);
        assertTrue(activate(registry, active, 1L).activated());
        assertTrue(activate(registry, retired, 2L).activated());
        assertTrue(registry.retire(retired, 3L, 3_000L));
        assertEquals(1, registry.snapshot().activeCount());
        assertEquals(1, registry.snapshot().tombstoneCount());

        registry.clearForModeOff();

        assertEquals(0, registry.snapshot().activeCount());
        assertEquals(0, registry.snapshot().tombstoneCount());
        IronPickaxeAcquisitionScopeDecision disabled = registry.activate(
                false,
                "minecraft:iron_pickaxe",
                key(3),
                4L,
                4_000L
        );
        assertFalse(disabled.activated());
        assertFalse(disabled.reason().isBlank());
        assertEquals(0, registry.snapshot().activeCount());
        assertEquals(0, registry.snapshot().tombstoneCount());
    }

    @Test
    void ninthActiveLedgerIsRefusedWithoutEvictingAnyExistingLedger() {
        IronPickaxeAcquisitionScopeRegistry registry = new IronPickaxeAcquisitionScopeRegistry();
        List<IronPickaxeAcquisitionScopeKey> admitted = new ArrayList<>();
        for (int index = 1; index <= 8; index++) {
            IronPickaxeAcquisitionScopeKey key = key(index);
            admitted.add(key);
            assertTrue(activate(registry, key, index).activated());
        }

        IronPickaxeAcquisitionScopeKey refusedKey = key(9);
        IronPickaxeAcquisitionScopeDecision refused = activate(registry, refusedKey, 9L);

        assertFalse(refused.activated());
        assertFalse(refused.reason().isBlank());
        assertEquals(8, registry.snapshot().activeCount());
        assertEquals(1L, registry.snapshot().activationRefusalCount());
        assertFalse(registry.snapshot().activeKeys().contains(refusedKey));
        assertTrue(registry.snapshot().activeKeys().containsAll(admitted));
    }

    @Test
    void ninthTombstoneReplacesOnlyTheOldestRetirementSequence() {
        IronPickaxeAcquisitionScopeRegistry registry = new IronPickaxeAcquisitionScopeRegistry();
        List<IronPickaxeAcquisitionScopeKey> keys = new ArrayList<>();
        for (int index = 1; index <= 9; index++) {
            IronPickaxeAcquisitionScopeKey key = key(index);
            keys.add(key);
            assertTrue(activate(registry, key, index).activated());
            assertTrue(registry.retire(key, 100L + index, 1_000L + index));
        }

        assertEquals(0, registry.snapshot().activeCount());
        assertEquals(8, registry.snapshot().tombstoneCount());
        assertFalse(registry.snapshot().tombstoneKeys().contains(keys.get(0)));
        assertTrue(registry.snapshot().tombstoneKeys().containsAll(keys.subList(1, 9)));
        assertEquals(1L, registry.snapshot().replacedTombstoneCount());
    }

    @Test
    void lateAndExpiredEventsNeverReopenARetiredLedger() {
        IronPickaxeAcquisitionScopeRegistry registry = new IronPickaxeAcquisitionScopeRegistry();
        IronPickaxeAcquisitionScopeKey key = key(1);
        assertTrue(activate(registry, key, 10L).activated());
        assertTrue(registry.retire(key, 20L, 20_000L));

        assertEquals(
                IronPickaxeAcquisitionObservationDisposition.LATE_TOMBSTONE,
                registry.observe(key, 21L, 21_000L)
        );
        assertEquals(1L, registry.snapshot().lateEventCount());
        assertEquals(0, registry.snapshot().activeCount());
        IronPickaxeAcquisitionScopeDecision refused = activate(registry, key, 22L);
        assertFalse(refused.activated());
        assertEquals("LATE_TOMBSTONE_CANNOT_REOPEN", refused.reason());
        assertEquals(0, registry.snapshot().activeCount());

        assertEquals(
                IronPickaxeAcquisitionObservationDisposition.UNKNOWN_RETIRED,
                registry.observe(key, 220L, 220_000L)
        );
        assertEquals(0, registry.snapshot().activeCount());
        assertFalse(registry.snapshot().activeKeys().contains(key));
    }

    @Test
    void blankOrOversizeOpaqueIdentityFailsClosedWithoutTruncation() {
        IronPickaxeAcquisitionScopeRegistry registry = new IronPickaxeAcquisitionScopeRegistry();
        IronPickaxeAcquisitionScopeKey blank = new IronPickaxeAcquisitionScopeKey(
                "session",
                1L,
                "",
                "correlation",
                "user-root-1",
                1L,
                "root-instance"
        );
        String oversizeCorrelation = "한".repeat(121);
        assertTrue(oversizeCorrelation.getBytes(StandardCharsets.UTF_8).length > 360);
        IronPickaxeAcquisitionScopeKey oversize = new IronPickaxeAcquisitionScopeKey(
                "session",
                1L,
                "request",
                oversizeCorrelation,
                "user-root-1",
                1L,
                "root-instance"
        );

        IronPickaxeAcquisitionScopeDecision blankDecision = activate(registry, blank, 1L);
        IronPickaxeAcquisitionScopeDecision oversizeDecision = activate(registry, oversize, 2L);

        assertFalse(blankDecision.activated());
        assertEquals("UNAVAILABLE_BLANK_ID", blankDecision.reason());
        assertFalse(oversizeDecision.activated());
        assertEquals("UNAVAILABLE_OVERSIZE", oversizeDecision.reason());
        assertEquals(0, registry.snapshot().activeCount());
    }

    private static IronPickaxeAcquisitionScopeDecision activate(
            IronPickaxeAcquisitionScopeRegistry registry,
            IronPickaxeAcquisitionScopeKey key,
            long tick) {
        return registry.activate(
                true,
                "minecraft:iron_pickaxe",
                key,
                tick,
                tick * 1_000L
        );
    }

    private static IronPickaxeAcquisitionScopeKey key(int index) {
        return new IronPickaxeAcquisitionScopeKey(
                "session-" + index,
                1L,
                "request-" + index,
                "correlation-" + index,
                "user-root-" + index,
                index,
                "root-instance-" + index
        );
    }
}
