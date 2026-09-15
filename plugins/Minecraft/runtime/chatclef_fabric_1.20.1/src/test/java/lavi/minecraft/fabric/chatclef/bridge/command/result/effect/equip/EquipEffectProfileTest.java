package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip;

import adris.altoclef.util.ItemTarget;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipEffectProfile;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EquipEffectProfileTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.createGameVersion(); Bootstrap.initialize();
        try (var client = lavi.minecraft.testsupport.HeadlessMinecraftClientSession.inGame()) { adris.altoclef.TaskCatalogue.resourceNames(); }
    }
    @Test void allFiveNativeSetsFreezeFourDistinctEquipmentSlots() {
        for (String set : List.of("leather", "iron", "gold", "diamond", "netherite")) {
            var profile = EquipEffectProfile.capture("@equip " + set);
            assertEquals("available", profile.reason(), set);
            assertEquals("equip " + set, profile.command());
            assertEquals(4, profile.targets().size());
            assertEquals(4, profile.targets().stream().map(t -> t.matches().get(0).slot()).distinct().count());
            assertTrue(profile.targets().stream().allMatch(t -> t.requestedCount() == 1 && t.matches().size() == 1));
        }
    }
    @Test void nativeListParserPreservesQuantityAndAggregatesRepeatedTokens() {
        var profile = EquipEffectProfile.capture("equip [diamond_helmet 2,diamond_chestplate,diamond_helmet 3]");
        assertEquals("available", profile.reason());
        assertEquals(2, profile.targets().size());
        assertEquals(5, profile.targets().stream().filter(t -> t.nativeTarget().equals("diamond_helmet")).findFirst().orElseThrow().requestedCount());
    }
    @Test void freezesEveryAlternativeWithoutMutatingNativeTargetArray() {
        Item[] candidates = {Items.IRON_HELMET, Items.DIAMOND_HELMET};
        var profile = EquipEffectProfile.fromTargets("equip alternative", new ItemTarget[]{new ItemTarget(candidates, 7)});
        candidates[0] = Items.DIRT;
        assertEquals(2, profile.targets().get(0).matches().size());
        assertEquals(7, profile.targets().get(0).requestedCount());
        assertTrue(profile.targets().get(0).matches().stream().noneMatch(m -> m.itemId().endsWith("dirt")));
        assertThrows(UnsupportedOperationException.class, () -> profile.targets().clear());
    }
    @Test void vanillaShieldUsesOffhandButUnsupportedEquipmentIsNotInvented() {
        var shield = EquipEffectProfile.capture("equip shield");
        assertEquals("available", shield.reason());
        assertEquals("offhand", shield.targets().get(0).matches().get(0).slot());
        for (Item item : List.of(Items.ELYTRA, Items.CARVED_PUMPKIN, Items.PLAYER_HEAD, Items.DIAMOND_SWORD))
            assertEquals("unsupported_native_target", EquipEffectProfile.fromTargets("equip unsupported", new ItemTarget[]{new ItemTarget(item)}).reason());
    }
    @Test void unsafeNativeShieldAlternativeCastRemainsUnverified() {
        var profile = EquipEffectProfile.fromTargets("equip mixed", new ItemTarget[]{new ItemTarget(new Item[]{Items.IRON_HELMET, Items.SHIELD}, 1)});
        assertEquals("unsupported_native_target", profile.reason());
    }
    @Test void malformedQuantityAndExcessiveProfileDegradeWithoutChangingNativeExecution() {
        assertNotEquals("available", EquipEffectProfile.capture("equip diamond_helmet -1").reason());
        assertNotEquals("available", EquipEffectProfile.capture("equip no_such_equipment").reason());
        assertNotEquals("available", EquipEffectProfile.capture("equip diamond_helmet 2147483648").reason());
        assertEquals("profile_limit_exceeded", EquipEffectProfile.fromTargets("equip many", new ItemTarget[33]).reason());
        assertFalse(EquipEffectProfile.capture("get diamond_helmet").tracked());
    }
}
