package adris.altoclef.tasks.construction.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import baritone.api.utils.Rotation;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.GameOptionsScreen;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

//20260728_kpopmodder: Added this task to place blocks carried by the Carry On mod before normal item-crafting fallback starts.
public class PlaceCarriedBlockTask extends Task {

    //20260730_kpopmodder: Identify each carried placement attempt so task-chain handoffs can be traced in logs.
    private static final AtomicInteger NEXT_DIAGNOSTIC_ID = new AtomicInteger(1);

    private static final int MAX_TICKS = 20 * 10;
    private static final int CLICK_INTERVAL_TICKS = 2;
    private static final int MAX_EMPTY_SPACE_CANDIDATES = 8;
    private static final int RELEASE_CONFIRM_TICKS = 2;

    private final int diagnosticId;
    private final String diagnosticSource;
    private final Block[] expectedBlocks;
    private final CarriedBlockPlacementPlanner planner = new CarriedBlockPlacementPlanner();
    private final CarryOnEmptySpacePlacementPlanner emptySpacePlanner = new CarryOnEmptySpacePlacementPlanner();
    private final CarryOnPlacementCandidateCycle targetCycle = new CarryOnPlacementCandidateCycle();
    private final CarryOnPlacementInputController inputController = new CarryOnPlacementInputController();
    private final StateChangeLogger debugLogger = new StateChangeLogger("PlaceCarriedBlockTask");

    private Subscription<BlockPlaceEvent> blockPlaceSubscription;
    private CarriedBlockPlacementPlanner.PlacementTarget target;
    private BlockPos placed;
    private boolean completed;
    private boolean failed;
    private boolean targetFromEmptySpaceFallback;
    private boolean wasCarryingTarget;
    private int noCarryTicks;
    private int ticks;
    private int clickCooldown;
    private int attempts;

    public PlaceCarriedBlockTask(Block... expectedBlocks) {
        this("unspecified", expectedBlocks);
    }

    public PlaceCarriedBlockTask(String diagnosticSource, Block... expectedBlocks) {
        this.diagnosticId = NEXT_DIAGNOSTIC_ID.getAndIncrement();
        this.diagnosticSource = diagnosticSource == null || diagnosticSource.trim().isEmpty()
                ? "unspecified"
                : diagnosticSource;
        this.expectedBlocks = expectedBlocks;
    }

    public String describeDiagnostic() {
        return "placementId=" + diagnosticId + ", source=" + diagnosticSource;
    }

    public boolean hasFailed() {
        return failed;
    }

    public BlockPos getPlaced() {
        return placed != null ? placed : target == null ? null : target.placePos();
    }

