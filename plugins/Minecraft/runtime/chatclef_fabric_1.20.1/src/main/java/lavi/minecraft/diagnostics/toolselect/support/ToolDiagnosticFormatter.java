package lavi.minecraft.diagnostics.toolselect.support;

import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;

import java.util.List;
import java.util.StringJoiner;

//20260804_kpopmodder: Share repeated tool diagnostic stack, slot, and speed formatting.
public final class ToolDiagnosticFormatter {
    public static final int MAX_CANDIDATES = 12;

    private ToolDiagnosticFormatter() {
    }

    public static String basicStackDetails(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.itemStackSummary(stack)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#stackString=" + ChatClefDiagnostics.safeValue(stack::toString);
    }

    public static String toolStackDetails(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.itemStackSummary(stack)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#isTool=" + ChatClefDiagnostics.safeValue(() -> stack.getItem() instanceof ToolItem)
                + "#isShears=" + ChatClefDiagnostics.safeValue(() -> stack.getItem() == Items.SHEARS)
                + "#stackString=" + ChatClefDiagnostics.safeValue(stack::toString);
    }

    public static String equipItemFingerprint(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.safeValue(() -> stack.getItem())
                + "#count=" + ChatClefDiagnostics.safeValue(stack::getCount)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#stackString=" + ChatClefDiagnostics.safeValue(stack::toString)
                + "#empty=" + ChatClefDiagnostics.safeValue(stack::isEmpty);
    }

    public static String bestToolStackFingerprint(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.safeValue(() -> stack.getItem())
                + "#count=" + ChatClefDiagnostics.safeValue(stack::getCount)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#empty=" + ChatClefDiagnostics.safeValue(stack::isEmpty);
    }

    public static ItemStack slotStack(Slot slot) {
        if (slot == null) {
            return null;
        }
        try {
            return StorageHelper.getItemStackInSlot(slot);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static String slotListSummary(List<Slot> slots) {
        if (slots == null) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        int emitted = 0;
        for (Slot slot : slots) {
            if (emitted >= MAX_CANDIDATES) {
                break;
            }
            joiner.add(ChatClefDiagnostics.slotSummary(slot));
            emitted++;
        }
        if (slots.size() > emitted) {
            joiner.add("truncated=" + (slots.size() - emitted));
        }
        return joiner.toString();
    }

    public static String speedValue(double speed) {
        if (Double.isNaN(speed)) {
            return "not_evaluated";
        }
        if (speed == Double.NEGATIVE_INFINITY) {
            return "none";
        }
        return Double.toString(speed);
    }
}
