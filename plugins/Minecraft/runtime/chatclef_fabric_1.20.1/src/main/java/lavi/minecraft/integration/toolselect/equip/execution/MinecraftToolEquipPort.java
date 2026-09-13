//#if MC == 12001
package lavi.minecraft.integration.toolselect.equip.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.integration.toolselect.equip.model.*;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

//20260913_kpopmodder: Validate actual handler/backing-inventory slots before the existing force-click boundary.
public final class MinecraftToolEquipPort implements ToolEquipPort {
    private final AltoClef mod;
    private final Predicate<ItemStack> usable;
    private final IntPredicate destinationAllowed;
    public MinecraftToolEquipPort(AltoClef mod, Predicate<ItemStack> usable, IntPredicate destinationAllowed) {
        this.mod = java.util.Objects.requireNonNull(mod);
        this.usable = java.util.Objects.requireNonNull(usable);
        this.destinationAllowed = java.util.Objects.requireNonNull(destinationAllowed);
    }
    @Override public ToolEquipFrame read(int source, int destination) {
        var player = mod.getPlayer();
        ScreenHandler handler = player == null ? null : player.currentScreenHandler;
        ToolEquipBinding binding = new ToolEquipBinding(mod.getWorld(), player,
                mod.getUserTaskChain().getCurrentTask(), mod.getUserTaskChain().currentRootInvocation(),
                handler, handler == null ? -1 : handler.syncId);
        int sourceWindow = window(handler, source), destinationWindow = window(handler, destination);
        boolean mapped = player != null && source >= 0 && source < 36 && destination >= 0 && destination < 9
                && sourceWindow >= 0 && destinationWindow >= 0;
        ItemStack from = mapped ? player.getInventory().getStack(source) : ItemStack.EMPTY;
        ItemStack to = mapped ? player.getInventory().getStack(destination) : ItemStack.EMPTY;
        boolean exchange = false;
        if (mapped) {
            var sourceSlot = handler.getSlot(sourceWindow);
            var destinationSlot = handler.getSlot(destinationWindow);
            exchange = sourceSlot.canTakeItems(player) && destinationSlot.canTakeItems(player)
                    && sourceSlot.canInsert(to) && destinationSlot.canInsert(from)
                    && to.getCount() <= sourceSlot.getMaxItemCount(to)
                    && from.getCount() <= destinationSlot.getMaxItemCount(from);
        }
        return new ToolEquipFrame(binding, value(from), value(to), sourceWindow, destinationWindow,
                player == null ? -1 : player.getInventory().selectedSlot, mapped,
                handler != null && handler.getCursorStack().isEmpty(), exchange,
                inputAvailable(), mapped && mod.getWorld() != null && usable.test(from),
                mapped && mod.getWorld() != null && destinationAllowed.test(destination));
    }
    public boolean inputAvailable() {
        return mod.getPlayer() != null && mod.getWorld() != null
                && net.minecraft.client.MinecraftClient.getInstance().isOnThread()
                && mod.getTaskRunner().getCurrentTaskChain() == mod.getUserTaskChain()
                && !mod.getFoodChain().isTryingToEat() && !mod.getPlayer().isUsingItem()
                && !(mod.getModSettings().isMobDefense()
                    && mod.getWorld().getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL
                    && mod.getMobDefenseChain().isToolInputClaimed());
    }
    private int window(ScreenHandler handler, int inventoryIndex) {
        if (handler == null || mod.getPlayer() == null || inventoryIndex < 0 || inventoryIndex >= 36) return -1;
        for (int i = 0; i < handler.slots.size(); i++) {
            var slot = handler.getSlot(i);
            if (slot.inventory == mod.getPlayer().getInventory() && slot.getIndex() == inventoryIndex) return i;
        }
        return -1;
    }
    @Override public void swap(int sourceWindow, int destinationHotbar) {
        mod.getSlotHandler().forceSwapPlayerSlot(Slot.getFromCurrentScreen(sourceWindow), destinationHotbar);
    }
    @Override public void selectHotbar(int hotbar) { mod.getPlayer().getInventory().selectedSlot = hotbar; }
    public static ToolStackValue value(ItemStack stack) {
        return new ToolStackValue(stack.getItem(), stack.getCount(), stack.getDamage(),
                stack.getNbt() == null ? null : stack.getNbt().copy());
    }
}
//#endif
