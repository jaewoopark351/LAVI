package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation;

//20260915_kpopmodder: Share a frozen slot value without exposing a live ItemStack.
public record EquipSlotValue(String slot, String itemId, int count) { }
