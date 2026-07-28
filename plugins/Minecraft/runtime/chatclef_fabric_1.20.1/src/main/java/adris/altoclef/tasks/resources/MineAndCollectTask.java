package adris.altoclef.tasks.resources;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.multiversion.ToolMaterialVer;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.tasks.movement.PickupDroppedItemTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.time.TimerGame;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MiningToolItem;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.function.Predicate;

public class MineAndCollectTask extends ResourceTask {

    private final Block[] _blocksToMine;

    private final MiningRequirement _requirement;

    private final TimerGame _cursorStackTimer = new TimerGame(3);

    private final MineOrCollectTask _subtask;
    private final StateChangeLogger debugLogger = new StateChangeLogger("MineAndCollectTask");

    public MineAndCollectTask(ItemTarget[] itemTargets, Block[] blocksToMine, MiningRequirement requirement) {
        super(itemTargets);
        _requirement = requirement;
        _blocksToMine = blocksToMine;
        _subtask = new MineOrCollectTask(_blocksToMine, this.itemTargets);
    }

    public MineAndCollectTask(ItemTarget[] blocksToMine, MiningRequirement requirement) {
        this(blocksToMine, itemTargetToBlockList(blocksToMine), requirement);
    }

    public MineAndCollectTask(ItemTarget target, Block[] blocksToMine, MiningRequirement requirement) {
        this(new ItemTarget[]{target}, blocksToMine, requirement);
    }

    public MineAndCollectTask(Item item, int count, Block[] blocksToMine, MiningRequirement requirement) {
        this(new ItemTarget(item, count), blocksToMine, requirement);
    }

    public static Block[] itemTargetToBlockList(ItemTarget[] targets) {
        List<Block> result = new ArrayList<>(targets.length);
        for (ItemTarget target : targets) {
            for (Item item : target.getMatches()) {
                Block block = Block.getBlockFromItem(item);
                if (block != null && !WorldHelper.isAir(block)) {
                    result.add(block);
                }
            }
        }
        return result.toArray(Block[]::new);
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();

        // We're mining, so don't throw away pickaxes.
        mod.getBehaviour().addProtectedItems(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);

        _subtask.resetSearch();
        debugLogger.event("start: targets=" + Arrays.toString(itemTargets)
                + ", blocks=" + Arrays.toString(_blocksToMine)
                + ", requirement=" + _requirement);
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // Picking up is controlled by a separate task here.
        return true;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!StorageHelper.miningRequirementMet(_requirement)) {
            debugLogger.state("satisfy mining requirement first: requirement=" + _requirement
                    + ", targets=" + Arrays.toString(itemTargets));
            return new SatisfyMiningRequirementTask(_requirement);
        }

        if (_subtask.isMining()) {
            debugLogger.state("mining active: pos=" + describePos(_subtask.miningPos())
                    + ", targets=" + Arrays.toString(itemTargets));
            makeSureToolIsEquipped(mod);
        }

        // Wrong dimension check.
        if (_subtask.wasWandering() && isInWrongDimension(mod) && !mod.getBlockScanner().anyFound(_blocksToMine)) {
            debugLogger.state("wrong dimension while mining; traveling: current=" + WorldHelper.getCurrentDimension()
                    + ", targets=" + Arrays.toString(itemTargets));
            return getToCorrectDimensionTask(mod);
        }

