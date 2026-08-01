package adris.altoclef.tasks.construction;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.multiversion.ToolMaterialVer;
import adris.altoclef.tasks.movement.RunAwayFromPositionTask;
import adris.altoclef.tasks.movement.SafeRandomShimmyTask;
import adris.altoclef.tasksystem.ITaskRequiresGrounded;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.tasktrace.VisibleTaskDiagnostics;
import net.minecraft.block.*;
import adris.altoclef.multiversion.versionedfields.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

/**
 * Destroy a block at a position.
 */
public class DestroyBlockTask extends Task implements ITaskRequiresGrounded {
    private final MovementProgressChecker stuckCheck = new MovementProgressChecker();
    private final MovementProgressChecker _moveChecker = new MovementProgressChecker();
    private final BlockPos pos;
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
            Blocks.SHORT_GRASS,
            Blocks.SWEET_BERRY_BUSH
    };
    private Task unstuckTask = null;
    private boolean isMining;

    public DestroyBlockTask(BlockPos pos) {
        this.pos = pos;
    }

    /**
     * Generates an array of BlockPos objects representing the sides of a given BlockPos.
     *
     * @param pos The BlockPos object to generate the sides for.
     * @return An array of BlockPos objects representing the sides of the given BlockPos.
     */
    private static BlockPos[] generateSides(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // Log the values of x, y, and z for debugging
        Debug.logInternal("x = " + x);
        Debug.logInternal("y = " + y);
        Debug.logInternal("z = " + z);

        return new BlockPos[]{
                new BlockPos(x + 1, y, z),
                new BlockPos(x - 1, y, z),
                new BlockPos(x, y, z + 1),
                new BlockPos(x, y, z - 1),
                new BlockPos(x + 1, y, z - 1),
                new BlockPos(x + 1, y, z + 1),
                new BlockPos(x - 1, y, z - 1),
                new BlockPos(x - 1, y, z + 1)
        };
    }

    /**
     * Checks if a block is annoying.
     *
     * @param mod The AltoClef mod instance.
     * @param pos The position of the block.
     * @return true if the block is annoying, false otherwise.
     */
    private boolean isAnnoying(AltoClef mod, BlockPos pos) {
        for (Block annoyingBlock : annoyingBlocks) {
            boolean isAnnoying = mod.getWorld().getBlockState(pos).getBlock() == annoyingBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof DoorBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FenceGateBlock
                    || mod.getWorld().getBlockState(pos).getBlock() instanceof FlowerBlock;
            if (isAnnoying) {
                Debug.logInternal("Block at position " + pos + " is annoying.");
                return true;
            }
        }
        Debug.logInternal("Block at position " + pos + " is not annoying.");
        return false;
    }

    /**
     * Returns the position of the block where the player is stuck.
     * If there are no annoying block positions, returns null.
     *
     * @param mod The instance of the AltoClef mod.
     * @return The BlockPos of the stuck block, or null if none found.
     */
    private BlockPos stuckInBlock(AltoClef mod) {
        BlockPos playerPos = mod.getPlayer().getBlockPos();
        BlockPos[] toCheck = generateSides(playerPos);
        BlockPos[] toCheckHigh = generateSides(playerPos.up());

        // Check if player position is annoying
        if (isAnnoying(mod, playerPos)) {
            Debug.logInternal("Player position is annoying: " + playerPos);
            return playerPos;
        }

        // Check if player position (up) is annoying
        if (isAnnoying(mod, playerPos.up())) {
            Debug.logInternal("Player position (up) is annoying: " + playerPos.up());
            return playerPos.up();
        }

        // Check each side block position
        for (BlockPos check : toCheck) {
            if (isAnnoying(mod, check)) {
                Debug.logInternal("Block position is annoying: " + check);
                return check;
            }
        }

        // Check each high block position
        for (BlockPos check : toCheckHigh) {
            if (isAnnoying(mod, check)) {
                Debug.logInternal("Block position (up) is annoying: " + check);
                return check;
            }
        }

        Debug.logInternal("No annoying block positions found.");
        return null;
    }

    /**
     * Retrieves a task to get the fence unstuck.
     *
     * @return The task to get the fence unstuck.
     */
    private Task getFenceUnstuckTask() {
        // Log the start of the function
        Debug.logInternal("Entering getFenceUnstuckTask");

        // Create a safe random shimmy task
        Task task = createSafeRandomShimmyTask();

        // Log the end of the function
        Debug.logInternal("Exiting getFenceUnstuckTask");

        // Return the task
        return task;
    }

    /**
     * Creates a new instance of SafeRandomShimmyTask.
     *
     * @return The created SafeRandomShimmyTask.
     */
    private Task createSafeRandomShimmyTask() {
        Task task = new SafeRandomShimmyTask();
        Debug.logInternal("Created SafeRandomShimmyTask: " + task);
        return task;
    }

    /**
     * This method is called when the mod starts.
     * It cancels any ongoing pathing behavior, resets move checker and stuck check.
     * If the cursor stack is not empty, it tries to move it to a suitable slot in the player inventory.
     * If the item can be thrown away, it drops it in an undefined slot or the garbage slot.
     * If the cursor stack is empty, it closes the screen.
     */
    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();
        VisibleTaskDiagnostics.logLifecycle(mod, this, "START", "destroy_block_start",
                "targetPosition", ChatClefDiagnostics.blockPos(pos),
                "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));

        // Cancel any ongoing pathing behavior.
        mod.getClientBaritone().getPathingBehavior().forceCancel();

        // Reset move checker and stuck check.
        _moveChecker.reset();
        stuckCheck.reset();

        // Get the item stack in the cursor slot.
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        Debug.logInternal("Cursor stack: " + cursorStack);

        // If the cursor stack is not empty, try to move it to a suitable slot in the player inventory.
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            Debug.logInternal("Move to slot: " + moveTo);

            // If there is a slot where the item can fit, click on that slot to move the item.
            moveTo.ifPresent(slot -> {
                mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP);
                Debug.logInternal("Clicked slot: " + slot);
            });

            // If the item can be thrown away, click on an undefined slot to drop the item.
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                Debug.logInternal("Clicked undefined slot");
            }

            // Get the garbage slot and click on it to move the item.
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            Debug.logInternal("Garbage slot: " + garbage);

            garbage.ifPresent(slot -> {
                mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP);
                Debug.logInternal("Clicked slot: " + slot);
            });

            // Click on an undefined slot to drop the item.
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            Debug.logInternal("Clicked undefined slot");
        } else {
            // If the cursor stack is empty, close the screen.
            StorageHelper.closeScreen();
            Debug.logInternal("Closed screen");
        }
    }

    /**
     * This method is called periodically to perform various tasks.
     *
     * @return The next task to be executed.
     */
    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        // Check if there is white wool at the specified position
        if (mod.getWorld().getBlockState(pos).getBlock() == Blocks.WHITE_WOOL) {
            // Iterate over all entities in the world
            Iterable<Entity> entities = mod.getWorld().getEntities();
            for (Entity entity : entities) {
                // Check if the entity is a PillagerEntity and is within a distance of 144 blocks from the position
                if (entity instanceof PillagerEntity && pos.isWithinDistance(entity.getPos(), 144)) {
                    Debug.logMessage("Blacklisting pillager wool.");
                    VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_blacklist_pillager_wool",
                            "targetPosition=" + ChatClefDiagnostics.blockPos(pos) + "|entity=" + ChatClefDiagnostics.entitySummary(entity),
                            "targetPosition", ChatClefDiagnostics.blockPos(pos),
                            "entity", ChatClefDiagnostics.entitySummary(entity));
                    // Request the block at the position to be marked as unreachable
                    mod.getBlockScanner().requestBlockUnreachable(pos, 0);
                }
            }
        }

        // Reset the move checker if Baritone is currently pathing
        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            VisibleTaskDiagnostics.logProgress(mod, this, "destroy_block_pathing_active_reset_move_checker",
                    "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                    "targetPosition", ChatClefDiagnostics.blockPos(pos));
            _moveChecker.reset();
        }

        // Check if the player is in a Nether portal
        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_nether_portal_hold_escape_inputs",
                        "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                        "targetPosition", ChatClefDiagnostics.blockPos(pos));
                // Hold the sneak and move forward inputs to exit the Nether portal
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                return null;
            } else {
                VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_nether_portal_pathing_release_escape_inputs",
                        "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                        "targetPosition", ChatClefDiagnostics.blockPos(pos));
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_pathing_release_escape_inputs",
                    "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                    "targetPosition", ChatClefDiagnostics.blockPos(pos));
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.MOVE_BACK);
            mod.getInputControls().release(Input.MOVE_FORWARD);
        }

        // Check if there is an active unstuck task and the player is stuck in a block
        if (unstuckTask != null && unstuckTask.isActive() && !unstuckTask.isFinished()) {
            BlockPos activeStuckBlock = stuckInBlock(mod);
            if (activeStuckBlock != null) {
                setDebugState("Getting unstuck from block.");
                stuckCheck.reset();
                // Release control of Baritone's custom goal process and explore process
                mod.getClientBaritone().getCustomGoalProcess().onLostControl();
                mod.getClientBaritone().getExploreProcess().onLostControl();
                VisibleTaskDiagnostics.logReturnTask(mod, this, unstuckTask, "destroy_block_return_active_unstuck_task",
                        "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                        "targetPosition", ChatClefDiagnostics.blockPos(pos),
                        "stuckBlock", ChatClefDiagnostics.blockPos(activeStuckBlock));
                return unstuckTask;
            }
        }

        // Check if the move checker or the stuck check failed
        if (!_moveChecker.check(mod) || !stuckCheck.check(mod)) {
            BlockPos blockStuck = stuckInBlock(mod);
            VisibleTaskDiagnostics.logProgress(mod, this, "destroy_block_progress_check_failed",
                    "targetPosition=" + ChatClefDiagnostics.blockPos(pos) + "|stuckBlock=" + ChatClefDiagnostics.blockPos(blockStuck),
                    "targetPosition", ChatClefDiagnostics.blockPos(pos),
                    "stuckBlock", ChatClefDiagnostics.blockPos(blockStuck));
            if (blockStuck != null) {
                unstuckTask = getFenceUnstuckTask();
                VisibleTaskDiagnostics.logReturnTask(mod, this, unstuckTask, "destroy_block_return_new_unstuck_task",
                        "targetPosition=" + ChatClefDiagnostics.blockPos(pos) + "|stuckBlock=" + ChatClefDiagnostics.blockPos(blockStuck),
                        "targetPosition", ChatClefDiagnostics.blockPos(pos),
                        "stuckBlock", ChatClefDiagnostics.blockPos(blockStuck));
                return unstuckTask;
            }
            stuckCheck.reset();
        }

        // Check if the move checker failed
        if (!_moveChecker.check(mod)) {
            _moveChecker.reset();
            VisibleTaskDiagnostics.logProgress(mod, this, "destroy_block_move_checker_failed_request_unreachable",
                    "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                    "targetPosition", ChatClefDiagnostics.blockPos(pos),
                    "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)));
            // Request the block at the position to be marked as unreachable
            mod.getBlockScanner().requestBlockUnreachable(pos);
        }

        // Check if the block above the position is not solid, the player is above the position,
        // and the player is within a distance of 0.89 blocks from the position
        if (!WorldHelper.isSolidBlock(pos.up()) && mod.getPlayer().getPos().y > pos.getY() && pos.isWithinDistance(mod.getPlayer().isOnGround() ? mod.getPlayer().getPos() : mod.getPlayer().getPos().add(0, -1, 0), 0.89)) {
            if (WorldHelper.dangerousToBreakIfRightAbove(pos)) {
                setDebugState("It's dangerous to break as we're right above it, moving away and trying again.");
                Task runAwayTask = new RunAwayFromPositionTask(3, pos.getY(), pos);
                VisibleTaskDiagnostics.logReturnTask(mod, this, runAwayTask, "destroy_block_return_run_away_dangerous_break",
                        "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                        "targetPosition", ChatClefDiagnostics.blockPos(pos),
                        "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)));
                return runAwayTask;
            }
        }

        Optional<Rotation> reach = LookHelper.getReach(pos);
        if (reach.isPresent() && (mod.getPlayer().isTouchingWater() || mod.getPlayer().isOnGround()) && !mod.getFoodChain().needsToEat() && !WorldHelper.isInNetherPortal() && mod.getClientBaritone().getPathingBehavior().isSafeToCancel()) {
            setDebugState("Block in range, mining...");
            VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_in_range_mining",
                    "targetPosition=" + ChatClefDiagnostics.blockPos(pos)
                            + "|targetBlock=" + ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos).getBlock())
                            + "|mainHand=" + ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getMainHandStack().getItem()),
                    "targetPosition", ChatClefDiagnostics.blockPos(pos),
                    "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)),
                    "reach", reach.get(),
                    "playerTouchingWater", mod.getPlayer().isTouchingWater(),
                    "playerOnGround", mod.getPlayer().isOnGround(),
                    "foodChainNeedsToEat", mod.getFoodChain().needsToEat(),
                    "safeToCancel", mod.getClientBaritone().getPathingBehavior().isSafeToCancel());
            stuckCheck.reset();
            isMining = true;
            mod.getInputControls().release(Input.SNEAK);
            mod.getInputControls().release(Input.MOVE_BACK);
            mod.getInputControls().release(Input.MOVE_FORWARD);
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getBuilderProcess().onLostControl();
            if (!LookHelper.isLookingAt(mod, reach.get())) {
                LookHelper.lookAt(reach.get());
            }
            // Tool equip is handled in `PlayerInteractionFixChain`. Oof.
            //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
            // Diagnostics-only: prove harvest/tool state immediately before the existing CLICK_LEFT request.
            ChatClefDiagnostics.logEvent("DESTROY_BLOCK", "CLICK_LEFT_PRE", "before_click_left_force_state", this,
                    "targetPosition", ChatClefDiagnostics.blockPos(pos),
                    "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)),
                    "targetBlock", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos).getBlock()),
                    "targetRequiresTool", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos).isToolRequired()),
                    "targetMinimumMiningRequirement", ChatClefDiagnostics.safeValue(() -> MiningRequirement.getMinimumRequirementForBlock(mod.getWorld().getBlockState(pos).getBlock())),
                    "currentMiningRequirement", ChatClefDiagnostics.safeValue(StorageHelper::getCurrentMiningRequirement),
                    "stoneRequirementMet", ChatClefDiagnostics.safeValue(() -> StorageHelper.miningRequirementMet(MiningRequirement.STONE)),
                    "stoneRequirementMetInventory", ChatClefDiagnostics.safeValue(() -> StorageHelper.miningRequirementMetInventory(MiningRequirement.STONE)),
                    "mainHandSlot", ChatClefDiagnostics.safeValue(PlayerSlot::getEquipSlot),
                    "mainHandStack", ChatClefDiagnostics.safeValue(() -> ChatClefDiagnostics.itemStackSummary(StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()))),
                    "mainHandItem", ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem()),
                    "mainHandSuitableForTarget", ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem().getDefaultStack().isSuitableFor(mod.getWorld().getBlockState(pos))),
                    "mainHandMiningLevel", ChatClefDiagnostics.safeValue(() -> {
                        Item item = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
                        return item instanceof ToolItem tool ? ToolMaterialVer.getMiningLevel(tool) : "not_tool";
                    }),
                    "bestToolSlot", ChatClefDiagnostics.safeValue(() -> StorageHelper.getBestToolSlot(mod, mod.getWorld().getBlockState(pos)).map(Object::toString).orElse("none")),
                    "bestToolStack", ChatClefDiagnostics.safeValue(() -> StorageHelper.getBestToolSlot(mod, mod.getWorld().getBlockState(pos))
                            .map(slot -> ChatClefDiagnostics.itemStackSummary(StorageHelper.getItemStackInSlot(slot))).orElse("none")),
                    "bestToolItem", ChatClefDiagnostics.safeValue(() -> StorageHelper.getBestToolSlot(mod, mod.getWorld().getBlockState(pos))
                            .map(slot -> StorageHelper.getItemStackInSlot(slot).getItem()).orElse(null)),
                    "bestToolSuitableForTarget", ChatClefDiagnostics.safeValue(() -> StorageHelper.getBestToolSlot(mod, mod.getWorld().getBlockState(pos))
                            .map(slot -> StorageHelper.getItemStackInSlot(slot).getItem().getDefaultStack().isSuitableFor(mod.getWorld().getBlockState(pos))).orElse(false)),
                    "leftClickHeldBefore", ChatClefDiagnostics.safeValue(() -> mod.getInputControls().isHeldDown(Input.CLICK_LEFT)),
                    "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()),
                    "customGoalActive", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getCustomGoalProcess().isActive()),
                    "willRequestClickLeft", true);
            mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, true);
        } else {
            setDebugState("Getting to block...");
            VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_getting_to_block",
                    "targetPosition=" + ChatClefDiagnostics.blockPos(pos)
                            + "|reachPresent=" + reach.isPresent()
                            + "|isMining=" + isMining,
                    "targetPosition", ChatClefDiagnostics.blockPos(pos),
                    "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)),
                    "reachPresent", reach.isPresent(),
                    "playerTouchingWater", mod.getPlayer().isTouchingWater(),
                    "playerOnGround", mod.getPlayer().isOnGround(),
                    "foodChainNeedsToEat", mod.getFoodChain().needsToEat(),
                    "safeToCancel", mod.getClientBaritone().getPathingBehavior().isSafeToCancel());
            if (isMining && mod.getPlayer().isTouchingWater()) {
                setDebugState("We are in water... holding break button");
                isMining = false;
                VisibleTaskDiagnostics.logProgress(mod, this, "destroy_block_water_mining_request_unreachable",
                        "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                        "targetPosition", ChatClefDiagnostics.blockPos(pos));
                mod.getBlockScanner().requestBlockUnreachable(pos);
                mod.getInputControls().hold(Input.CLICK_LEFT);
            } else {
                isMining = false;
            }
            boolean isCloseToMoveBack = pos.isWithinDistance(mod.getPlayer().getPos(), 2);
            if (isCloseToMoveBack) {
                if (!mod.getClientBaritone().getPathingBehavior().isPathing() && !mod.getPlayer().isTouchingWater() &&
                        !mod.getFoodChain().needsToEat()) {
                    VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_close_hold_move_back_sneak",
                            "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                            "targetPosition", ChatClefDiagnostics.blockPos(pos),
                            "isCloseToMoveBack", true);
                    mod.getInputControls().hold(Input.MOVE_BACK);
                    mod.getInputControls().hold(Input.SNEAK);
                } else {
                    VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_close_release_move_back_sneak",
                            "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                            "targetPosition", ChatClefDiagnostics.blockPos(pos),
                            "isCloseToMoveBack", true);
                    mod.getInputControls().release(Input.MOVE_BACK);
                    mod.getInputControls().release(Input.SNEAK);
                }
            }
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getBuilderProcess().onLostControl();
                VisibleTaskDiagnostics.logDecision(mod, this, "destroy_block_set_goal_and_path",
                        "targetPosition=" + ChatClefDiagnostics.blockPos(pos),
                        "targetPosition", ChatClefDiagnostics.blockPos(pos),
                        "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)),
                        "aboveBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos.up())));
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(mod.getWorld().getBlockState(pos.up()).getBlock() ==
                        Blocks.SNOW ? new GoalBlock(pos) : new GoalNear(pos, 1));
            }
        }
        return null;
    }

    /**
     * This method is called when the task is interrupted or stopped.
     * It cancels Baritone pathing and releases certain input controls.
     *
     * @param interruptTask The task that interrupted the current task.
     */
    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();
        VisibleTaskDiagnostics.logLifecycle(mod, this, "STOP_BEGIN", "destroy_block_stop",
                "targetPosition", ChatClefDiagnostics.blockPos(pos),
                "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(pos)),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));

        // Cancel Baritone pathing
        mod.getClientBaritone().getPathingBehavior().forceCancel();

        // If not in game, return
        if (!AltoClef.inGame()) {
            VisibleTaskDiagnostics.logLifecycle(mod, this, "STOP_END_NOT_IN_GAME", "destroy_block_stop",
                    "targetPosition", ChatClefDiagnostics.blockPos(pos),
                    "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
            return;
        }

        // Release input controls
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(Input.CLICK_LEFT, false);
        mod.getInputControls().release(Input.SNEAK);
        mod.getInputControls().release(Input.MOVE_BACK);
        mod.getInputControls().release(Input.MOVE_FORWARD);

        // Logging statements for debugging
        Debug.logInternal("onStop method called");
        Debug.logInternal("Baritone pathing cancelled");
        if (!AltoClef.inGame()) {
            Debug.logInternal("Not in game");
        }
        Debug.logInternal("Left click input force state set to false");
        Debug.logInternal("Released sneak input control");
        Debug.logInternal("Released move back input control");
        Debug.logInternal("Released move forward input control");
        VisibleTaskDiagnostics.logLifecycle(mod, this, "STOP_END", "destroy_block_stop",
                "targetPosition", ChatClefDiagnostics.blockPos(pos),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
    }

    /**
     * Checks if the block at the given position is air.
     *
     * @return true if the block is air, false otherwise
     */
    @Override
    public boolean isFinished() {
        BlockState blockState = AltoClef.getInstance().getWorld().getBlockState(pos);
        boolean isAir = blockState.isAir();
        Debug.logInternal("Block at position " + pos + " is air: " + isAir);
        VisibleTaskDiagnostics.logFinishedCheck(AltoClef.getInstance(), this, isAir, "destroy_block_is_finished",
                "targetPosition", ChatClefDiagnostics.blockPos(pos),
                "targetBlockState", ChatClefDiagnostics.safeValue(() -> blockState));
        return isAir;
    }

    /**
     * Checks if this task is equal to another task.
     *
     * @param other The other task to compare against.
     * @return True if the tasks are equal, false otherwise.
     */
    @Override
    protected boolean isEqual(Task other) {
        boolean isSame = false;

        // Check if the other task is an instance of DestroyBlockTask
        if (other instanceof DestroyBlockTask destroyBlockTask) {

            // Check if the positions of the tasks are equal
            if (destroyBlockTask.pos.equals(pos)) {
                isSame = true;
            }
        }

        // Log the result of the equality check
        Debug.logInternal("isEqual result: " + isSame);

        // Return the result of the equality check
        return isSame;
    }

    /**
     * Generates a debug string representing the block destruction position.
     *
     * @return The debug string.
     */
    @Override
    protected String toDebugString() {
        return "Destroy block at " + pos.toShortString();
    }
}
