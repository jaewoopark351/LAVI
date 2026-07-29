package adris.altoclef.tasks.container.access;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.construction.carryon.PlaceCarriedBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.logging.StateChangeLogger;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;
import java.util.function.Consumer;

//20260729_kpopmodder: Added this controller to keep Carry On container placement state out of the main task tick flow.
public final class CarryOnContainerController {

    private final Block[] containerBlocks;
    private final ContainerBlockValidator blockValidator;
    private final StateChangeLogger debugLogger;
    private PlaceCarriedBlockTask placeCarriedTask;
    private int debugTickCount;
    private int localPlaceTaskCreateCount;

    public CarryOnContainerController(Block[] containerBlocks,
                                      ContainerBlockValidator blockValidator,
                                      StateChangeLogger debugLogger) {
        this.containerBlocks = containerBlocks;
        this.blockValidator = blockValidator;
        this.debugLogger = debugLogger;
    }

    public void reset() {
        placeCarriedTask = null;
        debugTickCount = 0;
        localPlaceTaskCreateCount = 0;
    }

    public boolean shouldUseSafeInteraction() {
        return blockValidator.shouldUseCarryOnSafeInteraction();
    }

    public BlockPos getCarriedPlacedPosition() {
        return placeCarriedTask == null ? null : placeCarriedTask.getPlaced();
    }

    public boolean isCarriedTaskActive() {
        return placeCarriedTask != null && placeCarriedTask.isActive();
    }

    public boolean isCarriedTaskFinished() {
        return placeCarriedTask != null && placeCarriedTask.isFinished();
    }

    public String describeStatus(AltoClef mod) {
        return "safeInteraction=" + shouldUseSafeInteraction()
                + ", localPlaceTask=" + describeLocalPlaceTask()
                + ", localPlaceTaskCreates=" + localPlaceTaskCreateCount
                + ", controllerTicks=" + debugTickCount
                + ", carriedBlock=" + ContainerTaskDiagnostics.describeCarriedBlock(mod);
    }

    public Task getCarriedContainerPlacementTask(AltoClef mod,
                                                Consumer<BlockPos> cachedContainerUpdater,
                                                Runnable justPlacedTimerReset,
                                                Consumer<String> debugStateSetter) {
        debugTickCount++;
        boolean safeInteraction = shouldUseSafeInteraction();
        debugLogger.state("carried container controller tick:" + debugTickCount,
                "carried container controller tick=" + debugTickCount
                        + ", safeInteraction=" + safeInteraction
                        + ", " + describeStatus(mod)
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        if (!safeInteraction) {
            debugLogger.state("carried container controller inactive:" + debugTickCount,
                    "carried container controller inactive: safe interaction disabled"
                            + ", tick=" + debugTickCount
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            return null;
        }

        if (placeCarriedTask != null) {
            debugLogger.state("carried container local task inspect:" + debugTickCount,
                    "carried container local task inspect: tick=" + debugTickCount
                            + ", " + describeLocalPlaceTask()
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            debugLogger.state("carried container task status:" + ContainerTaskDiagnostics.describePos(placeCarriedTask.getPlaced())
                            + ":failed=" + placeCarriedTask.hasFailed()
                            + ":active=" + placeCarriedTask.isActive()
                            + ":finished=" + placeCarriedTask.isFinished(),
                    "carried container task status: placed=" + ContainerTaskDiagnostics.describePos(placeCarriedTask.getPlaced())
                            + ", failed=" + placeCarriedTask.hasFailed()
                            + ", active=" + placeCarriedTask.isActive()
                            + ", finished=" + placeCarriedTask.isFinished()
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            if (placeCarriedTask.isFinished()) {
                //20260729_kpopmodder: Only cache a Carry On placement after the task confirms the carried state cleared.
                Optional<BlockPos> placed = blockValidator.getPlacedContainerIfValid(mod, placeCarriedTask.getPlaced());
                debugLogger.state("carried container finished validation:" + debugTickCount,
                        "carried container finished validation: tick=" + debugTickCount
                                + ", placedCandidate=" + ContainerTaskDiagnostics.describePos(placeCarriedTask.getPlaced())
                                + ", validPlaced=" + ContainerTaskDiagnostics.describeOptionalPos(placed)
                                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                if (placed.isPresent()) {
                    cachedContainerUpdater.accept(placed.get());
                    justPlacedTimerReset.run();
                    debugLogger.state("carried container placed: targetPosition=" + placed.get().toShortString()
                                    + ":context=" + ContainerTaskDiagnostics.describeInputState(mod)
                                    + ":carried=" + ContainerTaskDiagnostics.describeCarriedBlock(mod),
                            "carried container placed: targetPosition=" + placed.get().toShortString()
                                    + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                    placeCarriedTask = null;
                    return null;
                }
                debugLogger.state("carried container placement completed without reachable placed container: "
                        + ContainerTaskDiagnostics.describeInteractionContext(mod));
                placeCarriedTask = null;
                return null;
            }
            if (placeCarriedTask.hasFailed()) {
                debugLogger.state("carried container placement failed:" + debugTickCount,
                        "carried container placement failed, falling back to normal container flow: tick="
                                + debugTickCount
                                + ", " + describeLocalPlaceTask()
                                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                placeCarriedTask = null;
                return null;
            }
            if (placeCarriedTask.isActive() && !placeCarriedTask.isFinished()) {
                debugStateSetter.accept("Placing carried container");
                debugLogger.state("return active carried container task:" + debugTickCount,
                        "return active carried container task: tick=" + debugTickCount
                                + ", " + describeLocalPlaceTask()
                                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                return placeCarriedTask;
            }
        }

        Optional<BlockState> carriedContainer = blockValidator.getCarriedContainerState(mod);
        if (carriedContainer.isEmpty()) {
            debugLogger.state("carried container scan none:" + debugTickCount,
                    "carried container scan none: tick=" + debugTickCount
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            return null;
        }

        cachedContainerUpdater.accept(null);
        justPlacedTimerReset.run();
        placeCarriedTask = new PlaceCarriedBlockTask(containerBlocks);
        localPlaceTaskCreateCount++;
        debugLogger.state("carried container before release:" + debugTickCount,
                "carried container before release: tick=" + debugTickCount
                        + ", createCount=" + localPlaceTaskCreateCount
                        + ", carried=" + carriedContainer.get().getBlock().getTranslationKey()
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        mod.getInputControls().release(Input.SNEAK);
        debugStateSetter.accept("Placing carried container");
        debugLogger.state("detected carried container block:" + carriedContainer.get().getBlock().getTranslationKey()
                        + ":after-release:" + ContainerTaskDiagnostics.describeInputState(mod),
                "detected carried container block: " + carriedContainer.get().getBlock().getTranslationKey()
                        + ", localPlaceTaskCreated=true"
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        return placeCarriedTask;
    }

    private String describeLocalPlaceTask() {
        if (placeCarriedTask == null) {
            return "localTask=none";
        }
        return "localTask=present"
                + ", localTaskPlaced=" + ContainerTaskDiagnostics.describePos(placeCarriedTask.getPlaced())
                + ", localTaskFailed=" + placeCarriedTask.hasFailed()
                + ", localTaskActive=" + placeCarriedTask.isActive()
                + ", localTaskFinished=" + placeCarriedTask.isFinished();
    }
}
