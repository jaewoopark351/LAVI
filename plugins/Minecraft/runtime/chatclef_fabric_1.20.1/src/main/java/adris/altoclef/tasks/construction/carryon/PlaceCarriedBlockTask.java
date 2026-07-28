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

//20260728_kpopmodder: Added this task to place blocks carried by the Carry On mod before normal item-crafting fallback starts.
public class PlaceCarriedBlockTask extends Task {

    private static final int MAX_TICKS = 20 * 10;
    private static final int CLICK_INTERVAL_TICKS = 2;
    private static final int MAX_EMPTY_SPACE_CANDIDATES = 8;

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
        this.expectedBlocks = expectedBlocks;
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
            placed = event.blockPos;
            completed = true;
            debugLogger.event("observed carried placement at " + event.blockPos.toShortString()
                    + " block=" + event.blockState.getBlock().getTranslationKey());
        });
        debugLogger.state("starting carried placement for " + describeExpectedBlocks());
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        ticks++;
        if (isPlaced(mod, placed)) {
            completed = true;
            return null;
        }
        if (target != null && isPlaced(mod, target.placePos())) {
            placed = target.placePos();
            completed = true;
            return null;
        }
        if (ticks > MAX_TICKS) {
            failed = true;
            debugLogger.event("failed: timed out while placing carried block after attempts=" + attempts
                    + ", target=" + describeTarget());
            return null;
        }

        if (closeBlockingScreen(mod)) {
            setDebugState("Closing screen before placing carried block");
            return null;
        }

        Optional<BlockState> carriedState = getCarriedTargetState(mod);
        if (carriedState.isEmpty()) {
            if (wasCarryingTarget) {
                noCarryTicks++;
                setDebugState("Confirming carried block was placed");
                if (noCarryTicks >= 2) {
                    completed = true;
                    placed = getPlaced();
                    debugLogger.event("carried block released; treating Carry On placement as complete: target="
                            + describeTarget());
                }
                return null;
            }
            failed = true;
            debugLogger.event("failed: expected carried block was not available at task start");
            return null;
        }
        wasCarryingTarget = true;
        noCarryTicks = 0;

        mod.getClientBaritone().getPathingBehavior().forceCancel();

        if (target == null || !isTargetValid(mod, target, carriedState.get())) {
            selectPlacementTarget(mod, carriedState.get());
            if (target == null) {
                setDebugState("Searching empty placement spot for carried block");
                debugLogger.state("waiting: no block-empty nearby Carry On placement support for "
                        + describeBlock(carriedState.get().getBlock()));
                return null;
            }
        }

        setDebugState("Placing carried " + describeBlock(carriedState.get().getBlock())
                + " at " + target.placePos().toShortString());

        if (!prepareTargetForClick(mod)) {
            return null;
        }

        if (clickCooldown > 0) {
            clickCooldown--;
            return null;
        }
        clickCooldown = CLICK_INTERVAL_TICKS;
        int nextAttempt = attempts + 1;
        if (inputController.tryShiftRightClickSupport(mod, target, nextAttempt)) {
            attempts = nextAttempt;
            if (targetCycle.isBlockEmptyFallback()) {
                targetCycle.advance();
                target = targetCycle.current();
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
    }

    @Override
    public boolean isFinished() {
        AltoClef mod = AltoClef.getInstance();
        return completed || isPlaced(mod, placed) || target != null && isPlaced(mod, target.placePos());
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
        if (!(screen instanceof GameMenuScreen) && !(screen instanceof GameOptionsScreen) && !(screen instanceof ChatScreen)) {
            StorageHelper.closeScreen();
        }
        return true;
    }

    private boolean isPlaced(AltoClef mod, BlockPos pos) {
        return pos != null && ArrayUtils.contains(expectedBlocks, mod.getWorld().getBlockState(pos).getBlock());
    }

    private void selectPlacementTarget(AltoClef mod, BlockState carriedState) {
        List<CarriedBlockPlacementPlanner.PlacementTarget> emptySpaceTargets =
                emptySpacePlanner.findNearestCandidates(mod, carriedState, MAX_EMPTY_SPACE_CANDIDATES);
        if (!emptySpaceTargets.isEmpty()) {
            targetCycle.setBlockEmptyFallback(emptySpaceTargets);
            target = targetCycle.current();
            targetFromEmptySpaceFallback = true;
            logSelectedTarget(targetCycle.mode(), targetCycle.size());
            return;
        }

        Optional<CarriedBlockPlacementPlanner.PlacementTarget> strictTarget = planner.findNearest(mod, carriedState);
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
            return true;
        }

        Optional<Rotation> reach = LookHelper.getReach(target.supportPos(), target.supportFace());
        if (reach.isEmpty()) {
            debugLogger.state("retry: support no longer reachable at " + target.supportPos().toShortString());
            target = null;
            targetCycle.reset();
            return false;
        }

        LookHelper.lookAt(reach.get());
        if (!LookHelper.isLookingAt(mod, reach.get())) {
            return false;
        }
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
                + " face=" + target.supportFace());
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
}
