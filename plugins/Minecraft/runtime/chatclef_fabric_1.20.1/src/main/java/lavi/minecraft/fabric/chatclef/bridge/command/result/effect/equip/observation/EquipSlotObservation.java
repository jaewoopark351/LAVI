package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation;

import java.util.List;

//20260915_kpopmodder: Immutable client-slot values; live identity references never cross the wire.
public record EquipSlotObservation(boolean available, String reason, long observedAtMs,
                                   Object world, Object player, List<EquipSlotValue> slots) {
    public static final List<String> SLOT_ORDER = List.of("head", "chest", "legs", "feet", "offhand");
    public EquipSlotObservation { slots = List.copyOf(slots); }
    public static EquipSlotObservation unavailable(String reason) {
        return new EquipSlotObservation(false, reason, System.currentTimeMillis(), null, null, List.of());
    }
}
