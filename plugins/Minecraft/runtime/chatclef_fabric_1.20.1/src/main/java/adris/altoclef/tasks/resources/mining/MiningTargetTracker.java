package adris.altoclef.tasks.resources.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;
import java.util.function.Consumer;

//20260728_kpopmodder: Added this tracker to isolate current mining target lifetime, skip, and release policy.
final class MiningTargetTracker {
    private static final int MINING_TARGET_TIMEOUT_TICKS = 20 * 30;
    private static final int TEMPORARY_BLOCK_SKIP_TICKS = 20 * 45;

    private final Block[] blocks;
    private final TemporaryBlockBlacklist temporaryBlockBlacklist = new TemporaryBlockBlacklist();
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private BlockPos miningPos;
    private int miningTargetStartTick;

    MiningTargetTracker(Block[] blocks) {
        this.blocks = blocks;
    }

    void resetProgressIfPathing(AltoClef mod) {
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            progressChecker.reset();
        }
    }

    Optional<Skip> skipIfTimedOutOrStuck(AltoClef mod) {
        if (miningPos == null) {
            return Optional.empty();
        }
        if (WorldHelper.getTicks() - miningTargetStartTick > MINING_TARGET_TIMEOUT_TICKS) {
            return Optional.of(skipCurrentTarget(mod, "target timeout"));
        }
        if (!progressChecker.check(mod)) {
            return Optional.of(skipCurrentTarget(mod, "mining progress failed"));
        }
        return Optional.empty();
    }

    void releaseCompleted(AltoClef mod, Consumer<Release> onRelease) {
        if (miningPos == null || mod.getBlockScanner().isBlockAtPosition(miningPos, blocks)) {
            return;
        }
        onRelease.accept(releaseMiningTarget("target block changed"));
    }

    Optional<BlockPos> retainOrRelease(AltoClef mod, Consumer<Release> onRelease) {
        if (miningPos == null) {
            return Optional.empty();
        }

        String releaseReason = getMiningTargetReleaseReason(mod, miningPos);
        if (releaseReason != null) {
            onRelease.accept(releaseMiningTarget(releaseReason));
            return Optional.empty();
        }
        return Optional.of(miningPos);
    }

    Optional<ItemEntity> getDropCloserThanCurrentTarget(AltoClef mod, Vec3d pos, Optional<ItemEntity> closestDrop, double dropSq) {
        if (miningPos == null || closestDrop.isEmpty() || getMiningTargetReleaseReason(mod, miningPos) != null) {
            return Optional.empty();
        }
        ItemEntity drop = closestDrop.get();
        if (!MiningTargetScanner.isUsableDrop(drop)) {
            return Optional.empty();
        }
        if (dropSq <= currentTargetDistanceSq(pos)) {
            return Optional.of(drop);
        }
        return Optional.empty();
    }

    boolean selectMiningTarget(BlockPos newPos) {
        boolean changed = miningPos == null || !miningPos.equals(newPos);
        if (changed) {
            progressChecker.reset();
            miningTargetStartTick = WorldHelper.getTicks();
        }
        miningPos = newPos;
        return changed;
    }

    void clear() {
        miningPos = null;
        miningTargetStartTick = 0;
        progressChecker.reset();
    }

    void reset() {
        clear();
        temporaryBlockBlacklist.pruneExpired();
    }

    void pruneExpired() {
        temporaryBlockBlacklist.pruneExpired();
    }

    boolean isAllowedCandidate(BlockPos pos) {
        return !temporaryBlockBlacklist.contains(pos);
    }

    int skippedBlockCount() {
        return temporaryBlockBlacklist.size();
    }

    int targetTimeoutTicks() {
        return MINING_TARGET_TIMEOUT_TICKS;
    }

    boolean isMining() {
        return miningPos != null;
    }

    BlockPos miningPos() {
        return miningPos;
    }

    double currentTargetDistanceSq(Vec3d pos) {
        if (miningPos == null) {
            return Double.POSITIVE_INFINITY;
        }
        return BlockPosVer.getSquaredDistance(miningPos, pos);
    }

    private Skip skipCurrentTarget(AltoClef mod, String reason) {
        BlockPos skipped = miningPos;
        temporaryBlockBlacklist.add(skipped, TEMPORARY_BLOCK_SKIP_TICKS);
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        mod.getBlockScanner().requestBlockUnreachable(skipped, 2);
        clear();
        return new Skip(skipped, reason, TEMPORARY_BLOCK_SKIP_TICKS, temporaryBlockBlacklist.size());
    }

    private Release releaseMiningTarget(String reason) {
        BlockPos released = miningPos;
        boolean blockChanged = "target block changed".equals(reason);
        clear();
        return new Release(released, reason, blockChanged);
    }

    private String getMiningTargetReleaseReason(AltoClef mod, BlockPos pos) {
        if (!mod.getBlockScanner().isBlockAtPosition(pos, blocks)) {
            return "target block changed";
        }
        if (temporaryBlockBlacklist.contains(pos)) {
            return "target temporarily skipped";
        }
        if (mod.getBlockScanner().isUnreachable(pos)) {
            return "target marked unreachable";
        }
        if (!WorldHelper.canBreak(pos)) {
            return "target cannot be broken";
        }
        return null;
    }

    static final class Release {
        private final BlockPos pos;
        private final String reason;
        private final boolean blockChanged;

        private Release(BlockPos pos, String reason, boolean blockChanged) {
            this.pos = pos;
            this.reason = reason;
            this.blockChanged = blockChanged;
        }

        BlockPos pos() {
            return pos;
        }

        String reason() {
            return reason;
        }

        boolean blockChanged() {
            return blockChanged;
        }
    }

    static final class Skip {
        private final BlockPos pos;
        private final String reason;
        private final int skipTicks;
        private final int skippedBlocks;

        private Skip(BlockPos pos, String reason, int skipTicks, int skippedBlocks) {
            this.pos = pos;
            this.reason = reason;
            this.skipTicks = skipTicks;
            this.skippedBlocks = skippedBlocks;
        }

        BlockPos pos() {
            return pos;
        }

        String reason() {
            return reason;
        }

        int skipTicks() {
            return skipTicks;
        }

        int skippedBlocks() {
            return skippedBlocks;
        }
    }
}
