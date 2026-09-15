package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip;

import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.evidence.EquipEffectEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipEffectProfile;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipTarget;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipTargetMatch;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotValue;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EquipEffectEvidenceTest {
    static final Object WORLD = new Object(), PLAYER = new Object();
    static EquipEffectProfile profile() {
        return new EquipEffectProfile("equip [helmet,shield]", true, "available", List.of(
                new EquipTarget(0, "helmet", 2, List.of(new EquipTargetMatch("minecraft:diamond_helmet", "head"), new EquipTargetMatch("minecraft:iron_helmet", "head"))),
                new EquipTarget(1, "shield", 1, List.of(new EquipTargetMatch("minecraft:shield", "offhand")))));
    }
    static EquipSlotObservation slots(long time, Map<String, String> worn) {
        return new EquipSlotObservation(true, "available", time, WORLD, PLAYER,
                EquipSlotObservation.SLOT_ORDER.stream().map(slot -> new EquipSlotValue(slot,
                        worn.getOrDefault(slot, "minecraft:air"), worn.containsKey(slot) ? 1 : 0)).toList());
    }
    @Test void allTargetsAndAnyAlternativeAreRequiredAndCountsAreNotWornQuantities() {
        var after = slots(11, Map.of("head", "minecraft:iron_helmet", "offhand", "minecraft:shield"));
        var evidence = EquipEffectEvidence.evaluate(profile(), slots(10, Map.of()), after, "valid");
        assertEquals("satisfied", evidence.status());
        assertEquals(List.of(true, true), evidence.afterSatisfied());
    }
    @Test void existingIdenticalEquipmentIsReportedAsAlreadySatisfied() {
        var state = slots(10, Map.of("head", "minecraft:iron_helmet", "offhand", "minecraft:shield"));
        assertEquals("already_satisfied", EquipEffectEvidence.evaluate(profile(), state, state, "valid").status());
    }
    @Test void partialAndWrongSlotAreNotWholeSuccess() {
        assertEquals("partial", EquipEffectEvidence.evaluate(profile(), slots(10, Map.of()),
                slots(11, Map.of("head", "minecraft:diamond_helmet")), "valid").status());
        assertEquals("not_satisfied", EquipEffectEvidence.evaluate(profile(), slots(10, Map.of()),
                slots(11, Map.of("chest", "minecraft:diamond_helmet", "head", "minecraft:shield")), "valid").status());
    }
    @Test void conflictingSlotTargetsCannotBothBecomeSatisfied() {
        var profile = new EquipEffectProfile("equip conflict", true, "available", List.of(
                new EquipTarget(0, "diamond_helmet", 1, List.of(new EquipTargetMatch("minecraft:diamond_helmet", "head"))),
                new EquipTarget(1, "iron_helmet", 1, List.of(new EquipTargetMatch("minecraft:iron_helmet", "head")))));
        assertEquals("partial", EquipEffectEvidence.evaluate(profile, slots(10, Map.of()), slots(11, Map.of("head", "minecraft:iron_helmet")), "valid").status());
    }
    @Test void missingBeforeOrAfterIsUnverifiedEvenWhenAnotherObservationShowsEquipment() {
        var unavailable = EquipSlotObservation.unavailable("slot_read_failed");
        for (var pair : List.of(List.of(unavailable, slots(10, Map.of())), List.of(slots(10, Map.of()), unavailable))) {
            var result = EquipEffectEvidence.evaluate(profile(), pair.get(0), pair.get(1), "valid");
            assertEquals("unavailable", result.status());
            assertFalse(result.bindingValid());
        }
    }
    @Test void worldPlayerAndTimeReplacementCannotProveEffect() {
        var before = slots(10, Map.of());
        for (var after : List.of(new EquipSlotObservation(true, "available", 11, new Object(), PLAYER, before.slots()),
                new EquipSlotObservation(true, "available", 11, WORLD, new Object(), before.slots()), slots(9, Map.of()))) {
            assertEquals("unavailable", EquipEffectEvidence.evaluate(profile(), before, after, "valid").status());
        }
    }
    @Test void detachedContextOverridesEvenSatisfiedSlots() {
        var state = slots(10, Map.of("head", "minecraft:iron_helmet", "offhand", "minecraft:shield"));
        var result = EquipEffectEvidence.evaluate(profile(), state, state, "context_detached");
        assertEquals("unavailable", result.status());
        assertEquals("context_detached", result.reason());
    }
}
