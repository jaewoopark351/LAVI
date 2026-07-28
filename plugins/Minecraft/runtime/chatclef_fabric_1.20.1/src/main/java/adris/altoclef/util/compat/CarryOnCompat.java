package adris.altoclef.util.compat;

import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.util.compat.carryon.CarriedBlockStateProvider;
import adris.altoclef.util.compat.carryon.NoOpCarriedBlockStateProvider;
import adris.altoclef.util.compat.carryon.ReflectiveCarryOnCarriedBlockStateProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

//20260727_kpopmodder: Keep Carry On checks isolated so vanilla ChatClef behavior stays unchanged when the mod is absent.
public final class CarryOnCompat {

    private static final String MOD_ID = "carryon";
    private static final CarriedBlockStateProvider CARRIED_BLOCK_STATE_PROVIDER = createCarriedBlockStateProvider();

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

    public static boolean shouldAutoPlaceCarriedBlock(Block block) {
        return isLoaded() && isCarryOnAutoPlaceBlock(block);
    }

    public static CarriedBlockStateProvider carriedBlockStateProvider() {
        return CARRIED_BLOCK_STATE_PROVIDER;
    }

    public static boolean isCarryingBlock(PlayerEntity player) {
        return CARRIED_BLOCK_STATE_PROVIDER.isCarryingBlock(player);
    }

    public static Optional<BlockState> getCarriedBlockState(PlayerEntity player) {
        return CARRIED_BLOCK_STATE_PROVIDER.getCarriedBlockState(player);
    }

    public static Optional<Identifier> getCarriedBlockId(PlayerEntity player) {
        return CARRIED_BLOCK_STATE_PROVIDER.getCarriedBlockId(player);
    }

    public static boolean isCarryingBlock(PlayerEntity player, Block expectedBlock) {
        return CARRIED_BLOCK_STATE_PROVIDER.isCarryingBlock(player, expectedBlock);
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

    private static boolean isCarryOnAutoPlaceBlock(Block block) {
        if (block == null) {
            return false;
        }
        return block == Blocks.SMOKER
                || block == Blocks.FURNACE;
    }

    private static CarriedBlockStateProvider createCarriedBlockStateProvider() {
        if (!isLoaded()) {
            return new NoOpCarriedBlockStateProvider();
        }
        return ReflectiveCarryOnCarriedBlockStateProvider.create()
                .orElseGet(NoOpCarriedBlockStateProvider::new);
    }
}
