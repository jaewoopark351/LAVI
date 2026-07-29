package adris.altoclef.chains.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasks.construction.carryon.PlaceCarriedBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.logging.StateChangeLogger;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;

import java.util.Optional;

//20260728_kpopmodder: Added this chain so carried Carry On blocks are placed before normal task loops continue.
public class CarryOnFixChain extends SingleTaskChain {

    private static final float PRIORITY = 52;
    private static final float INACTIVE_PRIORITY = -100;
    private static final int FAILURE_COOLDOWN_TICKS = 80;
    //20260729_kpopmodder: Give Carry On a short tick window to clear stale carried state after successful placement.
    private static final int SUCCESS_COOLDOWN_TICKS = 8;

    private final CarriedBlockPreflightPolicy policy = new CarriedBlockPreflightPolicy();
    private final StateChangeLogger debugLogger = new StateChangeLogger("CarryOnFixChain");

    private Block lastFailedBlock;
    private int failureCooldownTicks;
    private int successCooldownTicks;
    private int priorityCheckCount;
    private int detectedCarriedCount;

    public CarryOnFixChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        if (mainTask instanceof PlaceCarriedBlockTask) {
            successCooldownTicks = SUCCESS_COOLDOWN_TICKS;
            debugLogger.event("carried block preflight finished; success cooldown started: ticks=" + successCooldownTicks
                    + ", " + describeChainContext(mod));
        } else {
            debugLogger.event("carried block preflight finished: task=" + describeTask(mainTask)
                    + ", " + describeChainContext(mod));
        }
        mainTask = null;
    }

    @Override
    public float getPriority() {
        AltoClef mod = AltoClef.getInstance();
        priorityCheckCount++;
        debugLogger.state("priority check:" + priorityCheckCount,
                "priority check: tick=" + priorityCheckCount
                        + ", mainTask=" + describeTask(mainTask)
                        + ", successCooldownTicks=" + successCooldownTicks
                        + ", failureCooldownTicks=" + failureCooldownTicks
                        + ", lastFailedBlock=" + policy.describeBlock(lastFailedBlock)
                        + ", " + describeChainContext(mod));
        if (!canCheckCarryState(mod)) {
            debugLogger.state("priority inactive cannot check:" + priorityCheckCount,
                    "priority inactive: cannot check Carry On state: tick=" + priorityCheckCount
                            + ", mainTask=" + describeTask(mainTask)
                            + ", " + describeChainContext(mod));
            clearCurrentPlacementTask();
            return INACTIVE_PRIORITY;
        }

        tickCooldowns();
        debugLogger.state("priority cooldowns after tick:" + priorityCheckCount,
                "priority cooldowns after tick: tick=" + priorityCheckCount
                        + ", successCooldownTicks=" + successCooldownTicks
                        + ", failureCooldownTicks=" + failureCooldownTicks
                        + ", " + describeChainContext(mod));

        if (mainTask instanceof PlaceCarriedBlockTask placeTask) {
            debugLogger.state("active carried placement task:"
                            + placeTask.getPlaced()
                            + ":failed=" + placeTask.hasFailed()
                            + ":successCooldown=" + successCooldownTicks
                            + ":failureCooldown=" + failureCooldownTicks,
                    "active carried placement task: placed=" + describePos(placeTask.getPlaced())
                            + ", failed=" + placeTask.hasFailed()
                            + ", successCooldownTicks=" + successCooldownTicks
                            + ", failureCooldownTicks=" + failureCooldownTicks
                            + ", " + describeChainContext(mod));
            if (placeTask.hasFailed()) {
                rememberFailure(mod);
                debugLogger.state("priority inactive placement failed:" + priorityCheckCount,
                        "priority inactive: carried placement task failed: tick=" + priorityCheckCount
                                + ", task=" + describeTask(placeTask)
                                + ", " + describeChainContext(mod));
                clearCurrentPlacementTask();
                return INACTIVE_PRIORITY;
            }
            debugLogger.state("priority active existing placement:" + priorityCheckCount,
                    "priority active: existing carried placement task: tick=" + priorityCheckCount
                            + ", task=" + describeTask(placeTask)
                            + ", " + describeChainContext(mod));
            return PRIORITY;
        }

        Optional<BlockState> carriedState = getCarriedState(mod);
        if (carriedState.isEmpty()) {
            debugLogger.state("priority idle no carried block:" + priorityCheckCount,
                    "priority idle: no carried block: tick=" + priorityCheckCount
                            + ", " + describeChainContext(mod));
            return INACTIVE_PRIORITY;
        }

        BlockState state = carriedState.get();
        if (!policy.shouldPlaceBeforeUserTask(state)) {
            debugLogger.state("priority ignored carried state:" + priorityCheckCount,
                    "priority ignored carried state: tick=" + priorityCheckCount
                            + ", block=" + policy.describeBlock(state)
                            + ", reason=" + policy.describeReason(state)
                            + ", " + describeChainContext(mod));
            return INACTIVE_PRIORITY;
        }

        Block carriedBlock = state.getBlock();
        if (successCooldownTicks > 0) {
            debugLogger.state("cooldown after placement success: block=" + policy.describeBlock(carriedBlock)
                            + ", ticks_left=" + successCooldownTicks
                            + ", context=" + describeChainContext(mod),
                    "cooldown after placement success: block=" + policy.describeBlock(carriedBlock)
                            + ", ticks_left=" + successCooldownTicks
                            + ", " + describeChainContext(mod));
            return INACTIVE_PRIORITY;
        }
        if (isInFailureCooldown(carriedBlock)) {
            debugLogger.state("cooldown after placement failure: block=" + policy.describeBlock(carriedBlock)
                            + ", ticks_left=" + failureCooldownTicks
                            + ", context=" + describeChainContext(mod),
                    "cooldown after placement failure: block=" + policy.describeBlock(carriedBlock)
                            + ", ticks_left=" + failureCooldownTicks
                            + ", " + describeChainContext(mod));
            return INACTIVE_PRIORITY;
        }

        detectedCarriedCount++;
        debugLogger.event("detected carried block before task loop: block=" + policy.describeBlock(state)
                + ", reason=" + policy.describeReason(state)
                + ", detectedCount=" + detectedCarriedCount
                + ", priorityTick=" + priorityCheckCount
                + ", " + describeChainContext(mod));
        setTask(new PlaceCarriedBlockTask(carriedBlock));
        debugLogger.state("priority active new placement:" + priorityCheckCount,
                "priority active: new carried placement task created: tick=" + priorityCheckCount
                        + ", detectedCount=" + detectedCarriedCount
                        + ", block=" + policy.describeBlock(state)
                        + ", mainTask=" + describeTask(mainTask)
                        + ", " + describeChainContext(mod));
        return PRIORITY;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getName() {
        return "Carry On fix chain";
    }

    private boolean canCheckCarryState(AltoClef mod) {
        if (mod == null || !AltoClef.inGame() || mod.getPlayer() == null || !CarryOnCompat.isLoaded()) {
            debugLogger.state("inactive: carry-on unavailable or not in game");
            return false;
        }
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ChatScreen || currentScreen instanceof DeathScreen || currentScreen instanceof GameMenuScreen) {
            debugLogger.state("inactive: protected screen open: " + currentScreen.getClass().getSimpleName());
            return false;
        }
        return true;
    }

    private Optional<BlockState> getCarriedState(AltoClef mod) {
        try {
            return CarryOnCompat.getCarriedBlockState(mod.getPlayer());
        } catch (RuntimeException | LinkageError e) {
            debugLogger.event("failed to read Carry On carried block state: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private void rememberFailure(AltoClef mod) {
        Optional<BlockState> carriedState = getCarriedState(mod);
        lastFailedBlock = carriedState.map(BlockState::getBlock).orElse(lastFailedBlock);
        failureCooldownTicks = FAILURE_COOLDOWN_TICKS;
        debugLogger.event("carried block placement failed; cooldown started: block="
                + policy.describeBlock(lastFailedBlock) + ", ticks=" + failureCooldownTicks
                + ", " + describeChainContext(mod));
    }

    private void clearCurrentPlacementTask() {
        if (mainTask != null) {
            setTask(null);
        }
    }

    private void tickCooldowns() {
        if (failureCooldownTicks > 0) {
            failureCooldownTicks--;
        }
        if (successCooldownTicks > 0) {
            successCooldownTicks--;
        }
    }

    private boolean isInFailureCooldown(Block carriedBlock) {
        if (failureCooldownTicks <= 0) {
            return false;
        }
        return lastFailedBlock == null || lastFailedBlock == carriedBlock;
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

    private String describeChainContext(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "context=missing-client";
        }
        return "screen=" + describeCurrentScreen()
                + ", handler=" + (mod.getPlayer().currentScreenHandler == null
                ? "none"
                : mod.getPlayer().currentScreenHandler.getClass().getSimpleName())
                + ", playerSneaking=" + mod.getPlayer().isSneaking()
                + ", inputSneaking=" + mod.getPlayer().input.sneaking
                + ", sneakKeyHeld=" + mod.getInputControls().isHeldDown(Input.SNEAK)
                + ", useKeyHeld=" + mod.getInputControls().isHeldDown(Input.CLICK_RIGHT)
                + ", carriedBlock=" + describeCarriedBlock(mod);
    }

    private String describeCurrentScreen() {
        Screen screen = MinecraftClient.getInstance().currentScreen;
        return screen == null ? "none" : screen.getClass().getSimpleName();
    }

    private String describeCarriedBlock(AltoClef mod) {
        try {
            return CarryOnCompat.getCarriedBlockState(mod.getPlayer())
                    .map(state -> policy.describeBlock(state))
                    .orElse("none");
        } catch (RuntimeException | LinkageError e) {
            return "read-failed:" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    private String describePos(net.minecraft.util.math.BlockPos pos) {
        return pos == null ? "none" : pos.toShortString();
    }
}
