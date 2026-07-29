package adris.altoclef.tasks.interaction.block;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.catalogue.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.time.TimerGame;
import baritone.api.pathing.goals.Goal;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.Objects;
import java.util.Optional;

//20260730_kpopmodder: Moved into interaction.block so block-interaction flow and helpers stay together.
/**
 * Left or Right click on a block on a particular (or any) side of the block.
 */
public class InteractWithBlockTask extends Task {
    private final ItemTarget toUse;
    private final Direction direction;
    private final BlockPos target;
    private final boolean walkInto;
    private final Vec3i interactOffset;
    private final Input interactInput;
    private final boolean shiftClick;
    private final TimerGame clickTimer = new TimerGame(5);
    private final BlockInteractionProgressTracker progressTracker = new BlockInteractionProgressTracker();
    private final BlockInteractionRecoveryPolicy recoveryPolicy = new BlockInteractionRecoveryPolicy();
    private ClickResponse cachedClickStatus = ClickResponse.CANT_REACH;
    private final StateChangeLogger debugLogger = new StateChangeLogger("InteractWithBlockTask");
    private final BlockInteractionClickController clickController;
    private int debugTickCount;

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, Vec3i interactOffset, boolean shiftClick) {
        this.toUse = toUse;
        this.direction = direction;
        this.target = target;
        this.interactInput = interactInput;
        this.walkInto = walkInto;
        this.interactOffset = interactOffset;
        this.shiftClick = shiftClick;
        this.clickController = new BlockInteractionClickController(toUse, direction, target, interactInput, shiftClick, debugLogger);
    }

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, boolean shiftClick) {
        this(toUse, direction, target, interactInput, walkInto, Vec3i.ZERO, shiftClick);
    }

    public InteractWithBlockTask(ItemTarget toUse, Direction direction, BlockPos target, boolean walkInto) {
        this(toUse, direction, target, Input.CLICK_RIGHT, walkInto, true);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto, Vec3i interactOffset) {
        // null means any side is OK
        this(toUse, null, target, Input.CLICK_RIGHT, walkInto, interactOffset, true);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target, boolean walkInto) {
        this(toUse, target, walkInto, Vec3i.ZERO);
    }

    public InteractWithBlockTask(ItemTarget toUse, BlockPos target) {
        this(toUse, target, false);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, Vec3i interactOffset, boolean shiftClick) {
        this(new ItemTarget(toUse, 1), direction, target, interactInput, walkInto, interactOffset, shiftClick);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, Input interactInput, boolean walkInto, boolean shiftClick) {
        this(new ItemTarget(toUse, 1), direction, target, interactInput, walkInto, shiftClick);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, boolean walkInto) {
        this(new ItemTarget(toUse, 1), direction, target, walkInto);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target) {
        this(new ItemTarget(toUse, 1), direction, target, Input.CLICK_RIGHT, false, false);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target, boolean walkInto, Vec3i interactOffset) {
        this(new ItemTarget(toUse, 1), target, walkInto, interactOffset);
    }

    public InteractWithBlockTask(Item toUse, Direction direction, BlockPos target, Vec3i interactOffset) {
        this(new ItemTarget(toUse, 1), direction, target, Input.CLICK_RIGHT, false, interactOffset, false);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target, Vec3i interactOffset) {
        this(new ItemTarget(toUse, 1), null, target, Input.CLICK_RIGHT, false, interactOffset, false);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target, boolean walkInto) {
        this(new ItemTarget(toUse, 1), target, walkInto);
    }

    public InteractWithBlockTask(Item toUse, BlockPos target) {
        this(new ItemTarget(toUse, 1), target);
    }

    public InteractWithBlockTask(BlockPos target, boolean shiftClick) {
        this(ItemTarget.EMPTY, null, target, Input.CLICK_RIGHT, false, shiftClick);
    }

    public InteractWithBlockTask(BlockPos target) {
        this(ItemTarget.EMPTY, null, target, Input.CLICK_RIGHT, false, false);
    }

    @Override
    protected void onStart() {
        AltoClef.getInstance().getClientBaritone().getPathingBehavior().forceCancel();

        progressTracker.resetForStart();
        recoveryPolicy.resetWanderForStart();
        clickTimer.reset();
        debugTickCount = 0;
        debugLogger.event("start: target=" + target.toShortString()
                + ", toUse=" + toUse
                + ", direction=" + direction
                + ", shiftClick=" + shiftClick
                + ", " + clickController.describeInteractionContext(AltoClef.getInstance()));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        debugTickCount++;
        debugLogger.state("interact tick:" + debugTickCount,
                "interact tick=" + debugTickCount
                        + ", target=" + target.toShortString()
                        + ", toUse=" + toUse
                        + ", direction=" + direction
                        + ", walkInto=" + walkInto
                        + ", interactInput=" + interactInput
                        + ", shiftClick=" + shiftClick
                        + ", waitingForClickTicks=" + recoveryPolicy.getWaitingForClickTicks()
                        + ", cachedClickStatus=" + cachedClickStatus
                        + ", pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                        + ", customGoalActive=" + mod.getClientBaritone().getCustomGoalProcess().isActive()
                        + ", " + clickController.describeInteractionContext(mod));

        if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
            progressTracker.resetMoveProgress();
        }
        if (WorldHelper.isInNetherPortal()) {
            if (!mod.getClientBaritone().getPathingBehavior().isPathing()) {
                setDebugState("Getting out from nether portal");
                mod.getInputControls().hold(Input.SNEAK);
                mod.getInputControls().hold(Input.MOVE_FORWARD);
                debugLogger.state("interact return nether portal forward:" + debugTickCount,
                        "interact return: getting out from nether portal: tick=" + debugTickCount
                                + ", " + clickController.describeInteractionContext(mod));
                return null;
            } else {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        } else {
            if (mod.getClientBaritone().getPathingBehavior().isPathing()) {
                mod.getInputControls().release(Input.SNEAK);
                mod.getInputControls().release(Input.MOVE_BACK);
                mod.getInputControls().release(Input.MOVE_FORWARD);
            }
        }
        Task activeUnstuckTask = progressTracker.getActiveUnstuckTask(mod);
        if (activeUnstuckTask != null) {
            setDebugState("Getting unstuck from block.");
            progressTracker.resetStuckProgress();
            // Stop other tasks, we are JUST shimmying
            mod.getClientBaritone().getCustomGoalProcess().onLostControl();
            mod.getClientBaritone().getExploreProcess().onLostControl();
            debugLogger.state("interact return unstuck task:" + debugTickCount,
                    "interact return unstuck task: tick=" + debugTickCount
                            + ", task=" + activeUnstuckTask
                            + ", " + clickController.describeInteractionContext(mod));
            return activeUnstuckTask;
        }
        BlockInteractionProgressTracker.StuckRecovery stuckRecovery = progressTracker.checkStuckOrCreateRecovery(mod);
        if (stuckRecovery.hasTask()) {
            debugLogger.state("interact return new unstuck task:" + debugTickCount,
                    "interact return new unstuck task: tick=" + debugTickCount
                            + ", blockStuck=" + stuckRecovery.blockStuck().toShortString()
                            + ", task=" + stuckRecovery.task()
                            + ", " + clickController.describeInteractionContext(mod));
            return stuckRecovery.task();
        }

        cachedClickStatus = ClickResponse.CANT_REACH;

        // Get our use item first
        if (!ItemTarget.nullOrEmpty(toUse) && !StorageHelper.itemTargetsMet(mod, toUse)) {
            progressTracker.resetMoveProgress();
            clickTimer.reset();
            debugLogger.state("get interact item:" + debugTickCount,
                    "get interact item: tick=" + debugTickCount
                            + ", target=" + target.toShortString()
                            + ", item=" + toUse
                            + ", " + clickController.describeInteractionContext(mod));
            return TaskCatalogue.getItemTask(toUse);
        }

        // Wander and check
        if (recoveryPolicy.isWandering()) {
            progressTracker.resetMoveProgress();
            clickTimer.reset();
            debugLogger.state("wander before interact retry:" + debugTickCount,
                    "wander before interact retry: tick=" + debugTickCount
                            + ", target=" + target.toShortString()
                            + ", " + clickController.describeInteractionContext(mod));
            return recoveryPolicy.getWanderTask();
        }
        if (!progressTracker.isMovementProgressing(mod)) {
            Debug.logMessage("Failed, blacklisting and wandering.");
            debugLogger.state("interact movement failed:" + debugTickCount,
                    "interact movement failed; blacklisting target=" + target.toShortString()
                            + ", tick=" + debugTickCount
                            + ", " + clickController.describeInteractionContext(mod));
            mod.getBlockScanner().requestBlockUnreachable(target);
            return recoveryPolicy.getWanderTask();
        }

        int reachDistance = 0;
        Goal moveGoal = BlockInteractionGoalFactory.createGoalForInteract(target, reachDistance, direction, interactOffset, walkInto);
        ICustomGoalProcess proc = mod.getClientBaritone().getCustomGoalProcess();

        cachedClickStatus = clickController.click(mod);
        debugLogger.state("interact click response:" + debugTickCount,
                "interact click response: tick=" + debugTickCount
                        + ", response=" + cachedClickStatus
                        + ", customGoalActiveBeforeSwitch=" + proc.isActive()
                        + ", target=" + target.toShortString()
                        + ", " + clickController.describeInteractionContext(mod));
        switch (Objects.requireNonNull(cachedClickStatus)) {
            case CANT_REACH -> {
                setDebugState("Getting to our goal");
                // Get to our goal then
                if (!proc.isActive()) {
                    proc.setGoalAndPath(moveGoal);
                    debugLogger.state("interact set goal and path:" + debugTickCount,
                            "interact set goal and path: tick=" + debugTickCount
                                    + ", target=" + target.toShortString()
                                    + ", goal=" + moveGoal
                                    + ", " + clickController.describeInteractionContext(mod));
                }
                clickTimer.reset();
            }
            case WAIT_FOR_CLICK -> {
                setDebugState("Waiting for click");
                if (proc.isActive()) {
                    proc.onLostControl();
                    debugLogger.state("interact lost control wait click:" + debugTickCount,
                            "interact lost custom goal control while waiting for click: tick=" + debugTickCount
                                    + ", target=" + target.toShortString()
                                    + ", " + clickController.describeInteractionContext(mod));
                }
                clickTimer.reset();

                // try to get unstuck by pressing shift
                int waitingForClickTicks = recoveryPolicy.incrementWaitingForClickTicks();
                if (waitingForClickTicks % 25 == 0 && shiftClick) {
                    mod.getInputControls().hold(Input.SNEAK);
                    mod.log("trying to press shift");
                    debugLogger.state("interact wait click hold shift:" + debugTickCount,
                            "interact wait click hold shift: tick=" + debugTickCount
                                    + ", waitingForClickTicks=" + waitingForClickTicks
                                    + ", " + clickController.describeInteractionContext(mod));
                }

                if (waitingForClickTicks > 10*20) {
                    mod.log("trying to wander");
                    recoveryPolicy.resetWaitingForClickTicks();
                    debugLogger.state("interact wait click timeout wander:" + debugTickCount,
                            "interact wait click timeout wander: tick=" + debugTickCount
                                    + ", target=" + target.toShortString()
                                    + ", " + clickController.describeInteractionContext(mod));
                    return recoveryPolicy.getWanderTask();
                }
            }
            case CLICK_ATTEMPTED -> {
                setDebugState("Clicking.");
                if (proc.isActive()) {
                    proc.onLostControl();
                    debugLogger.state("interact lost control clicked:" + debugTickCount,
                            "interact lost custom goal control after click attempted: tick=" + debugTickCount
                                    + ", target=" + target.toShortString()
                                    + ", " + clickController.describeInteractionContext(mod));
                }
                if (clickTimer.elapsed()) {
                    // We tried clicking but failed.
                    clickTimer.reset();
                    debugLogger.state("interact click timeout wander:" + debugTickCount,
                            "interact click timeout wander: tick=" + debugTickCount
                                    + ", target=" + target.toShortString()
                                    + ", " + clickController.describeInteractionContext(mod));
                    return recoveryPolicy.getWanderTask();
                }
            }
        }

        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();

        debugLogger.event("stop: interruptedBy=" + describeTask(interruptTask)
                + ", target=" + target.toShortString()
                + ", ticks=" + debugTickCount
                + ", cachedClickStatus=" + cachedClickStatus
                + ", waitingForClickTicks=" + recoveryPolicy.getWaitingForClickTicks()
                + ", " + clickController.describeInteractionContext(mod));
        mod.getClientBaritone().getPathingBehavior().forceCancel();
        mod.getInputControls().release(Input.SNEAK);
    }

    @Override
    public boolean isFinished() {
        return false;
        //return _trying && !proc(mod).isActive();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof InteractWithBlockTask task) {
            if ((task.direction == null) != (direction == null)) return false;
            if (task.direction != null && !task.direction.equals(direction)) return false;
            if ((task.toUse == null) != (toUse == null)) return false;
            if (task.toUse != null && !task.toUse.equals(toUse)) return false;
            if (!task.target.equals(target)) return false;
            if (!task.interactInput.equals(interactInput)) return false;
            return task.walkInto == walkInto;
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Interact using " + toUse + " at " + target + " dir " + direction;
    }

    public ClickResponse getClickStatus() {
        return cachedClickStatus;
    }

    public Optional<Rotation> getCurrentReach() {
        return clickController.getCurrentReach();
    }

    private String describeTask(Task task) {
        return BlockInteractionDiagnostics.describeTask(task);
    }

    public enum ClickResponse {
        CANT_REACH,
        WAIT_FOR_CLICK,
        CLICK_ATTEMPTED
    }

}
