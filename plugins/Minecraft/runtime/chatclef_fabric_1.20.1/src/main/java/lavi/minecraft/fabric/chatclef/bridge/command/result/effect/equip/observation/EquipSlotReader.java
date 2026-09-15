package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation;

import adris.altoclef.AltoClef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import java.util.ArrayList;
import java.util.function.Supplier;

//20260915_kpopmodder: Read only five authoritative client equipment slots on the Minecraft thread.
public final class EquipSlotReader implements Supplier<EquipSlotObservation> {
    @Override public EquipSlotObservation get() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || !client.isOnThread()) return unavailable("minecraft_client_thread_required");
            if (client.world == null || client.player == null) return unavailable("client_world_or_player_unavailable");
            AltoClef mod = AltoClef.getInstance();
            if (mod == null || mod.getWorld() != client.world || mod.getPlayer() != client.player)
                return unavailable("altoclef_client_binding_mismatch");
            var slots = new ArrayList<EquipSlotValue>();
            for (String slot : EquipSlotObservation.SLOT_ORDER) {
                ItemStack stack = client.player.getEquippedStack(EquipmentSlot.byName(slot));
                String id = stack.isEmpty() ? "minecraft:air" : Registries.ITEM.getId(stack.getItem()).toString();
                if (id.length() > 256) return unavailable("slot_item_id_limit_exceeded");
                slots.add(new EquipSlotValue(slot, id, stack.isEmpty() ? 0 : stack.getCount()));
            }
            return new EquipSlotObservation(true, "available", System.currentTimeMillis(), client.world, client.player, slots);
        } catch (RuntimeException | LinkageError error) {
            return unavailable("slot_read_failed");
        }
    }
    private static EquipSlotObservation unavailable(String reason) { return EquipSlotObservation.unavailable(reason); }
}
