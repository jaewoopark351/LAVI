package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Fix the closed single-target GetCommand effect grammar.
class FabricChatClefGetItemEffectProfileTest {
    @Test
    void anySingleCatalogueTargetAndPositiveQuantityUsesAcquireDelta() {
        FabricChatClefGetItemEffectProfile profile = profile(
                "  get   iron_pickaxe   7  "
        );

        assertTrue(profile.tracked());
        assertEquals("fabric_chatclef_get_acquire_delta", profile.effectProfileId());
        assertEquals(1, profile.effectProfileVersion());
        assertEquals("get_acquisition_delta", profile.effectKind());
        assertEquals("ACQUIRE_DELTA", profile.quantitySemantics());
        assertEquals("iron_pickaxe", profile.targetItem());
        assertEquals(7, profile.requestedCount());
        assertEquals(List.of("minecraft:iron_pickaxe"), profile.targetMatchIds());
        assertFalse(profile.legacyFlatCompatible());
    }

    @Test
    void omittedQuantityDefaultsToOneAndGroupTargetCapturesSortedMatchSet() {
        FabricChatClefGetItemEffectProfile profile = profile("get log");
        List<String> sorted = new ArrayList<>(profile.targetMatchIds());
        sorted.sort(Comparator.naturalOrder());

        assertTrue(profile.tracked());
        assertEquals("log", profile.targetItem());
        assertEquals(1, profile.requestedCount());
        assertEquals(
                List.of("minecraft:oak_log", "minecraft:spruce_log"),
                profile.targetMatchIds()
        );
        assertEquals(sorted, profile.targetMatchIds());
        assertTrue(profile.targetMatchIds().contains("minecraft:oak_log"));
    }

    @Test
    void exactLegacyDiamondShapeKeepsTransitionalFlatCompatibility() {
        FabricChatClefGetItemEffectProfile profile = profile(
                "  get   diamond_pickaxe   1  "
        );

        assertTrue(profile.tracked());
        assertTrue(profile.legacyFlatCompatible());
        assertEquals("diamond_pickaxe", profile.targetItem());
        assertEquals(1, profile.requestedCount());
    }

    @Test
    void atPrefixedNormalizedOakLogCommandIsTracked() {
        FabricChatClefGetItemEffectProfile profile = profile("@get oak_log 2");

        assertTrue(profile.tracked());
        assertEquals("oak_log", profile.targetItem());
        assertEquals(2, profile.requestedCount());
        assertEquals(List.of("minecraft:oak_log"), profile.targetMatchIds());
        assertFalse(profile.legacyFlatCompatible());
    }

    @Test
    void listUnknownNonpositiveOverflowAndOtherCommandShapesAreExcluded() {
        assertFalse(profile("get [diamond_pickaxe 1,iron_pickaxe 1]").tracked());
        assertFalse(profile("get not_a_catalogue_target 1").tracked());
        assertFalse(profile("get diamond_pickaxe 0").tracked());
        assertFalse(profile("get diamond_pickaxe -1").tracked());
        assertFalse(profile("get diamond_pickaxe 2147483648").tracked());
        assertTrue(profile("@get diamond_pickaxe 1").tracked());
        assertFalse(profile("@@get diamond_pickaxe 1").tracked());
        assertFalse(profile("get diamond_pickaxe 1;get diamond 1").tracked());
        assertFalse(profile("deposit diamond_pickaxe 1").tracked());
        assertFalse(profile(null).tracked());
    }

    @Test
    void controlAndNonAsciiWhitespaceCannotEnterTheClosedGrammar() {
        assertFalse(profile("get\tdiamond_pickaxe 1").tracked());
        assertFalse(profile("get diamond_pickaxe\n1").tracked());
        assertFalse(profile("get diamond_pickaxe 1\r").tracked());
        assertFalse(profile("get\u00a0diamond_pickaxe 1").tracked());
    }

    private static FabricChatClefGetItemEffectProfile profile(String command) {
        return FabricChatClefGetItemTestProfiles.profile(command);
    }
}
