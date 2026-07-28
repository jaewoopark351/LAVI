package adris.altoclef.tasks.resources.mining;

import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

//20260729_kpopmodder: Added this logger to keep mining/drop choice reasons out of task orchestration.
final class MiningChoiceLogger {
    private final MineOrCollectDiagnostics diagnostics;
    private final StateChangeLogger debugLogger;
    private final Function<ItemEntity, String> dropFormatter;

    MiningChoiceLogger(MineOrCollectDiagnostics diagnostics,
                       StateChangeLogger debugLogger,
                       Function<ItemEntity, String> dropFormatter) {
        this.diagnostics = diagnostics;
        this.debugLogger = debugLogger;
        this.dropFormatter = dropFormatter;
    }

    MiningTargetChoice interactionPausedDropPreferred(Optional<ItemEntity> closestDrop,
                                                      Optional<BlockPos> closestBlock,
                                                      int skippedBlocks) {
        diagnostics.recordInteractionPausedDropPreferred();
        debugLogger.state("interaction paused; prefer dropped item: drop=" + describeDrop(closestDrop)
                + ", block=" + describeBlock(closestBlock)
                + ", skippedBlocks=" + skippedBlocks);
        return closestDrop
                .map(drop -> MiningTargetChoice.drop(drop, MiningTargetChoice.Reason.INTERACTION_PAUSED_DROP_PREFERRED))
                .orElseGet(() -> MiningTargetChoice.empty(MiningTargetChoice.Reason.INTERACTION_PAUSED_DROP_PREFERRED));
    }

    MiningTargetChoice pickupCoordinatorPreferred(Object target) {
        return MiningTargetChoice.target(target, MiningTargetChoice.Reason.PICKUP_COORDINATOR_PREFERRED);
    }

    MiningTargetChoice dropInterruptsMiningTarget(ItemEntity drop,
                                                  MiningTargetTracker miningTargetTracker,
                                                  Vec3d playerPos,
                                                  double dropSq) {
        diagnostics.recordDropPreferred();
        debugLogger.state("closest drop interrupts mining target " + drop.getUuid(),
                "closest dropped item interrupts retained mining target: drop="
                        + describeDrop(drop)
                        + ", miningTarget=" + describePos(miningTargetTracker.miningPos())
                        + ", miningSq=" + formatDouble(miningTargetTracker.currentTargetDistanceSq(playerPos))
                        + ", dropSq=" + formatDouble(dropSq));
        return MiningTargetChoice.drop(drop, MiningTargetChoice.Reason.DROP_INTERRUPTS_MINING_TARGET);
    }

    MiningTargetChoice retainMiningTarget(BlockPos retained,
                                          Optional<BlockPos> closestBlock,
                                          double blockSq,
                                          Optional<ItemEntity> closestDrop,
                                          double dropSq,
                                          Vec3d playerPos) {
        diagnostics.recordBlockPreferred();
        diagnostics.recordMiningTargetRetained();
        debugLogger.state("retain mining target " + retained.toShortString(),
                "retained mining target: current=" + retained.toShortString()
                        + ", currentSq=" + formatDouble(BlockPosVer.getSquaredDistance(retained, playerPos))
                        + ", nearestBlock=" + describeBlock(closestBlock)
                        + ", nearestBlockSq=" + formatDouble(blockSq)
                        + ", drop=" + describeDrop(closestDrop)
                        + ", dropSq=" + formatDouble(dropSq));
        return MiningTargetChoice.block(retained, MiningTargetChoice.Reason.RETAIN_MINING_TARGET);
    }

    MiningTargetChoice localMiningTarget(BlockPos local,
                                         LocalMiningSessionPolicy localMiningSessionPolicy,
                                         Optional<ItemEntity> closestDrop,
                                         double dropSq) {
        diagnostics.recordBlockPreferred();
        diagnostics.recordLocalMiningPreferred();
        debugLogger.state("local mining session prefers block " + local.toShortString(),
                "local mining session prefers block: block=" + local.toShortString()
                        + ", anchor=" + localMiningSessionPolicy.describeAnchor()
                        + ", ticksRemaining=" + localMiningSessionPolicy.ticksRemaining()
                        + ", drop=" + describeDrop(closestDrop)
                        + ", dropSq=" + formatDouble(dropSq));
        return MiningTargetChoice.block(local, MiningTargetChoice.Reason.LOCAL_MINING_SESSION);
    }

    MiningTargetChoice closestDrop(Optional<ItemEntity> closestDrop,
                                   double dropSq,
                                   double blockSq,
                                   int skippedBlocks) {
        diagnostics.recordDropPreferred();
        debugLogger.state("closest target is dropped item: drop=" + describeDrop(closestDrop)
                + ", dropSq=" + formatDouble(dropSq)
                + ", blockSq=" + formatDouble(blockSq)
                + ", skippedBlocks=" + skippedBlocks);
        return closestDrop
                .map(drop -> MiningTargetChoice.drop(drop, MiningTargetChoice.Reason.CLOSEST_DROP))
                .orElseGet(() -> MiningTargetChoice.empty(MiningTargetChoice.Reason.CLOSEST_DROP));
    }

    MiningTargetChoice closestBlock(Optional<BlockPos> closestBlock,
                                    double blockSq,
                                    double dropSq,
                                    int skippedBlocks) {
        diagnostics.recordBlockPreferred();
        debugLogger.state("closest target is block: block=" + describeBlock(closestBlock)
                + ", blockSq=" + formatDouble(blockSq)
                + ", dropSq=" + formatDouble(dropSq)
                + ", skippedBlocks=" + skippedBlocks);
        return closestBlock
                .map(block -> MiningTargetChoice.block(block, MiningTargetChoice.Reason.CLOSEST_BLOCK))
                .orElseGet(() -> MiningTargetChoice.empty(MiningTargetChoice.Reason.CLOSEST_BLOCK));
    }

    void newMiningTarget(BlockPos newPos, int timeoutTicks) {
        debugLogger.state("new mining target: pos=" + newPos.toShortString()
                + ", timeoutTicks=" + timeoutTicks);
    }

    private String describeDrop(Optional<ItemEntity> drop) {
        return drop.map(this::describeDrop).orElse("none");
    }

    private String describeDrop(ItemEntity drop) {
        return dropFormatter.apply(drop);
    }

    private String describeBlock(Optional<BlockPos> block) {
        return block.map(BlockPos::toShortString).orElse("none");
    }

    private String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

    private String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "infinity";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
