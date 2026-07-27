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

        private final Block[] _blocks;
        private final ItemTarget[] _targets;
        private final TemporaryBlockBlacklist temporaryBlockBlacklist = new TemporaryBlockBlacklist();
        private final MovementProgressChecker progressChecker = new MovementProgressChecker();
        private final Task _pickupTask;
        private BlockPos miningPos;
        private int miningTargetStartTick;
        private final StateChangeLogger debugLogger = new StateChangeLogger("MineOrCollectTask");

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
            throw new UnsupportedOperationException("Shouldn't try to get the position of object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
        }

        @Override
        protected Optional<Object> getClosestTo(AltoClef mod, Vec3d pos) {
            temporaryBlockBlacklist.pruneExpired();

            Pair<Double, Optional<BlockPos>> closestBlock = getClosestBlock(mod,pos, this::isAllowedMiningCandidate, _blocks);
            Pair<Double, Optional<ItemEntity>> closestDrop = getClosestItemDrop(mod,pos,  _targets);

            double blockSq = closestBlock.getLeft();
            double dropSq = closestDrop.getLeft();

            // We can't mine right now.
            if (mod.getExtraBaritoneSettings().isInteractionPaused()) {
                debugLogger.state("interaction paused; prefer dropped item: drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                        + ", block=" + closestBlock.getRight().map(BlockPos::toShortString).orElse("none")
                        + ", skippedBlocks=" + temporaryBlockBlacklist.size());
                return closestDrop.getRight().map(Object.class::cast);
            }

            if (dropSq <= blockSq) {
                debugLogger.state("closest target is dropped item: drop=" + closestDrop.getRight().map(this::describeDrop).orElse("none")
                        + ", dropSq=" + formatDouble(dropSq)
                        + ", blockSq=" + formatDouble(blockSq)
                        + ", skippedBlocks=" + temporaryBlockBlacklist.size());
                return closestDrop.getRight().map(Object.class::cast);
            } else {
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
                    progressChecker.reset();
                    miningTargetStartTick = WorldHelper.getTicks();
                    debugLogger.state("new mining target: pos=" + newPos.toShortString()
                            + ", timeoutTicks=" + MINING_TARGET_TIMEOUT_TICKS);
                }
                miningPos = newPos;
                return new DestroyBlockTask(miningPos);
            }
            if (obj instanceof ItemEntity) {
                debugLogger.state("pickup target selected: " + describeDrop((ItemEntity) obj));
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
            return false;
        }

        @Override
        protected void onStart() {
            progressChecker.reset();
            miningPos = null;
            miningTargetStartTick = 0;
            temporaryBlockBlacklist.pruneExpired();
            debugLogger.event("start: blocks=" + Arrays.toString(_blocks)
                    + ", targets=" + Arrays.toString(_targets));
        }

        @Override
        protected void onStop(Task interruptTask) {
            debugLogger.event("stop: interruptedBy=" + (interruptTask == null ? "none" : interruptTask.getClass().getSimpleName())
                    + ", miningPos=" + describePos(miningPos));
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

        private void temporarilySkipMiningTarget(AltoClef mod, String reason) {
            if (miningPos == null) {
                return;
            }

            BlockPos skipped = miningPos;
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

        private boolean isAllowedMiningCandidate(BlockPos pos) {
            return !temporaryBlockBlacklist.contains(pos);
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
    }

    private String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

}