    public boolean isCarryingTarget(AltoClef mod) {
        return getCarriedTargetState(mod).isPresent();
    }

    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();
        target = null;
        placed = null;
        completed = false;
        failed = false;
        targetFromEmptySpaceFallback = false;
        targetCycle.reset();
        wasCarryingTarget = isCarryingTarget(mod);
        noCarryTicks = 0;
        ticks = 0;
        clickCooldown = 0;
        attempts = 0;
        inputController.reset();
        inputController.release(mod);
        blockPlaceSubscription = EventBus.subscribe(BlockPlaceEvent.class, event -> {
            if (!ArrayUtils.contains(expectedBlocks, event.blockState.getBlock())) {
                return;
            }
            //20260729_kpopmodder: Carry On may clear the carried state a few ticks after the block place event.
            boolean firstObservation = placed == null || !placed.equals(event.blockPos);
            placed = event.blockPos;
            if (firstObservation) {
                debugLogger.event(describeDiagnostic()
                        + ", observed carried placement at " + event.blockPos.toShortString()
                        + " block=" + event.blockState.getBlock().getTranslationKey());
            }
        });
        debugLogger.state("starting carried placement for " + describeExpectedBlocks(),
                "starting carried placement for " + describeExpectedBlocks()
                        + ", wasCarryingTarget=" + wasCarryingTarget
                        + ", " + describeRuntimeContext(mod));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        ticks++;
        Optional<BlockState> carriedState = getCarriedTargetState(mod);
        boolean observedPlacement = hasObservedPlacement(mod);
        debugLogger.state("carried placement tick:" + ticks,
                "carried placement tick=" + ticks
                        + ", attempts=" + attempts
                        + ", clickCooldown=" + clickCooldown
                        + ", completed=" + completed
                        + ", failed=" + failed
                        + ", wasCarryingTarget=" + wasCarryingTarget
                        + ", noCarryTicks=" + noCarryTicks
                        + ", observedPlacement=" + observedPlacement
                        + ", target=" + describeTarget()
                        + ", placed=" + describePos(placed)
                        + ", carriedState=" + describeOptionalBlockState(carriedState)
                        + ", expected=" + describeExpectedBlocks()
                        + ", " + describeRuntimeContext(mod));
        if (ticks > MAX_TICKS) {
            failed = true;
            debugLogger.event(describeDiagnostic()
                    + ", failed: timed out while placing carried block after attempts=" + attempts
                    + ", target=" + describeTarget()
                    + ", " + describeRuntimeContext(mod));
            return null;
        }
        if (observedPlacement) {
            if (target != null && isPlaced(mod, target.placePos())) {
                placed = target.placePos();
                debugLogger.state("observed placement target confirmed:" + ticks,
                        "observed placement target confirmed: tick=" + ticks
                                + ", placed=" + describePos(placed)
                                + ", target=" + describeTarget()
                                + ", carriedState=" + describeOptionalBlockState(carriedState)
                                + ", " + describeRuntimeContext(mod));
            }
            if (confirmReleasedAfterPlacement(carriedState)) {
                debugLogger.state("observed placement confirm release returned:" + ticks,
                        "observed placement confirm release returned: tick=" + ticks
                                + ", completed=" + completed
                                + ", noCarryTicks=" + noCarryTicks
                                + ", carriedState=" + describeOptionalBlockState(carriedState)
                                + ", target=" + describeTarget()
                                + ", " + describeRuntimeContext(mod));
                return null;
            }
            inputController.release(mod);
            setDebugState("Waiting for Carry On to release carried block");
            debugLogger.state("waiting-carried-release:" + ticks,
                    "waiting: placed block observed but Carry On still reports carried block: tick=" + ticks
                            + ", target=" + describeTarget()
                            + ", carriedState=" + describeOptionalBlockState(carriedState)
                            + ", " + describeRuntimeContext(mod));
            return null;
        }

        if (closeBlockingScreen(mod)) {
            setDebugState("Closing screen before placing carried block");
            debugLogger.state("closed or waiting blocking screen:" + ticks,
                    "closed or waiting blocking screen before placing carried block: tick=" + ticks
                            + ", target=" + describeTarget()
                            + ", " + describeRuntimeContext(mod));
            return null;
        }

        if (carriedState.isEmpty()) {
            if (wasCarryingTarget) {
                noCarryTicks++;
                setDebugState("Confirming carried block was placed");
                debugLogger.state("no carried state after previously carrying:" + ticks,
                        "no carried state after previously carrying: tick=" + ticks
                                + ", noCarryTicks=" + noCarryTicks
                                + ", releaseConfirmTicks=" + RELEASE_CONFIRM_TICKS
                                + ", target=" + describeTarget()
                                + ", placed=" + describePos(placed)
                                + ", " + describeRuntimeContext(mod));
                if (noCarryTicks >= RELEASE_CONFIRM_TICKS) {
                    completed = true;
                    placed = getPlaced();
                    debugLogger.event("carried block released; treating Carry On placement as complete: target="
                            + describeTarget()
                            + ", placed=" + describePos(placed)
                            + ", " + describeRuntimeContext(mod));
                }
                return null;
            }
            failed = true;
            debugLogger.event(describeDiagnostic()
                    + ", failed: expected carried block was not available at task start: "
                    + describeRuntimeContext(mod));
            return null;
        }
        wasCarryingTarget = true;
        noCarryTicks = 0;

        mod.getClientBaritone().getPathingBehavior().forceCancel();
        debugLogger.state("force cancel pathing for carried placement:" + ticks,
                "force cancel pathing for carried placement: tick=" + ticks
                        + ", carriedState=" + describeOptionalBlockState(carriedState)
                        + ", target=" + describeTarget()
                        + ", " + describeRuntimeContext(mod));

