package lavi.minecraft.diagnostics.toolselect.snapshot;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;

//20260913_kpopmodder: Read snapshots only inside the registered diagnostic owner's eligibility lease.
public final class ToolEquipSnapshotCapture {
    private ToolEquipSnapshotCapture() { }

    public static ToolEquipBeforeSnapshot before(AltoClef mod, Slot expectedSlot, ItemStack expectedStack) {
        int selected = selectedSlot(mod);
        Slot hotbar = Slot.getFromCurrentScreenInventory(1);
        ItemStack hotbarStack = stack(hotbar);
        ItemStack mainHand = stack(equipSlot());
        ItemStack expected = expectedStack == null ? stack(expectedSlot) : expectedStack.copy();
        return new ToolEquipBeforeSnapshot(selected, hotbar, hotbarStack, mainHand, expected);
    }

    public static Slot equipSlot() {
        try {
            return PlayerSlot.getEquipSlot();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static ItemStack stack(Slot slot) {
        try {
            return slot == null ? ItemStack.EMPTY : StorageHelper.getItemStackInSlot(slot).copy();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static int selectedSlot(AltoClef mod) {
        try {
            return mod.getPlayer().getInventory().selectedSlot;
        } catch (RuntimeException | LinkageError ignored) {
            return -1;
        }
    }
}
