package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.evidence;

import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipEffectProfile;
import java.util.List;

//20260915_kpopmodder: Evaluate ALL native targets / ANY candidate using slot presence, never target count as worn count.
public record EquipEffectEvidence(String status, String reason, boolean bindingValid,
                                  List<Boolean> beforeSatisfied, List<Boolean> afterSatisfied) {
    public EquipEffectEvidence { beforeSatisfied = List.copyOf(beforeSatisfied); afterSatisfied = List.copyOf(afterSatisfied); }
    public static EquipEffectEvidence evaluate(EquipEffectProfile profile, EquipSlotObservation before,
                                               EquipSlotObservation after, String bindingReason) {
        var beforeMet = satisfied(profile, before);
        var afterMet = satisfied(profile, after);
        boolean binding = "valid".equals(bindingReason);
        String unavailable = !binding ? bindingReason
                : !"available".equals(profile.reason()) ? profile.reason()
                : !before.available() || !after.available() ? "observation_unavailable"
                : before.world() == null || before.player() == null || before.world() != after.world()
                    || before.player() != after.player() ? "world_or_player_changed"
                : after.observedAtMs() < before.observedAtMs() ? "observation_time_invalid" : "";
        if (!unavailable.isEmpty()) return new EquipEffectEvidence("unavailable", unavailable, false, beforeMet, afterMet);
        if (afterMet.stream().allMatch(Boolean::booleanValue)) {
            boolean already = beforeMet.stream().allMatch(Boolean::booleanValue) && before.slots().equals(after.slots());
            return new EquipEffectEvidence(already ? "already_satisfied" : "satisfied",
                    already ? "already_equipped" : "all_targets_equipped", true, beforeMet, afterMet);
        }
        boolean partial = afterMet.stream().anyMatch(Boolean::booleanValue);
        return new EquipEffectEvidence(partial ? "partial" : "not_satisfied",
                partial ? "some_targets_not_equipped" : "no_targets_equipped", true, beforeMet, afterMet);
    }
    private static List<Boolean> satisfied(EquipEffectProfile profile, EquipSlotObservation observation) {
        if (!observation.available()) return List.of();
        return profile.targets().stream().map(target -> target.matches().stream().anyMatch(match ->
                observation.slots().stream().anyMatch(slot -> slot.slot().equals(match.slot())
                        && slot.itemId().equals(match.itemId()) && slot.count() > 0))).toList();
    }
}