        if (target == null || !isTargetValid(mod, target, carriedState.get())) {
            debugLogger.state("select carried placement target needed:" + ticks,
                    "select carried placement target needed: tick=" + ticks
                            + ", currentTarget=" + describeTarget()
                            + ", carriedState=" + describeOptionalBlockState(carriedState)
                            + ", targetNull=" + (target == null)
                            + ", " + describeRuntimeContext(mod));
            selectPlacementTarget(mod, carriedState.get());
            if (target == null) {
                setDebugState("Searching empty placement spot for carried block");
                debugLogger.state("waiting no carried placement target:" + ticks,
                        "waiting: no block-empty nearby Carry On placement support: tick=" + ticks
                                + ", carriedBlock=" + describeBlock(carriedState.get().getBlock())
                                + ", " + describeRuntimeContext(mod));
                return null;
            }
        }

        setDebugState("Placing carried " + describeBlock(carriedState.get().getBlock())
                + " at " + target.placePos().toShortString());

        if (!prepareTargetForClick(mod)) {
            debugLogger.state("prepare carried target not ready:" + ticks,
                    "prepare carried target not ready: tick=" + ticks
                            + ", target=" + describeTarget()
                            + ", carriedState=" + describeOptionalBlockState(carriedState)
                            + ", " + describeRuntimeContext(mod));
            return null;
        }

        if (clickCooldown > 0) {
            clickCooldown--;
            debugLogger.state("carried placement click cooldown:" + ticks,
                    "carried placement click cooldown: tick=" + ticks
                            + ", remaining=" + clickCooldown
                            + ", target=" + describeTarget()
                            + ", " + describeRuntimeContext(mod));
            return null;
        }
        clickCooldown = CLICK_INTERVAL_TICKS;
        int nextAttempt = attempts + 1;
        debugLogger.state("carried placement click attempt start:" + ticks,
                "carried placement click attempt start: tick=" + ticks
                        + ", nextAttempt=" + nextAttempt
                        + ", target=" + describeTarget()
                        + ", carriedState=" + describeOptionalBlockState(carriedState)
                        + ", " + describeRuntimeContext(mod));
        if (inputController.tryShiftRightClickSupport(mod, target, nextAttempt)) {
            attempts = nextAttempt;
            debugLogger.state("carried placement click attempt accepted:" + ticks,
                    "carried placement click attempt accepted: tick=" + ticks
                            + ", attempts=" + attempts
                            + ", target=" + describeTarget()
                            + ", targetCycleMode=" + targetCycle.mode()
                            + ", targetCycleSize=" + targetCycle.size()
                            + ", " + describeRuntimeContext(mod));
            if (targetCycle.isBlockEmptyFallback()) {
                targetCycle.advance();
                target = targetCycle.current();
                debugLogger.state("carried placement target cycle advanced:" + ticks,
                        "carried placement target cycle advanced: tick=" + ticks
                                + ", nextTarget=" + describeTarget()
                                + ", targetCycleMode=" + targetCycle.mode()
                                + ", targetCycleSize=" + targetCycle.size()
                                + ", " + describeRuntimeContext(mod));
            }
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();
        inputController.release(mod);
        if (blockPlaceSubscription != null) {
            EventBus.unsubscribe(blockPlaceSubscription);
            blockPlaceSubscription = null;
        }
        debugLogger.event("stop carried placement: interruptedBy=" + describeTask(interruptTask)
                + ", " + describeDiagnostic()
                + ", completed=" + completed
                + ", failed=" + failed
                + ", ticks=" + ticks
                + ", attempts=" + attempts
                + ", placed=" + describePos(placed)
                + ", target=" + describeTarget()
                + ", " + describeRuntimeContext(mod));
    }

    @Override
    public boolean isFinished() {
        return completed;
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof PlaceCarriedBlockTask task
                && Arrays.equals(expectedBlocks, task.expectedBlocks);
    }

    @Override
    protected String toDebugString() {
        return "Place carried block nearby";
    }

