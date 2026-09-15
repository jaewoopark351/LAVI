package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.binding.EquipEffectBinding;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.evidence.EquipEffectEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile.EquipEffectProfile;
import java.util.LinkedHashMap;
import java.util.Map;

//20260915_kpopmodder: Add bounded immutable EQUIP evidence to the existing v1 effect envelope.
public final class EquipEffectPayload {
    private EquipEffectPayload() { }
    public static FabricChatClefCommandResultDataPayload attach(Map<String, Object> base, EquipEffectProfile profile,
            EquipEffectBinding binding, EquipSlotObservation before, EquipSlotObservation after, EquipEffectEvidence evidence) {
        var effect = new LinkedHashMap<String, Object>();
        effect.put("command", profile.command());
        effect.put("request_id", binding.requestId());
        effect.put("session_id", binding.sessionId());
        effect.put("server_connection_generation", binding.serverGeneration());
        effect.put("java_socket_generation", binding.socketGeneration());
        effect.put("task_identity", EquipEffectBinding.taskIdentity(base));
        effect.put("observation_source", "minecraft_client_equipment_slots");
        effect.put("quantity_semantics", "all_targets_any_match_slot_presence");
        effect.put("targets", profile.targets().stream().map(target -> Map.of(
                "index", target.index(), "native_target", target.nativeTarget(), "requested_count", target.requestedCount(),
                "matches", target.matches().stream().map(match -> Map.of("item_id", match.itemId(), "slot", match.slot())).toList())).toList());
        effect.put("before", observation(before));
        effect.put("after", observation(after));
        effect.put("binding_valid", evidence.bindingValid());
        effect.put("effect_observation_status", evidence.status());
        effect.put("effect_observation_reason", evidence.reason());
        effect.put("before_satisfied", evidence.beforeSatisfied());
        effect.put("after_satisfied", evidence.afterSatisfied());
        var result = new LinkedHashMap<>(base);
        result.put("effect_kind", "equip_slots");
        result.put("effect_profile_id", "equip_armor_slots_v1");
        result.put("effect_profile_version", 1);
        result.put("effect_payload", Map.copyOf(effect));
        return FabricChatClefCommandResultDataPayload.fromMap(result);
    }
    public static Map<String, Object> observation(EquipSlotObservation value) {
        return Map.of("available", value.available(), "reason", value.reason(), "observed_at_ms", value.observedAtMs(),
                "slots", value.slots().stream().map(slot -> Map.of("slot", slot.slot(), "item_id", slot.itemId(), "count", slot.count())).toList());
    }
}
