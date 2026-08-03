package lavi.minecraft.diagnostics.toolselect.support;

import adris.altoclef.AltoClef;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

//20260804_kpopmodder: Share tool-save diagnostic explanation strings while preserving existing output keys.
public final class ToolSavePolicyDiagnostics {
    private ToolSavePolicyDiagnostics() {
    }

    public static String computedDecision(AltoClef mod, ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> {
            String unavailable = unavailableReason(stack, targetState);
            if (unavailable != null) {
                return unavailable;
            }
            Item item = stack.getItem();
            if (item != Items.IRON_PICKAXE) {
                return "result=false#reason=NOT_IRON_PICKAXE";
            }
            boolean hasDiamondPickaxe = mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE);
            if (hasDiamondPickaxe) {
                return "result=false#reason=HAS_DIAMOND_PICKAXE";
            }
            boolean shouldSave = StorageHelper.shouldSaveStack(mod, targetState.getBlock(), stack);
            return formatDecision(stack, targetState, hasDiamondPickaxe, shouldSave);
        });
    }

    public static String observedDecision(AltoClef mod,
                                          ItemStack stack,
                                          BlockState targetState,
                                          boolean observedShouldSave) {
        return ChatClefDiagnostics.safeValue(() -> {
            String unavailable = unavailableReason(stack, targetState);
            if (unavailable != null) {
                return unavailable;
            }
            Item item = stack.getItem();
            if (item != Items.IRON_PICKAXE) {
                return "result=false#reason=NOT_IRON_PICKAXE";
            }
            boolean hasDiamondPickaxe = mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE);
            if (hasDiamondPickaxe) {
                return "result=false#reason=HAS_DIAMOND_PICKAXE";
            }
            return formatDecision(stack, targetState, hasDiamondPickaxe, observedShouldSave);
        });
    }

    public static String shouldSaveFromDecision(String saveDecision) {
        if (saveDecision == null || !saveDecision.startsWith("result=")) {
            return "unavailable";
        }
        int delimiter = saveDecision.indexOf('#');
        return delimiter < 0 ? saveDecision.substring("result=".length()) : saveDecision.substring("result=".length(), delimiter);
    }

    private static String unavailableReason(ItemStack stack, BlockState targetState) {
        if (stack == null) {
            return "result=unavailable#reason=NO_STACK";
        }
        if (targetState == null) {
            return "result=unavailable#reason=NO_TARGET_STATE";
        }
        return null;
    }

    private static String formatDecision(ItemStack stack,
                                         BlockState targetState,
                                         boolean hasDiamondPickaxe,
                                         boolean shouldSave) {
        Block block = targetState.getBlock();
        boolean diamondRelatedBlock = block.equals(Blocks.DIAMOND_BLOCK)
                || block.equals(Blocks.DIAMOND_ORE)
                || block.equals(Blocks.DEEPSLATE_DIAMOND_ORE);
        int damage = stack.getDamage();
        int maxDamage = stack.getMaxDamage();
        boolean criticalDurability = damage + 8 > maxDamage;
        boolean lowDurability = damage + 30 > maxDamage;
        MiningRequirement minimumRequirement = MiningRequirement.getMinimumRequirementForBlock(block);

        String reason = "NOT_LOW_DURABILITY";
        if (criticalDurability) {
            reason = diamondRelatedBlock ? "CRITICAL_DURABILITY_DIAMOND_RELATED" : "CRITICAL_DURABILITY_NON_DIAMOND";
        } else if (lowDurability) {
            reason = minimumRequirement.equals(MiningRequirement.IRON)
                    ? "LOW_DURABILITY_IRON_REQUIRED"
                    : "LOW_DURABILITY_BLOCK_NOT_IRON_REQUIRED";
        }

        return "result=" + shouldSave
                + "#reason=" + reason
                + "#hasDiamondPickaxe=" + hasDiamondPickaxe
                + "#damage=" + damage
                + "#maxDamage=" + maxDamage
                + "#damagePlus8=" + (damage + 8)
                + "#damagePlus30=" + (damage + 30)
                + "#diamondRelatedBlock=" + diamondRelatedBlock
                + "#minimumMiningRequirement=" + minimumRequirement;
    }
}