    private Optional<BlockState> getCarriedTargetState(AltoClef mod) {
        return CarryOnCompat.getCarriedBlockState(mod.getPlayer())
                .filter(state -> ArrayUtils.contains(expectedBlocks, state.getBlock()));
    }

    private boolean closeBlockingScreen(AltoClef mod) {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        if (screen == null) {
            return false;
        }
        boolean closeAllowed = !(screen instanceof GameMenuScreen) && !(screen instanceof GameOptionsScreen) && !(screen instanceof ChatScreen);
        debugLogger.state("blocking screen before carried placement:" + ticks,
                "blocking screen before carried placement: tick=" + ticks
                        + ", screen=" + screen.getClass().getSimpleName()
                        + ", closeAllowed=" + closeAllowed
                        + ", " + describeRuntimeContext(mod));
        if (closeAllowed) {
            StorageHelper.closeScreen();
        }
        return true;
    }

    private boolean isPlaced(AltoClef mod, BlockPos pos) {
        return pos != null && ArrayUtils.contains(expectedBlocks, mod.getWorld().getBlockState(pos).getBlock());
    }

    private boolean hasObservedPlacement(AltoClef mod) {
        return isPlaced(mod, placed) || target != null && isPlaced(mod, target.placePos());
    }

    private boolean confirmReleasedAfterPlacement(Optional<BlockState> carriedState) {
        if (carriedState.isPresent()) {
            noCarryTicks = 0;
            debugLogger.state("confirm release still carrying:" + ticks,
                    "confirm release: still carrying after placement: tick=" + ticks
                            + ", " + describeDiagnostic()
                            + ", target=" + describeTarget()
                            + ", carriedState=" + describeOptionalBlockState(carriedState));
            return false;
        }

        noCarryTicks++;
        setDebugState("Confirming carried block was placed");
        if (noCarryTicks < RELEASE_CONFIRM_TICKS) {
            debugLogger.state("confirm release waiting:" + ticks,
                    "confirm release waiting: tick=" + ticks
                            + ", " + describeDiagnostic()
                            + ", noCarryTicks=" + noCarryTicks
                            + ", releaseConfirmTicks=" + RELEASE_CONFIRM_TICKS
                            + ", target=" + describeTarget());
            return true;
        }

        completed = true;
        debugLogger.event(describeDiagnostic()
                + ", carried block released after placement; treating Carry On placement as complete: target="
                + describeTarget());
        return true;
    }

    private void selectPlacementTarget(AltoClef mod, BlockState carriedState) {
        List<CarriedBlockPlacementPlanner.PlacementTarget> emptySpaceTargets =
                emptySpacePlanner.findNearestCandidates(mod, carriedState, MAX_EMPTY_SPACE_CANDIDATES);
        debugLogger.state("empty-space carried target candidates:" + ticks,
                "empty-space carried target candidates: tick=" + ticks
                        + ", count=" + emptySpaceTargets.size()
                        + ", carriedState=" + describeOptionalBlockState(Optional.ofNullable(carriedState))
                        + ", " + describeRuntimeContext(mod));
        if (!emptySpaceTargets.isEmpty()) {
            targetCycle.setBlockEmptyFallback(emptySpaceTargets);
            target = targetCycle.current();
            targetFromEmptySpaceFallback = true;
            logSelectedTarget(targetCycle.mode(), targetCycle.size());
            return;
        }

        Optional<CarriedBlockPlacementPlanner.PlacementTarget> strictTarget = planner.findNearest(mod, carriedState);
        debugLogger.state("strict carried target candidate:" + ticks,
                "strict carried target candidate: tick=" + ticks
                        + ", present=" + strictTarget.isPresent()
                        + ", carriedState=" + describeOptionalBlockState(Optional.ofNullable(carriedState))
                        + ", " + describeRuntimeContext(mod));
        if (strictTarget.isPresent()) {
            targetCycle.setStrict(strictTarget.get());
            target = targetCycle.current();
            targetFromEmptySpaceFallback = false;
            logSelectedTarget(targetCycle.mode(), targetCycle.size());
            return;
        }

        targetCycle.reset();
        target = null;
        targetFromEmptySpaceFallback = false;
    }

