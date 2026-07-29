package adris.altoclef.tasks.container.access;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

//20260729_kpopmodder: Added this selector to isolate existing container target lookup from task execution.
public final class ContainerTargetSelector {

    private final Block[] containerBlocks;
    private final ContainerBlockValidator blockValidator;
    private final StateChangeLogger debugLogger;

    public ContainerTargetSelector(Block[] containerBlocks,
                                   ContainerBlockValidator blockValidator,
                                   StateChangeLogger debugLogger) {
        this.containerBlocks = containerBlocks;
        this.blockValidator = blockValidator;
        this.debugLogger = debugLogger;
    }

    public ContainerTargetPlan select(AltoClef mod,
                                      BlockPos overridePosition,
                                      BlockPos placedTaskPosition,
                                      BlockPos carriedPlacedPosition,
                                      double makeCost,
                                      boolean placeForceElapsed,
                                      boolean justPlacedElapsed) {
        Vec3d currentPos = mod.getPlayer().getPos();
        Optional<BlockPos> nearest;
        boolean usingPlacedContainer = false;

        Optional<BlockPos> placedContainer = getPlacedContainerIfValid(mod, carriedPlacedPosition, placedTaskPosition);
        if (placedContainer.isPresent()) {
            nearest = placedContainer;
            usingPlacedContainer = true;
            debugLogger.state("prefer placed container:" + nearest.get().toShortString(),
                    "prefer placed container: targetPosition=" + nearest.get().toShortString());
        } else if (overridePosition != null && mod.getBlockScanner().isBlockAtPosition(overridePosition, containerBlocks)) {
            // We have an override so go there instead.
            nearest = Optional.of(overridePosition);
        } else {
            // Track nearest container
            nearest = mod.getBlockScanner().getNearestBlock(currentPos, blockPos -> WorldHelper.canReach(blockPos), containerBlocks);
        }
        if (nearest.isEmpty()) {
            // If all else fails, try using our placed task
            nearest = getPlacedContainerIfValid(mod, carriedPlacedPosition, placedTaskPosition);
        }

        double costToWalk = Double.POSITIVE_INFINITY;
        if (nearest.isPresent()) {
            costToWalk = BaritoneHelper.calculateGenericHeuristic(currentPos, WorldHelper.toVec3d(nearest.get()));
        }

        return new ContainerTargetPlan(nearest,
                usingPlacedContainer,
                overridePosition,
                placedTaskPosition,
                carriedPlacedPosition,
                costToWalk,
                makeCost,
                placeForceElapsed,
                justPlacedElapsed);
    }

    private Optional<BlockPos> getPlacedContainerIfValid(AltoClef mod,
                                                        BlockPos carriedPlacedPosition,
                                                        BlockPos placedTaskPosition) {
        Optional<BlockPos> carriedPlaced = blockValidator.getPlacedContainerIfValid(mod, carriedPlacedPosition);
        if (carriedPlaced.isPresent()) {
            return carriedPlaced;
        }
        return blockValidator.getPlacedContainerIfValid(mod, placedTaskPosition);
    }
}
