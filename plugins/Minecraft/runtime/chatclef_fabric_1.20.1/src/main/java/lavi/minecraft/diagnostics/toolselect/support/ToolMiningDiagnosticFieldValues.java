package lavi.minecraft.diagnostics.toolselect.support;

import adris.altoclef.util.MiningRequirement;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

//20260807_kpopmodder: Share tool mining diagnostic field values without changing tool selection behavior.
public final class ToolMiningDiagnosticFieldValues {
    private ToolMiningDiagnosticFieldValues() {
    }

    public static Object minimumMiningRequirement(Block block) {
        try {
            return block == null ? "unavailable" : MiningRequirement.getMinimumRequirementForBlock(block);
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    public static Object remainingDurability(ItemStack stack) {
        try {
            if (stack == null || stack.getMaxDamage() <= 0) {
                return "unavailable";
            }
            return stack.getMaxDamage() - stack.getDamage();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    public static Object damagePlus(ItemStack stack, int margin) {
        try {
            return stack == null ? "unavailable" : stack.getDamage() + margin;
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    public static Object durabilityThresholdReached(ItemStack stack, int margin) {
        try {
            return stack == null ? "unavailable" : stack.getDamage() + margin > stack.getMaxDamage();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    public static Object isIronPickaxe(ItemStack stack) {
        try {
            return stack != null && stack.getItem().equals(Items.IRON_PICKAXE);
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }
}
