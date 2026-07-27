package adris.altoclef.util.compat;

import adris.altoclef.multiversion.versionedfields.Blocks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;

//20260727_kpopmodder: Keep Carry On checks isolated so vanilla ChatClef behavior stays unchanged when the mod is absent.
public final class CarryOnCompat {

    private static final String MOD_ID = "carryon";

    private CarryOnCompat() {
    }

    public static boolean isLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded(MOD_ID);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static boolean shouldAvoidSneakRightClick(Block... targetBlocks) {
        return isLoaded() && containsCarryOnSensitiveBlock(targetBlocks);
    }

    public static boolean containsCarryOnSensitiveBlock(Block... targetBlocks) {
        if (targetBlocks == null) {
            return false;
        }
        for (Block block : targetBlocks) {
            if (isCarryOnSensitiveBlock(block)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCarryOnSensitiveBlock(Block block) {
        if (block == null) {
            return false;
        }
        return block == Blocks.SMOKER
                || block == Blocks.FURNACE
                || block == Blocks.BLAST_FURNACE
                || block == Blocks.CRAFTING_TABLE
                || block == Blocks.CHEST
                || block == Blocks.TRAPPED_CHEST
                || block == Blocks.BARREL
                || block == Blocks.ANVIL
                || block == Blocks.CHIPPED_ANVIL
                || block == Blocks.DAMAGED_ANVIL
                || block instanceof ShulkerBoxBlock;
    }
}