    private boolean prepareTargetForClick(AltoClef mod) {
        if (targetFromEmptySpaceFallback) {
            LookHelper.lookAt(mod, target.supportPos(), target.supportFace());
            debugLogger.state("prepare carried target empty-space look:" + ticks,
                    "prepare carried target empty-space look: tick=" + ticks
                            + ", target=" + describeTarget()
                            + ", " + describeRuntimeContext(mod));
            return true;
        }

        Optional<Rotation> reach = LookHelper.getReach(target.supportPos(), target.supportFace());
        if (reach.isEmpty()) {
            debugLogger.state("retry support no longer reachable:" + ticks,
                    "retry: support no longer reachable: tick=" + ticks
                            + ", support=" + target.supportPos().toShortString()
                            + ", target=" + describeTarget()
                            + ", " + describeRuntimeContext(mod));
            target = null;
            targetCycle.reset();
            return false;
        }

        LookHelper.lookAt(reach.get());
        if (!LookHelper.isLookingAt(mod, reach.get())) {
            debugLogger.state("prepare carried target waiting look:" + ticks,
                    "prepare carried target waiting look: tick=" + ticks
                            + ", rotation=" + reach.get()
                            + ", target=" + describeTarget()
                            + ", " + describeRuntimeContext(mod));
            return false;
        }
        debugLogger.state("prepare carried target ready:" + ticks,
                "prepare carried target ready: tick=" + ticks
                        + ", rotation=" + reach.get()
                        + ", target=" + describeTarget()
                        + ", " + describeRuntimeContext(mod));
        return true;
    }

    private boolean isTargetValid(AltoClef mod, CarriedBlockPlacementPlanner.PlacementTarget target, BlockState carriedState) {
        if (targetFromEmptySpaceFallback) {
            return emptySpacePlanner.isValid(mod, target, carriedState);
        }
        return planner.isValid(mod, target, carriedState);
    }

    private void logSelectedTarget(String mode, int candidateCount) {
        debugLogger.state("selected carried placement target mode=" + mode
                + ", candidates=" + candidateCount
                + " place=" + target.placePos().toShortString()
                + " support=" + target.supportPos().toShortString()
                + " face=" + target.supportFace()
                + ", " + describeDiagnostic());
    }

    private String describeExpectedBlocks() {
        return Arrays.stream(expectedBlocks)
                .map(this::describeBlock)
                .reduce((left, right) -> left + ", " + right)
                .orElse("<none>");
    }

    private String describeBlock(Block block) {
        return block.getTranslationKey();
    }

    private String describeTarget() {
        if (target == null) {
            return "none";
        }
        return "place=" + target.placePos().toShortString()
                + ", support=" + target.supportPos().toShortString()
                + ", face=" + target.supportFace()
                + ", mode=" + (targetFromEmptySpaceFallback ? targetCycle.mode() : "strict")
                + ", candidates=" + targetCycle.size();
    }

    private String describePos(BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }

    private String describeOptionalBlockState(Optional<BlockState> state) {
        return state.map(blockState -> describeBlock(blockState.getBlock())).orElse("none");
    }

    private String describeRuntimeContext(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return describeDiagnostic() + ", context=missing-client";
        }
        return describeDiagnostic()
                + ", screen=" + describeCurrentScreen()
                + ", handler=" + (mod.getPlayer().currentScreenHandler == null
                ? "none"
                : mod.getPlayer().currentScreenHandler.getClass().getSimpleName())
                + ", pathing=" + mod.getClientBaritone().getPathingBehavior().isPathing()
                + ", playerSneaking=" + mod.getPlayer().isSneaking()
                + ", inputSneaking=" + mod.getPlayer().input.sneaking
                + ", sneakKeyHeld=" + mod.getInputControls().isHeldDown(Input.SNEAK)
                + ", useKeyHeld=" + mod.getInputControls().isHeldDown(Input.CLICK_RIGHT);
    }

    private String describeCurrentScreen() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private String describeTask(Task task) {
        if (task == null) {
            return "none";
        }
        try {
            return task.getClass().getSimpleName() + "{" + task + "}";
        } catch (RuntimeException ex) {
            return task.getClass().getSimpleName() + "{debugString failed: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "}";
        }
    }
}
