package adris.altoclef.tasks.container.access;

import adris.altoclef.AltoClef;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260729_kpopmodder: Added this helper to isolate container block validation and Carry On block detection.
public final class ContainerBlockValidator {

    private final Block[] containerBlocks;
    private final StateChangeLogger debugLogger;

    public ContainerBlockValidator(Block[] containerBlocks, StateChangeLogger debugLogger) {
        this.containerBlocks = containerBlocks;
        this.debugLogger = debugLogger;
    }

    public Optional<BlockPos> getPlacedContainerIfValid(AltoClef mod, BlockPos placed) {
        if (placed == null) {
            return Optional.empty();
        }
        if (!isPlacedContainerBlock(mod, placed)) {
            debugLogger.state("placed container rejected:" + placed.toShortString() + ":not-block",
                    "placed container rejected: pos=" + placed.toShortString()
                            + ", reason=not-container-block-or-unloaded");
            return Optional.empty();
        }
        if (!WorldHelper.canReach(placed)) {
            debugLogger.state("placed container rejected:" + placed.toShortString() + ":unreachable",
                    "placed container rejected: pos=" + placed.toShortString()
                            + ", reason=unreachable");
            return Optional.empty();
        }
        return Optional.of(placed);
    }

    public Optional<BlockState> getCarriedContainerState(AltoClef mod) {
        return CarryOnCompat.getCarriedBlockState(mod.getPlayer())
                .filter(state -> isContainerBlock(state.getBlock()));
    }

    public boolean shouldUseCarryOnSafeInteraction() {
        return CarryOnCompat.shouldAvoidSneakRightClick(containerBlocks);
    }

    private boolean isPlacedContainerBlock(AltoClef mod, BlockPos placed) {
        if (!mod.getChunkTracker().isChunkLoaded(placed)) {
            return false;
        }
        return isContainerBlock(mod.getWorld().getBlockState(placed).getBlock());
    }

    private boolean isContainerBlock(Block block) {
        for (Block containerBlock : containerBlocks) {
            if (block == containerBlock) {
                return true;
            }
        }
        return false;
    }
}