        debugLogger.state("delegate to mine/collect subtask: targets=" + Arrays.toString(itemTargets));
        return _subtask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        debugLogger.event("stop: interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName())
                + ", targets=" + Arrays.toString(itemTargets));
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof MineAndCollectTask task) {
            return Arrays.equals(task._blocksToMine, _blocksToMine);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return "Mine And Collect";
    }

    private void makeSureToolIsEquipped(AltoClef mod) {
        if (_cursorStackTimer.elapsed() && !mod.getFoodChain().needsToEat()) {
            assert MinecraftClient.getInstance().player != null;
            ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
            if (cursorStack != null && !cursorStack.isEmpty()) {
                // We have something in our cursor stack
                Item item = cursorStack.getItem();
                if (item.getDefaultStack().isSuitableFor(mod.getWorld().getBlockState(_subtask.miningPos()))) {
                    // Our cursor stack would help us mine our current block
                    Item currentlyEquipped = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
                    if (item instanceof MiningToolItem) {
                        if (currentlyEquipped instanceof MiningToolItem currentPick) {
                            MiningToolItem swapPick = (MiningToolItem) item;
                            if (ToolMaterialVer.getMiningLevel(swapPick) > ToolMaterialVer.getMiningLevel(currentPick)) {
                                // We can equip a better pickaxe.
                                mod.getSlotHandler().forceEquipSlot(CursorSlot.SLOT);
                            }
                        } else {
                            // We're not equipped with a pickaxe...
                            mod.getSlotHandler().forceEquipSlot(CursorSlot.SLOT);
                        }
                    }
                }
            }
            _cursorStackTimer.reset();
        }
    }

    public static class MineOrCollectTask extends AbstractDoToClosestObjectTask<Object> {

        private static final int MINING_TARGET_TIMEOUT_TICKS = 20 * 30;
        private static final int TEMPORARY_BLOCK_SKIP_TICKS = 20 * 45;
        private static final int DROPPED_ITEM_PICKUP_GRACE_TICKS = 20 * 5;
        private static final int ACTIVE_PICKUP_CONTINUATION_TICKS = 20 * 2;
        private static final double DROPPED_ITEM_PICKUP_GRACE_RANGE = 16;

        private static final PickupContinuationGoal PICKUP_CONTINUATION_GOAL = new PickupContinuationGoal();

        private final Block[] _blocks;
        private final ItemTarget[] _targets;
        private final TemporaryBlockBlacklist temporaryBlockBlacklist = new TemporaryBlockBlacklist();
        private final PickupContinuationPolicy pickupContinuationPolicy = new PickupContinuationPolicy(
                DROPPED_ITEM_PICKUP_GRACE_TICKS,
                ACTIVE_PICKUP_CONTINUATION_TICKS,
                DROPPED_ITEM_PICKUP_GRACE_RANGE
        );
        private final MovementProgressChecker progressChecker = new MovementProgressChecker();
        private final Task _pickupTask;
        private BlockPos miningPos;
        private int miningTargetStartTick;
        private final StateChangeLogger debugLogger = new StateChangeLogger("MineOrCollectTask");
        //20260728_kpopmodder: Count block/drop choice stability so latest.log can show whether pickup switching calmed down.
        private int blockPreferredCount = 0;
        private int dropPreferredCount = 0;
        private int pickupGracePreferredCount = 0;
        private int pickupContinuationPreferredCount = 0;
        private int interactionPausedDropPreferredCount = 0;
        private int miningTargetSwitchCount = 0;
        private int miningTargetRetainCount = 0;
        private int miningTargetReleaseCount = 0;
        private int pickupTargetSwitchCount = 0;
        private int temporaryMiningSkipCount = 0;
        private String lastSelectedGoalKey = "";

        public MineOrCollectTask(Block[] blocks, ItemTarget[] targets) {
            _blocks = blocks;
            _targets = targets;
            _pickupTask = new PickupDroppedItemTask(_targets, true);
        }

        @Override
        protected Vec3d getPos(AltoClef mod, Object obj) {
            if (obj instanceof BlockPos b) {
                return WorldHelper.toVec3d(b);
            }
            if (obj instanceof ItemEntity item) {
                return item.getPos();
            }
            if (obj instanceof PickupContinuationGoal) {
                return pickupContinuationPolicy.activePickupPos(mod.getPlayer().getPos());
            }
            throw new UnsupportedOperationException("Shouldn't try to get the position of object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
        }

        @Override
        protected Optional<Object> getClosestTo(AltoClef mod, Vec3d pos) {
            temporaryBlockBlacklist.pruneExpired();
            pickupContinuationPolicy.pruneExpired();

            Pair<Double, Optional<BlockPos>> closestBlock = getClosestBlock(mod,pos, this::isAllowedMiningCandidate, _blocks);
            Pair<Double, Optional<ItemEntity>> closestDrop = getClosestItemDrop(mod,pos,  _targets);

            double blockSq = closestBlock.getLeft();
            double dropSq = closestDrop.getLeft();

            // We can't mine right now.
            if (mod.getExtraBaritoneSettings().isInteractionPaused()) {
                interactionPausedDropPreferredCount++;
                debugLogger.state("interaction paused; prefer dropped item: drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                        + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                        + ", skippedBlocks=" + temporaryBlockBlacklist.size());
                return closestDrop.getRight().map(Object.class::cast);
            }

            Optional<ItemEntity> graceDrop = pickupContinuationPolicy.getPreferredMinedDrop(closestDrop.getRight());
            if (graceDrop.isPresent()) {
                pickupGracePreferredCount++;
                debugLogger.state("pickup grace prefers dropped item",
                        "pickup grace prefers dropped item: drop=" + describeDrop(graceDrop.get())
                                + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                                + ", ticksRemaining=" + pickupContinuationPolicy.miningGraceTicksRemaining());
                return graceDrop.map(Object.class::cast);
            }

            Optional<ItemEntity> continuationDrop = pickupContinuationPolicy.getPreferredActivePickupDrop(
                    closestDrop.getRight(),
                    isPickupTaskContinuing(),
                    pos
            );
            if (continuationDrop.isPresent()) {
                pickupContinuationPreferredCount++;
                debugLogger.state("active pickup prefers dropped item",
                        "active pickup prefers dropped item: drop=" + describeDrop(continuationDrop.get())
                                + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                                + ", ticksRemaining=" + pickupContinuationPolicy.activePickupTicksRemaining());
                return continuationDrop.map(Object.class::cast);
            }

            if (pickupContinuationPolicy.shouldContinueActivePickup(isPickupTaskContinuing())) {
                pickupContinuationPreferredCount++;
                debugLogger.state("active pickup settling",
                        "active pickup settling: keeping pickup task alive"
                                + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                                + ", drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                                + ", ticksRemaining=" + pickupContinuationPolicy.activePickupTicksRemaining());
                return Optional.of((Object) PICKUP_CONTINUATION_GOAL);
            }

            Optional<ItemEntity> currentMiningInterruptDrop = getDropCloserThanCurrentMiningTarget(mod, pos, closestDrop.getRight(), dropSq);
            if (currentMiningInterruptDrop.isPresent()) {
                dropPreferredCount++;
                debugLogger.state("closest drop interrupts mining target " + currentMiningInterruptDrop.get().getUuid(),
                        "closest dropped item interrupts retained mining target: drop="
                                + describeDrop(currentMiningInterruptDrop.get())
                                + ", miningTarget=" + describePos(miningPos)
                                + ", miningSq=" + formatDouble(currentMiningTargetDistanceSq(pos))
                                + ", dropSq=" + formatDouble(dropSq));
                return currentMiningInterruptDrop.map(Object.class::cast);
            }

            Optional<BlockPos> retainedMiningTarget = getRetainedMiningTarget(mod);
            if (retainedMiningTarget.isPresent()) {
                blockPreferredCount++;
                miningTargetRetainCount++;
                BlockPos retained = retainedMiningTarget.get();
                debugLogger.state("retain mining target " + retained.toShortString(),
                        "retained mining target: current=" + retained.toShortString()
                                + ", currentSq=" + formatDouble(BlockPosVer.getSquaredDistance(retained, pos))
                                + ", nearestBlock=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                                + ", nearestBlockSq=" + formatDouble(blockSq)
                                + ", drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                                + ", dropSq=" + formatDouble(dropSq));
                return retainedMiningTarget.map(Object.class::cast);
            }

            if (dropSq <= blockSq) {
                dropPreferredCount++;
                debugLogger.state("closest target is dropped item: drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                        + ", dropSq=" + formatDouble(dropSq)
                        + ", blockSq=" + formatDouble(blockSq)
                        + ", skippedBlocks=" + temporaryBlockBlacklist.size());
                return closestDrop.getRight().map(Object.class::cast);
            } else {
                blockPreferredCount++;
                debugLogger.state("closest target is block: block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                        + ", blockSq=" + formatDouble(blockSq)
                        + ", dropSq=" + formatDouble(dropSq)
                        + ", skippedBlocks=" + temporaryBlockBlacklist.size());
                return closestBlock.getRight().map(Object.class::cast);
            }
        }

        public static Pair<Double, Optional<ItemEntity>> getClosestItemDrop(AltoClef mod,Vec3d pos, ItemTarget... items) {
            Optional<ItemEntity> closestDrop = Optional.empty();
            if (mod.getEntityTracker().itemDropped(items)) {
                closestDrop = mod.getEntityTracker().getClosestItemDrop(pos, items);
            }

            return new Pair<>(
                    // + 5 to make the bot stop mining a bit less
                    closestDrop.map(itemEntity -> itemEntity.squaredDistanceTo(pos) + 10).orElse(Double.POSITIVE_INFINITY),
                    closestDrop
            );
        }

        public static Pair<Double,Optional<BlockPos> > getClosestBlock(AltoClef mod,Vec3d pos ,Block... blocks) {
            return getClosestBlock(mod, pos, check -> true, blocks);
        }

        public static Pair<Double,Optional<BlockPos> > getClosestBlock(AltoClef mod, Vec3d pos, Predicate<BlockPos> isValidTest, Block... blocks) {
            Optional<BlockPos> closestBlock = mod.getBlockScanner().getNearestBlock(pos, check -> {

                if (mod.getBlockScanner().isUnreachable(check)) return false;
                if (!isValidTest.test(check)) return false;
                return WorldHelper.canBreak(check);
            }, blocks);

            return new Pair<>(
                    closestBlock.map(blockPos -> BlockPosVer.getSquaredDistance(blockPos, pos)).orElse(Double.POSITIVE_INFINITY),
                    closestBlock
            );
        }

        @Override
        protected Vec3d getOriginPos(AltoClef mod) {
            return mod.getPlayer().getPos();
        }

        @Override
        protected Task onTick() {
            AltoClef mod = AltoClef.getInstance();

            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                progressChecker.reset();
            }
            if (miningPos != null && WorldHelper.getTicks() - miningTargetStartTick > MINING_TARGET_TIMEOUT_TICKS) {
                temporarilySkipMiningTarget(mod, "target timeout");
            }
            if (miningPos != null && !progressChecker.check(mod)) {
                temporarilySkipMiningTarget(mod, "mining progress failed");
            }
            return super.onTick();
        }

        @Override
        protected Task getGoalTask(Object obj) {
            if (obj instanceof BlockPos newPos) {
                if (miningPos == null || !miningPos.equals(newPos)) {
                    recordGoalSelection("block:" + newPos.toShortString(), true);
                    progressChecker.reset();
                    miningTargetStartTick = WorldHelper.getTicks();
                    debugLogger.state("new mining target: pos=" + newPos.toShortString()
                            + ", timeoutTicks=" + MINING_TARGET_TIMEOUT_TICKS);
                }
                miningPos = newPos;
                pickupContinuationPolicy.armAfterMining(newPos);
                return new DestroyBlockTask(miningPos);
            }
            if (obj instanceof ItemEntity drop) {
                recordGoalSelection("drop:" + drop.getUuid(), false);
                debugLogger.state("pickup target selected: " + describeDrop(drop));
                miningPos = null;
                pickupContinuationPolicy.armActivePickup(drop);
                return _pickupTask;
            }
            if (obj instanceof PickupContinuationGoal) {
                recordGoalSelection("drop:active-pickup-continuation", false);
                debugLogger.state("continue active pickup while drop settles",
                        "continue active pickup while drop settles: ticksRemaining="
                                + pickupContinuationPolicy.activePickupTicksRemaining());
                miningPos = null;
                return _pickupTask;
            }
            throw new UnsupportedOperationException("Shouldn't try to get the goal from object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
        }

        @Override
        protected boolean isValid(AltoClef mod, Object obj) {
            if (obj instanceof BlockPos b) {
                return mod.getBlockScanner().isBlockAtPosition(b, _blocks)
                        && !temporaryBlockBlacklist.contains(b)
                        && !mod.getBlockScanner().isUnreachable(b)
                        && WorldHelper.canBreak(b);
            }
            if (obj instanceof ItemEntity drop) {
                Item item = drop.getStack().getItem();
                if (_targets != null) {
                    for (ItemTarget target : _targets) {
                        if (target.matches(item)) return true;
                    }
                }
                return false;
            }
            if (obj instanceof PickupContinuationGoal) {
                return pickupContinuationPolicy.shouldContinueActivePickup(isPickupTaskContinuing());
            }
            return false;
        }

        @Override
        protected void onStart() {
            progressChecker.reset();
            miningPos = null;
            miningTargetStartTick = 0;
            temporaryBlockBlacklist.pruneExpired();
            pickupContinuationPolicy.reset();
            resetDiagnostics();
            debugLogger.event("start: blocks=" + Arrays.toString(_blocks)
                    + ", targets=" + Arrays.toString(_targets));
        }

        @Override
        protected void onStop(Task interruptTask) {
            debugLogger.event("stop: interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName())
                    + ", miningPos=" + describePos(miningPos));
            if (hasDiagnostics()) {
                debugLogger.event("choice summary: interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName())
                        + ", blockPreferredTicks=" + blockPreferredCount
                        + ", dropPreferredTicks=" + dropPreferredCount
                        + ", pickupGracePreferredTicks=" + pickupGracePreferredCount
                        + ", pickupContinuationPreferredTicks=" + pickupContinuationPreferredCount
                        + ", interactionPausedDropPreferredTicks=" + interactionPausedDropPreferredCount
                        + ", miningTargetSwitches=" + miningTargetSwitchCount
                        + ", miningTargetRetainedTicks=" + miningTargetRetainCount
                        + ", miningTargetReleases=" + miningTargetReleaseCount
                        + ", pickupTargetSwitches=" + pickupTargetSwitchCount
                        + ", temporaryMiningSkips=" + temporaryMiningSkipCount
                        + ", lastGoal=" + lastSelectedGoalKey);
            }
            pickupContinuationPolicy.reset();
        }

        @Override
        protected boolean isEqual(Task other) {
            if (other instanceof MineOrCollectTask task) {
                return Arrays.equals(task._blocks, _blocks) && Arrays.equals(task._targets, _targets);
            }
            return false;
        }

        @Override
        protected String toDebugString() {
            return "Mining or Collecting";
        }

        public boolean isMining() {
            return miningPos != null;
        }

        public BlockPos miningPos() {
            return miningPos;
        }

        private boolean isPickupTaskContinuing() {
            return _pickupTask.isActive()
                    && !_pickupTask.isFinished()
                    && !_pickupTask.thisOrChildAreTimedOut();
        }

        private void temporarilySkipMiningTarget(AltoClef mod, String reason) {
            if (miningPos == null) {
                return;
            }

            BlockPos skipped = miningPos;
            temporaryMiningSkipCount++;
            temporaryBlockBlacklist.add(skipped, TEMPORARY_BLOCK_SKIP_TICKS);
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            Debug.logMessage("Temporarily skipping mining target " + skipped.toShortString() + " (" + reason + ").");
            debugLogger.state("temporarily skip mining target: pos=" + skipped.toShortString()
                    + ", reason=" + reason
                    + ", skipTicks=" + TEMPORARY_BLOCK_SKIP_TICKS
                    + ", skippedBlocks=" + temporaryBlockBlacklist.size());
            mod.getBlockScanner().requestBlockUnreachable(skipped, 2);
            miningPos = null;
            miningTargetStartTick = 0;
            progressChecker.reset();
            resetSearch();
        }

        private Optional<BlockPos> getRetainedMiningTarget(AltoClef mod) {
            if (miningPos == null) {
                return Optional.empty();
            }

            String releaseReason = getMiningTargetReleaseReason(mod, miningPos);
            if (releaseReason != null) {
                releaseMiningTarget(releaseReason);
                return Optional.empty();
            }
            return Optional.of(miningPos);
        }

        private Optional<ItemEntity> getDropCloserThanCurrentMiningTarget(AltoClef mod, Vec3d pos, Optional<ItemEntity> closestDrop, double dropSq) {
            if (miningPos == null || closestDrop.isEmpty() || getMiningTargetReleaseReason(mod, miningPos) != null) {
                return Optional.empty();
            }
            ItemEntity drop = closestDrop.get();
            if (!isUsableDrop(drop)) {
                return Optional.empty();
            }
            if (dropSq <= currentMiningTargetDistanceSq(pos)) {
                return Optional.of(drop);
            }
            return Optional.empty();
        }

        private double currentMiningTargetDistanceSq(Vec3d pos) {
            if (miningPos == null) {
                return Double.POSITIVE_INFINITY;
            }
            return BlockPosVer.getSquaredDistance(miningPos, pos);
        }

        private String getMiningTargetReleaseReason(AltoClef mod, BlockPos pos) {
            if (!mod.getBlockScanner().isBlockAtPosition(pos, _blocks)) {
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

        private void releaseMiningTarget(String reason) {
            if (miningPos == null) {
                return;
            }
            BlockPos released = miningPos;
            miningTargetReleaseCount++;
            debugLogger.state("release mining target " + released.toShortString() + " " + reason,
                    "released mining target: pos=" + released.toShortString()
                            + ", reason=" + reason);
            miningPos = null;
            miningTargetStartTick = 0;
            progressChecker.reset();
        }

        private boolean isAllowedMiningCandidate(BlockPos pos) {
            return !temporaryBlockBlacklist.contains(pos);
        }

        private boolean isUsableDrop(ItemEntity drop) {
            return drop != null && drop.isAlive() && !drop.getStack().isEmpty();
        }

        private String describeDrop(ItemEntity drop) {
            return drop.getStack().getItem().getTranslationKey()
                    + " x " + drop.getStack().getCount()
                    + " at " + drop.getBlockPos().toShortString();
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

        private void recordGoalSelection(String goalKey, boolean miningGoal) {
            if (goalKey.equals(lastSelectedGoalKey)) {
                return;
            }
            lastSelectedGoalKey = goalKey;
            if (miningGoal) {
                miningTargetSwitchCount++;
            } else {
                pickupTargetSwitchCount++;
            }
        }

        private boolean hasDiagnostics() {
            return blockPreferredCount
                    + dropPreferredCount
                    + pickupGracePreferredCount
                    + pickupContinuationPreferredCount
                    + interactionPausedDropPreferredCount
                    + miningTargetSwitchCount
                    + miningTargetRetainCount
                    + miningTargetReleaseCount
                    + pickupTargetSwitchCount
                    + temporaryMiningSkipCount > 0;
        }

        private void resetDiagnostics() {
            blockPreferredCount = 0;
            dropPreferredCount = 0;
            pickupGracePreferredCount = 0;
            pickupContinuationPreferredCount = 0;
            interactionPausedDropPreferredCount = 0;
            miningTargetSwitchCount = 0;
            miningTargetRetainCount = 0;
            miningTargetReleaseCount = 0;
            pickupTargetSwitchCount = 0;
            temporaryMiningSkipCount = 0;
            lastSelectedGoalKey = "";
        }

        //20260728_kpopmodder: Sentinel goal keeps active pickup alive briefly while item entities settle.
        private static class PickupContinuationGoal {
        }

        //20260727_kpopmodder: Keep per-task block cooldown bookkeeping separate from mining candidate selection.
        private static class TemporaryBlockBlacklist {
            private final Map<BlockPos, Integer> skipUntilTick = new HashMap<>();

            public void add(BlockPos pos, int ticks) {
                skipUntilTick.put(pos, WorldHelper.getTicks() + ticks);
            }

            public boolean contains(BlockPos pos) {
                pruneExpired();
                Integer untilTick = skipUntilTick.get(pos);
                return untilTick != null && untilTick > WorldHelper.getTicks();
            }

            public int size() {
                pruneExpired();
                return skipUntilTick.size();
            }

            public void pruneExpired() {
                int currentTick = WorldHelper.getTicks();
                skipUntilTick.entrySet().removeIf(entry -> entry.getValue() <= currentTick);
            }
        }

        //20260728_kpopmodder: Keeps mine/drop switching policy separate from target scoring.
        private static class PickupContinuationPolicy {
            private final int miningGraceTicks;
            private final int activePickupTicks;
            private final double maxDistanceSq;
            private int miningGraceUntilTick;
            private int activePickupUntilTick;
            private BlockPos miningOrigin;
            private Vec3d activePickupPos;

            private PickupContinuationPolicy(int miningGraceTicks, int activePickupTicks, double maxDistance) {
                this.miningGraceTicks = miningGraceTicks;
                this.activePickupTicks = activePickupTicks;
                maxDistanceSq = maxDistance * maxDistance;
            }

            private void armAfterMining(BlockPos origin) {
                miningOrigin = origin;
                miningGraceUntilTick = WorldHelper.getTicks() + miningGraceTicks;
                activePickupUntilTick = 0;
            }

            private void armActivePickup(ItemEntity drop) {
                if (isUsableDrop(drop)) {
                    activePickupUntilTick = WorldHelper.getTicks() + activePickupTicks;
                    activePickupPos = drop.getPos();
                }
            }

            private Optional<ItemEntity> getPreferredMinedDrop(Optional<ItemEntity> closestDrop) {
                pruneExpired();
                if (miningOrigin == null || closestDrop.isEmpty()) {
                    return Optional.empty();
                }

                ItemEntity drop = closestDrop.get();
                if (!isUsableDrop(drop)) {
                    return Optional.empty();
                }

                double distanceSq = drop.getPos().squaredDistanceTo(WorldHelper.toVec3d(miningOrigin));
                if (distanceSq > maxDistanceSq) {
                    return Optional.empty();
                }
                return Optional.of(drop);
            }

            private Optional<ItemEntity> getPreferredActivePickupDrop(Optional<ItemEntity> closestDrop, boolean pickupTaskContinuing, Vec3d playerPos) {
                pruneExpired();
                if (!pickupTaskContinuing || activePickupUntilTick <= WorldHelper.getTicks() || closestDrop.isEmpty()) {
                    return Optional.empty();
                }

                ItemEntity drop = closestDrop.get();
                if (!isUsableDrop(drop) || drop.getPos().squaredDistanceTo(playerPos) > maxDistanceSq) {
                    return Optional.empty();
                }
                return Optional.of(drop);
            }

            private boolean shouldContinueActivePickup(boolean pickupTaskContinuing) {
                pruneExpired();
                return pickupTaskContinuing && activePickupUntilTick > WorldHelper.getTicks();
            }

            private Vec3d activePickupPos(Vec3d fallback) {
                return activePickupPos == null ? fallback : activePickupPos;
            }

            private int miningGraceTicksRemaining() {
                return Math.max(0, miningGraceUntilTick - WorldHelper.getTicks());
            }

            private int activePickupTicksRemaining() {
                return Math.max(0, activePickupUntilTick - WorldHelper.getTicks());
            }

            private void pruneExpired() {
                int currentTick = WorldHelper.getTicks();
                if (miningOrigin != null && currentTick > miningGraceUntilTick) {
                    miningOrigin = null;
                    miningGraceUntilTick = 0;
                }
                if (activePickupUntilTick > 0 && currentTick > activePickupUntilTick) {
                    activePickupUntilTick = 0;
                    activePickupPos = null;
                }
            }

            private void reset() {
                miningOrigin = null;
                miningGraceUntilTick = 0;
                activePickupUntilTick = 0;
                activePickupPos = null;
            }

            private boolean isUsableDrop(ItemEntity drop) {
                return drop != null && drop.isAlive() && !drop.getStack().isEmpty();
            }
        }
    }

    private String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

}
