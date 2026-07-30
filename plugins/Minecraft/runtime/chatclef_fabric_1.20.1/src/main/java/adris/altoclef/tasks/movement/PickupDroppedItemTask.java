package adris.altoclef.tasks.movement;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasks.AbstractDoToClosestObjectTask;
import adris.altoclef.tasks.resources.SatisfyMiningRequirementTask;
import adris.altoclef.tasks.slot.EnsureFreeInventorySlotTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import net.minecraft.block.*;
import adris.altoclef.multiversion.versionedfields.Blocks;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PickupDroppedItemTask extends AbstractDoToClosestObjectTask<ItemEntity> implements ITaskRequiresGrounded {
    private static final Task getPickaxeFirstTask = new SatisfyMiningRequirementTask(MiningRequirement.STONE);
    // Not clean practice, but it helps keep things self contained I think.
    private static boolean isGettingPickaxeFirstFlag = false;
    private final TimeoutWanderTask wanderTask = new TimeoutWanderTask(5, true);
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker progressChecker = new MovementProgressChecker();
    private final ItemTarget[] itemTargets;

    // This happens all the time in mineshafts and swamps/jungles
    private final Set<ItemEntity> _blacklist = new HashSet<>();
    private final boolean _freeInventoryIfFull;
    Block[] annoyingBlocks = new Block[]{
            Blocks.VINE,
            Blocks.NETHER_SPROUTS,
            Blocks.CAVE_VINES,
            Blocks.CAVE_VINES_PLANT,
            Blocks.TWISTING_VINES,
            Blocks.TWISTING_VINES_PLANT,
            Blocks.WEEPING_VINES_PLANT,
            Blocks.LADDER,
            Blocks.BIG_DRIPLEAF,
            Blocks.BIG_DRIPLEAF_STEM,
            Blocks.SMALL_DRIPLEAF,
            Blocks.TALL_GRASS,
            Blocks.SHORT_GRASS
    };
    private Task unstuckTask = null;
    // Am starting to regret not making this a singleton
    private AltoClef _mod;
    private boolean _collectingPickaxeForThisResource = false;
    private ItemEntity _currentDrop = null;

    public PickupDroppedItemTask(ItemTarget[] itemTargets, boolean freeInventoryIfFull) {
        this.itemTargets = itemTargets;
        _freeInventoryIfFull = freeInventoryIfFull;
    }

    public PickupDroppedItemTask(ItemTarget target, boolean freeInventoryIfFull) {
        this(new ItemTarget[]{target}, freeInventoryIfFull);
    }

    public PickupDroppedItemTask(Item item, int targetCount, boolean freeInventoryIfFull) {
        this(new ItemTarget(item, targetCount), freeInventoryIfFull);
    }

    public PickupDroppedItemTask(Item item, int targetCount) {
        this(item, targetCount, true);
    }

    private static BlockPos[] generateSides(BlockPos pos) {
        return new BlockPos[]{
                pos.add(1,0,0),
                pos.add(-1,0,0),
                pos.add(0,0,1),
                pos.add(0,0,-1),
                pos.add(1,0,-1),
                pos.add(1,0,1),
                pos.add(-1,0,-1),
                pos.add(-1,0,1)
        };
    }

    public static boolean isIsGettingPickaxeFirst(AltoClef mod) {
        return isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst();
    }

    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        if (annoyingBlocks != null) {
            for (Block AnnoyingBlocks : annoyingBlocks) {
                return mod.getWorld().getBlockState(pos).getBlock() == AnnoyingBlocks ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock ||
                        mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
            }
        }
        return false;
    }

    private BlockPos stuckInBlock(AltoClef mod) {
        BlockPos p = mod.getPlayer().getBlockPos();
        if (isAnnoying(mod, p)) return p;
        if (isAnnoying(mod, p.up())) return p.up();
        BlockPos[] toCheck = generateSides(p);
        for (BlockPos check : toCheck) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        BlockPos[] toCheckHigh = generateSides(p.up());
        for (BlockPos check : toCheckHigh) {
            if (isAnnoying(mod, check)) {
                return check;
            }
        }
        return null;
    }

    private Task getFenceUnstuckTask() {
        return new SafeRandomShimmyTask();
    }

    public boolean isCollectingPickaxeForThis() {
        return _collectingPickaxeForThisResource;
    }

    @Override
    protected void onStart() {
        //20260730_kpopmodder: Diagnostics-only LAVI log for dropped-item pickup loop investigation; no behavior change.
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "ON_START", "pickup_dropped_item_start", this,
                "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets),
                "freeInventoryIfFull", _freeInventoryIfFull,
                "blacklistSize", _blacklist.size(),
                "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop));
        wanderTask.reset();
        progressChecker.reset();
        stuckCheck.reset();
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "ON_STOP", "pickup_dropped_item_stop", this,
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask),
                "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets),
                "blacklistSize", _blacklist.size(),
                "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop));
    }

    @Override
    protected Task onTick() {
        if (wanderTask.isActive() && !wanderTask.isFinished()) {
            ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "continue_pickup_wander_task", this,
                    "wanderTask", ChatClefDiagnostics.taskSummary(wanderTask),
                    "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop));
            setDebugState("Wandering.");
            return wanderTask;
        }
        AltoClef mod = AltoClef.getInstance();

        boolean pathing = mod.getClientBaritone().getPathingBehavior().isPathing();
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "ON_TICK", "pickup_dropped_item_tick_begin", this,
                "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets),
                "playerPosition", ChatClefDiagnostics.playerPosition(mod),
                "pathing", pathing,
                "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop),
                "blacklistSize", _blacklist.size(),
                "unstuckTask", ChatClefDiagnostics.taskSummary(unstuckTask),
                "collectingPickaxeForThisResource", _collectingPickaxeForThisResource,
                "globalGettingPickaxeFirst", isGettingPickaxeFirstFlag);

        if (pathing) {
            ChatClefDiagnostics.logEvent("ITEM_PICKUP", "OBSERVE", "pathing_active_resets_progress", this);
            progressChecker.reset();
        }
        BlockPos activeUnstuckBlock = null;
        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished()) {
            activeUnstuckBlock = stuckInBlock(mod);
        }
        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished() && activeUnstuckBlock != null) {
            setDebugState("Getting unstuck from block.");
            ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "continue_unstuck_task", this,
                    "stuckBlock", ChatClefDiagnostics.blockPos(activeUnstuckBlock),
                    "unstuckTask", ChatClefDiagnostics.taskSummary(unstuckTask),
                    "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop));
            stuckCheck.reset();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            return unstuckTask;
        }
        boolean progressOk = progressChecker.check(mod);
        boolean stuckOk = true;
        if (progressOk) {
            stuckOk = stuckCheck.check(mod);
        }
        if (!progressOk || !stuckOk) {
            BlockPos blockStuck = stuckInBlock(mod);
            ChatClefDiagnostics.logEvent("ITEM_PICKUP", "OBSERVE", "pickup_progress_failed", this,
                    "progressOk", progressOk,
                    "stuckOk", stuckOk,
                    "stuckBlock", ChatClefDiagnostics.blockPos(blockStuck),
                    "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop));
            if (blockStuck != null) {
                unstuckTask = getFenceUnstuckTask();
                ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "start_unstuck_task", this,
                        "stuckBlock", ChatClefDiagnostics.blockPos(blockStuck),
                        "unstuckTask", ChatClefDiagnostics.taskSummary(unstuckTask));
                return unstuckTask;
            }
            stuckCheck.reset();
        }
        _mod = mod;

        // If we're getting a pickaxe for THIS resource...
        if (isIsGettingPickaxeFirst(mod) && _collectingPickaxeForThisResource && !StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)) {
            progressChecker.reset();
            setDebugState("Collecting pickaxe first");
            ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "collect_pickaxe_first", this,
                    "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop),
                    "pickaxeTask", ChatClefDiagnostics.taskSummary(getPickaxeFirstTask));
            return getPickaxeFirstTask;
        } else {
            if (StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)) {
                isGettingPickaxeFirstFlag = false;
            }
            _collectingPickaxeForThisResource = false;
        }

        boolean progressOkAfterInventory = progressChecker.check(mod);
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "OBSERVE", "post_inventory_progress_check", this,
                "progressOk", progressOkAfterInventory,
                "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop));
        if (!progressOkAfterInventory) {
            mod.getClientBaritone().getPathingBehavior().forceCancel();
            if (_currentDrop != null && !_currentDrop.getStack().isEmpty()) {
                // We might want to get a pickaxe first.
                if (!isGettingPickaxeFirstFlag && mod.getModSettings().shouldCollectPickaxeFirst() && !StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)) {
                    Debug.logMessage("Failed to pick up drop, will try to collect a stone pickaxe first and try again!");
                    _collectingPickaxeForThisResource = true;
                    isGettingPickaxeFirstFlag = true;
                    ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "failed_pickup_collect_pickaxe_first", this,
                            "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop),
                            "pickaxeTask", ChatClefDiagnostics.taskSummary(getPickaxeFirstTask));
                    return getPickaxeFirstTask;
                }
                Debug.logMessage(StlHelper.toString(_blacklist, element -> element == null ? "(null)" : element.getStack().getItem().getTranslationKey()));
                Debug.logMessage("Failed to pick up drop, suggesting it's unreachable.");
                _blacklist.add(_currentDrop);
                mod.getEntityTracker().requestEntityUnreachable(_currentDrop);
                ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "blacklist_unreachable_drop", this,
                        "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop),
                        "blacklistSize", _blacklist.size(),
                        "wanderTask", ChatClefDiagnostics.taskSummary(wanderTask));
                return wanderTask;
            }
        }

        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "delegate_to_closest_object_task", this,
                "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop),
                "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets));
        return super.onTick();
    }


    @Override
    protected boolean isEqual(Task other) {
        // Same target items
        if (other instanceof PickupDroppedItemTask task) {
            return Arrays.equals(task.itemTargets, itemTargets) && task._freeInventoryIfFull == _freeInventoryIfFull;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        StringBuilder result = new StringBuilder();
        result.append("Pickup Dropped Items: [");
        int c = 0;
        for (ItemTarget target : itemTargets) {
            result.append(target.toString());
            if (++c != itemTargets.length) {
                result.append(", ");
            }
        }
        result.append("]");
        return result.toString();
    }

    @Override
    protected Vec3d getPos(AltoClef mod, ItemEntity obj) {
        if (!obj.isOnGround() && !obj.isTouchingWater()) {
            // Assume we'll land down one or two blocks from here. We could do this more advanced but whatever.
            BlockPos p = obj.getBlockPos();
            if (!WorldHelper.isSolidBlock(p.down(3))) {
                return obj.getPos().subtract(0, 2, 0);
            }
            return obj.getPos().subtract(0, 1, 0);
        }
        return obj.getPos();
    }

    @Override
    protected Optional<ItemEntity> getClosestTo(AltoClef mod, Vec3d pos) {
        Optional<ItemEntity> closest = mod.getEntityTracker().getClosestItemDrop(
                pos,
                itemTargets);
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "OBSERVE", "closest_drop_result", this,
                "origin", ChatClefDiagnostics.vec3d(pos),
                "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets),
                "closestPresent", closest.isPresent(),
                "closestDrop", closest.map(ChatClefDiagnostics::entitySummary).orElse("none"),
                "closestDistanceSqr", closest.map(drop -> ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, drop)).orElse("unavailable"));
        return closest;
    }

    @Override
    protected Vec3d getOriginPos(AltoClef mod) {
        return mod.getPlayer().getPos();
    }

    @Override
    protected Task getGoalTask(ItemEntity itemEntity) {
        if (!itemEntity.equals(_currentDrop)) {
            ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "switch_current_drop", this,
                    "previousDrop", ChatClefDiagnostics.entitySummary(_currentDrop),
                    "nextDrop", ChatClefDiagnostics.entitySummary(itemEntity),
                    "nextDropStack", ChatClefDiagnostics.itemStackSummary(itemEntity.getStack()));
            _currentDrop = itemEntity;
            progressChecker.reset();
            if (isGettingPickaxeFirstFlag && _collectingPickaxeForThisResource) {
                Debug.logMessage("New goal, no longer collecting a pickaxe.");
                _collectingPickaxeForThisResource = false;
                isGettingPickaxeFirstFlag = false;
                ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "cancel_pickaxe_first_for_new_drop", this,
                        "currentDrop", ChatClefDiagnostics.entitySummary(_currentDrop));
            }
        }
        // Ensure our inventory is free if we're close
        boolean touching = _mod.getEntityTracker().isCollidingWithPlayer(itemEntity);
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "OBSERVE", "drop_goal_inventory_check", this,
                "drop", ChatClefDiagnostics.entitySummary(itemEntity),
                "dropStack", ChatClefDiagnostics.itemStackSummary(itemEntity.getStack()),
                "touchingPlayer", touching,
                "freeInventoryIfFull", _freeInventoryIfFull);
        if (touching) {
            if (_freeInventoryIfFull) {
                if (_mod.getItemStorage().getSlotsThatCanFitInPlayerInventory(itemEntity.getStack(), false).isEmpty()) {
                    ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "ensure_free_inventory_slot", this,
                            "drop", ChatClefDiagnostics.entitySummary(itemEntity));
                    return new EnsureFreeInventorySlotTask();
                }
            }
        }
        Task getToEntityTask = new GetToEntityTask(itemEntity);
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "DECISION", "return_get_to_drop_task", this,
                "drop", ChatClefDiagnostics.entitySummary(itemEntity),
                "getToEntityTask", ChatClefDiagnostics.taskSummary(getToEntityTask));
        return getToEntityTask;
    }

    @Override
    protected boolean isValid(AltoClef mod, ItemEntity obj) {
        boolean alive = obj.isAlive();
        boolean blacklisted = _blacklist.contains(obj);
        boolean valid = alive && !blacklisted;
        ChatClefDiagnostics.logEvent("ITEM_PICKUP", "OBSERVE", "drop_validity_check", this,
                "drop", ChatClefDiagnostics.entitySummary(obj),
                "dropStack", ChatClefDiagnostics.itemStackSummary(obj.getStack()),
                "alive", alive,
                "blacklisted", blacklisted,
                "valid", valid);
        return valid;
    }

}
