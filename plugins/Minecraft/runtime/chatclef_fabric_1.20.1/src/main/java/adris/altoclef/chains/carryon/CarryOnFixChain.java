package adris.altoclef.chains.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.SingleTaskChain;
import adris.altoclef.tasks.construction.carryon.PlaceCarriedBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.compat.CarryOnCompat;
import adris.altoclef.util.logging.StateChangeLogger;
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

    public CarryOnFixChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        if (mainTask instanceof PlaceCarriedBlockTask) {
            successCooldownTicks = SUCCESS_COOLDOWN_TICKS;
            debugLogger.event("carried block preflight finished; success cooldown started: ticks=" + successCooldownTicks);
        } else {
            debugLogger.event("carried block preflight finished");
        }
        mainTask = null;
    }

    @Override
    public float getPriority() {
        AltoClef mod = AltoClef.getInstance();
        if (!canCheckCarryState(mod)) {
            clearCurrentPlacementTask();
            return INACTIVE_PRIORITY;
        }

        tickCooldowns();

        if (mainTask instanceof PlaceCarriedBlockTask placeTask) {
            if (placeTask.hasFailed()) {
                rememberFailure(mod);
                clearCurrentPlacementTask();
                return INACTIVE_PRIORITY;
            }
            return PRIORITY;
        }

        Optional<BlockState> carriedState = getCarriedState(mod);
        if (carriedState.isEmpty()) {
            debugLogger.state("idle: no carried block");
            return INACTIVE_PRIORITY;
        }

        BlockState state = carriedState.get();
        if (!policy.shouldPlaceBeforeUserTask(state)) {
            debugLogger.state("ignored carried state: " + policy.describeBlock(state));
            return INACTIVE_PRIORITY;
        }

        Block carriedBlock = state.getBlock();
        if (successCooldownTicks > 0) {
            debugLogger.state("cooldown after placement success: block=" + policy.describeBlock(carriedBlock)
                    + ", ticks_left=" + successCooldownTicks);
            return INACTIVE_PRIORITY;
        }
        if (isInFailureCooldown(carriedBlock)) {
            debugLogger.state("cooldown after placement failure: block=" + policy.describeBlock(carriedBlock)
                    + ", ticks_left=" + failureCooldownTicks);
            return INACTIVE_PRIORITY;
        }

        debugLogger.event("detected carried block before task loop: block=" + policy.describeBlock(state)
                + ", reason=" + policy.describeReason(state));
        setTask(new PlaceCarriedBlockTask(carriedBlock));
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
                + policy.describeBlock(lastFailedBlock) + ", ticks=" + failureCooldownTicks);
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
}
