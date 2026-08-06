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
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.time.TimerGame;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.BaritonePathDiagnosticSnapshot;
import lavi.minecraft.diagnostics.mining.MiningPathDiagnostics;
import lavi.minecraft.diagnostics.tasktrace.VisibleTaskDiagnostics;
import lavi.minecraft.integration.mining.MiningToolReadiness;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
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

public class MineAndCollectTask extends ResourceTask {

    private final Block[] _blocksToMine;

    private final MiningRequirement _requirement;

    private final TimerGame _cursorStackTimer = new TimerGame(3);

    private final MineOrCollectTask _subtask;

    public MineAndCollectTask(ItemTarget[] itemTargets, Block[] blocksToMine, MiningRequirement requirement) {
        super(itemTargets);
        _requirement = requirement;
        _blocksToMine = blocksToMine;
        _subtask = new MineOrCollectTask(_blocksToMine, this.itemTargets, _requirement);
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
        VisibleTaskDiagnostics.logLifecycle(mod, this, "START", "mine_and_collect_start",
                "blocksToMine", Arrays.toString(_blocksToMine),
                "itemTargets", Arrays.toString(itemTargets),
                "miningRequirement", _requirement);
        mod.getBehaviour().push();

        // We're mining, so don't throw away pickaxes.
        mod.getBehaviour().addProtectedItems(Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.IRON_PICKAXE, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);

        _subtask.resetSearch();
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        // Picking up is controlled by a separate task here.
        return true;
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        if (!StorageHelper.miningRequirementMet(_requirement)) {
            Task requirementTask = new SatisfyMiningRequirementTask(_requirement);
            VisibleTaskDiagnostics.logReturnTask(mod, this, requirementTask, "mine_and_collect_return_requirement_task",
                    "requirement=" + _requirement,
                    "miningRequirement", _requirement,
                    "currentMiningRequirement", ChatClefDiagnostics.safeValue(StorageHelper::getCurrentMiningRequirement));
            return requirementTask;
        }

        if (_subtask.isMining()) {
            VisibleTaskDiagnostics.logDecision(mod, this, "mine_and_collect_subtask_is_mining",
                    "miningPos=" + ChatClefDiagnostics.blockPos(_subtask.miningPos()),
                    "miningPos", ChatClefDiagnostics.blockPos(_subtask.miningPos()));
            makeSureToolIsEquipped(mod);
        }

        // Wrong dimension check.
        if (_subtask.wasWandering() && isInWrongDimension(mod) && !mod.getBlockScanner().anyFound(_blocksToMine)) {
            Task dimensionTask = getToCorrectDimensionTask(mod);
            VisibleTaskDiagnostics.logReturnTask(mod, this, dimensionTask, "mine_and_collect_return_correct_dimension_task",
                    "blocksToMine=" + Arrays.toString(_blocksToMine),
                    "blocksToMine", Arrays.toString(_blocksToMine));
            return dimensionTask;
        }

        VisibleTaskDiagnostics.logReturnTask(mod, this, _subtask, "mine_and_collect_return_mine_or_collect_task",
                "mining=" + _subtask.isMining() + "|miningPos=" + ChatClefDiagnostics.blockPos(_subtask.miningPos()),
                "blocksToMine", Arrays.toString(_blocksToMine),
                "itemTargets", Arrays.toString(itemTargets),
                "subtaskMining", _subtask.isMining(),
                "subtaskMiningPos", ChatClefDiagnostics.blockPos(_subtask.miningPos()));
        return _subtask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        VisibleTaskDiagnostics.logLifecycle(mod, this, "STOP", "mine_and_collect_stop",
                "blocksToMine", Arrays.toString(_blocksToMine),
                "itemTargets", Arrays.toString(itemTargets),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
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
                                VisibleTaskDiagnostics.logDecision(mod, this, "mine_and_collect_cursor_tool_equip_slot",
                                        "cursorItem=" + item + "|miningPos=" + ChatClefDiagnostics.blockPos(_subtask.miningPos()),
                                        "cursorStack", ChatClefDiagnostics.itemStackSummary(cursorStack),
                                        "currentlyEquipped", currentlyEquipped,
                                        "swapMiningLevel", ToolMaterialVer.getMiningLevel(swapPick),
                                        "currentMiningLevel", ToolMaterialVer.getMiningLevel(currentPick),
                                        "miningPos", ChatClefDiagnostics.blockPos(_subtask.miningPos()));
                                mod.getSlotHandler().forceEquipSlot(CursorSlot.SLOT);
                            }
                        } else {
                            // We're not equipped with a pickaxe...
                            VisibleTaskDiagnostics.logDecision(mod, this, "mine_and_collect_cursor_tool_equip_slot",
                                    "cursorItem=" + item + "|miningPos=" + ChatClefDiagnostics.blockPos(_subtask.miningPos()),
                                    "cursorStack", ChatClefDiagnostics.itemStackSummary(cursorStack),
                                    "currentlyEquipped", currentlyEquipped,
                                    "miningPos", ChatClefDiagnostics.blockPos(_subtask.miningPos()));
                            mod.getSlotHandler().forceEquipSlot(CursorSlot.SLOT);
                        }
                    }
                }
            }
            _cursorStackTimer.reset();
        }
    }

    public static class MineOrCollectTask extends AbstractDoToClosestObjectTask<Object> {

        private final Block[] _blocks;
        private final ItemTarget[] _targets;
        private final MiningRequirement _requirement;
        private final Set<BlockPos> blacklist = new HashSet<>();
        private final MovementProgressChecker progressChecker = new MovementProgressChecker();
        private final Task _pickupTask;
        private BlockPos miningPos;

        public MineOrCollectTask(Block[] blocks, ItemTarget[] targets) {
            this(blocks, targets, MiningRequirement.HAND);
        }

        public MineOrCollectTask(Block[] blocks, ItemTarget[] targets, MiningRequirement requirement) {
            _blocks = blocks;
            _targets = targets;
            _requirement = requirement;
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
            Pair<Double, Optional<BlockPos>> closestBlock = getClosestBlock(mod,pos,  _blocks);
            Pair<Double, Optional<ItemEntity>> closestDrop = getClosestItemDrop(mod,pos,  _targets);

            double blockSq = closestBlock.getLeft();
            double dropSq = closestDrop.getLeft();

            // We can't mine right now.
            if (mod.getExtraBaritoneSettings().isInteractionPaused()) {
                Optional<Object> result = closestDrop.getRight().map(Object.class::cast);
                MiningPathDiagnostics.logMineTargetSelection(mod, this, closestBlock, closestDrop, result,
                        "INTERACTION_PAUSED_DROP_ONLY", selectedTargetChanged(result), localBlacklistContains(result),
                        _blocks, blacklist.size(), miningPos);
                VisibleTaskDiagnostics.logDecision(mod, this, "mine_or_collect_closest_choice",
                        "choice=drop_interaction_paused|drop=" + result.map(Object::toString).orElse("none"),
                        "choice", "drop_interaction_paused",
                        "closestBlockDistanceSq", blockSq,
                        "closestBlock", closestBlock.getRight().map(ChatClefDiagnostics::blockPos).orElse("none"),
                        "closestDropDistanceSq", dropSq,
                        "closestDrop", closestDrop.getRight().map(ChatClefDiagnostics::entitySummary).orElse("none"),
                        "resultPresent", result.isPresent());
                return result;
            }

            if (dropSq <= blockSq) {
                Optional<Object> result = closestDrop.getRight().map(Object.class::cast);
                MiningPathDiagnostics.logMineTargetSelection(mod, this, closestBlock, closestDrop, result,
                        result.isPresent() ? "DROP_CLOSER" : "NO_CANDIDATE", selectedTargetChanged(result), localBlacklistContains(result),
                        _blocks, blacklist.size(), miningPos);
                VisibleTaskDiagnostics.logDecision(mod, this, "mine_or_collect_closest_choice",
                        "choice=drop|drop=" + result.map(Object::toString).orElse("none"),
                        "choice", "drop",
                        "closestBlockDistanceSq", blockSq,
                        "closestBlock", closestBlock.getRight().map(ChatClefDiagnostics::blockPos).orElse("none"),
                        "closestDropDistanceSq", dropSq,
                        "closestDrop", closestDrop.getRight().map(ChatClefDiagnostics::entitySummary).orElse("none"),
                        "resultPresent", result.isPresent());
                return result;
            } else {
                Optional<Object> result = closestBlock.getRight().map(Object.class::cast);
                MiningPathDiagnostics.logMineTargetSelection(mod, this, closestBlock, closestDrop, result,
                        result.isPresent() ? "NEW_CLOSEST_NOT_IN_HEURISTIC_CACHE" : "NO_CANDIDATE", selectedTargetChanged(result), localBlacklistContains(result),
                        _blocks, blacklist.size(), miningPos);
                VisibleTaskDiagnostics.logDecision(mod, this, "mine_or_collect_closest_choice",
                        "choice=block|block=" + result.map(Object::toString).orElse("none"),
                        "choice", "block",
                        "closestBlockDistanceSq", blockSq,
                        "closestBlock", closestBlock.getRight().map(ChatClefDiagnostics::blockPos).orElse("none"),
                        "closestDropDistanceSq", dropSq,
                        "closestDrop", closestDrop.getRight().map(ChatClefDiagnostics::entitySummary).orElse("none"),
                        "resultPresent", result.isPresent());
                return result;
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
            Optional<BlockPos> closestBlock = mod.getBlockScanner().getNearestBlock(pos, check -> {

                if (mod.getBlockScanner().isUnreachable(check)) return false;
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
            boolean progressCheckEvaluated = miningPos != null;
            boolean progressCheckResult = !progressCheckEvaluated || progressChecker.check(mod);
            if (progressCheckEvaluated) {
                MiningPathDiagnostics.logMovementProgressCheckResult(
                        mod,
                        this,
                        miningPos,
                        "MINE_OR_COLLECT",
                        1,
                        true,
                        progressCheckResult,
                        progressCheckResult ? "PASS" : "PROGRESS_FAILURE",
                        false,
                        "not_destroy_move_checker",
                        false,
                        "not_destroy_stuck_checker",
                        false,
                        "not_destroy_second_move_checker");
            }
            if (miningPos != null && !progressCheckResult) {
                VisibleTaskDiagnostics.logProgress(mod, this, "mine_or_collect_progress_failed_blacklist",
                        "miningPos=" + ChatClefDiagnostics.blockPos(miningPos),
                        "miningPos", ChatClefDiagnostics.blockPos(miningPos),
                        "blacklistSizeBefore", blacklist.size());
                BaritonePathDiagnosticSnapshot cancelBefore = MiningPathDiagnostics.captureBaritoneSnapshot(mod, miningPos, null, "unavailable");
                mod.getClientBaritone().getPathingBehavior().forceCancel();
                MiningPathDiagnostics.logExistingCancelBoundary(mod, this, miningPos, "MINE_OR_COLLECT_PROGRESS_FAILURE", cancelBefore);
                Debug.logMessage("Failed to mine block. Suggesting it may be unreachable.");
                MiningPathDiagnostics.logBlockUnreachableRequest(mod, this, miningPos, 2, "MINE_OR_COLLECT_PROGRESS_FAILURE", null, null);
                mod.getBlockScanner().requestBlockUnreachable(miningPos, 2);
                blacklist.add(miningPos);
                miningPos = null;
                progressChecker.reset();
            }
            return super.onTick();
        }

        @Override
        protected Task getGoalTask(Object obj) {
            if (obj instanceof BlockPos newPos) {
                AltoClef mod = AltoClef.getInstance();
                BlockState targetState = mod.getWorld().getBlockState(newPos);
                MiningToolReadiness.Readiness readiness = MiningToolReadiness.evaluate(mod, targetState, _requirement);
                //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
                // Target-aware readiness keeps saved low-durability pickaxes protected and acquires a usable tool before mining.
                if (readiness.requiresAcquisition()) {
                    miningPos = null;
                    progressChecker.reset();
                    Task requirementTask = new SatisfyMiningRequirementTask(_requirement, targetState);
                    VisibleTaskDiagnostics.logReturnTask(mod, this, requirementTask, "mine_or_collect_return_target_tool_requirement_task",
                            "targetPosition=" + ChatClefDiagnostics.blockPos(newPos) + "|requirement=" + _requirement,
                            "targetPosition", ChatClefDiagnostics.blockPos(newPos),
                            "targetBlockState", targetState,
                            "miningRequirement", _requirement,
                            "broadRequirementMet", readiness.broadRequirementMet(),
                            "selectableToolPresent", readiness.selectableToolPresent(),
                            "rejectedBySavePolicy", readiness.rejectedBySavePolicy(),
                            "miningPos", ChatClefDiagnostics.blockPos(miningPos));
                    return requirementTask;
                }
                if (miningPos == null || !miningPos.equals(newPos)) {
                    progressChecker.reset();
                }
                miningPos = newPos;
                Task destroyTask = new DestroyBlockTask(miningPos);
                VisibleTaskDiagnostics.logReturnTask(mod, this, destroyTask, "mine_or_collect_return_destroy_block_task",
                        "miningPos=" + ChatClefDiagnostics.blockPos(miningPos),
                        "miningPos", ChatClefDiagnostics.blockPos(miningPos),
                        "targetBlockState", targetState);
                return destroyTask;
            }
            if (obj instanceof ItemEntity) {
                miningPos = null;
                VisibleTaskDiagnostics.logReturnTask(AltoClef.getInstance(), this, _pickupTask, "mine_or_collect_return_pickup_task",
                        "drop=" + obj,
                        "drop", ChatClefDiagnostics.entitySummary((ItemEntity) obj));
                return _pickupTask;
            }
            VisibleTaskDiagnostics.logDecision(AltoClef.getInstance(), this, "mine_or_collect_unsupported_goal_object",
                    "object=" + obj,
                    "objectClass", obj == null ? "null" : obj.getClass(),
                    "object", obj);
            throw new UnsupportedOperationException("Shouldn't try to get the goal from object " + obj + " of type " + (obj != null ? obj.getClass().toString() : "(null object)"));
        }

        @Override
        protected boolean isValid(AltoClef mod, Object obj) {
            if (obj instanceof BlockPos b) {
                return mod.getBlockScanner().isBlockAtPosition(b, _blocks) && WorldHelper.canBreak(b);
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
            VisibleTaskDiagnostics.logLifecycle(AltoClef.getInstance(), this, "START", "mine_or_collect_start",
                    "blocks", Arrays.toString(_blocks),
                    "targets", Arrays.toString(_targets));
            progressChecker.reset();
            miningPos = null;
        }

        @Override
        protected void onStop(Task interruptTask) {
            VisibleTaskDiagnostics.logLifecycle(AltoClef.getInstance(), this, "STOP", "mine_or_collect_stop",
                    "blocks", Arrays.toString(_blocks),
                    "targets", Arrays.toString(_targets),
                    "miningPos", ChatClefDiagnostics.blockPos(miningPos),
                    "blacklistSize", blacklist.size(),
                    "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
        }

        @Override
        protected boolean isEqual(Task other) {
            if (other instanceof MineOrCollectTask task) {
                return Arrays.equals(task._blocks, _blocks) && Arrays.equals(task._targets, _targets) && task._requirement == _requirement;
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

        private boolean selectedTargetChanged(Optional<Object> selected) {
            if (selected.isEmpty()) {
                return miningPos != null;
            }
            Object value = selected.get();
            if (value instanceof BlockPos blockPos) {
                return miningPos == null || !miningPos.equals(blockPos);
            }
            return miningPos != null;
        }

        private boolean localBlacklistContains(Optional<Object> selected) {
            return selected
                    .filter(BlockPos.class::isInstance)
                    .map(BlockPos.class::cast)
                    .map(blacklist::contains)
                    .orElse(false);
        }
    }

}
