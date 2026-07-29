package adris.altoclef.tasks.container.access;

import adris.altoclef.AltoClef;
import adris.altoclef.catalogue.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasks.interaction.block.InteractWithBlockTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.time.TimerGame;
import baritone.api.utils.input.Input;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;


//20260730_kpopmodder: Moved this container access coordinator into the access package without behavior changes.
/**
 * Interacts with a container, obtaining and placing one if none were found nearby.
 */
public abstract class DoStuffInContainerTask extends Task {

    private final ItemTarget containerTarget;
    private final Block[] containerBlocks;

    private final PlaceBlockNearbyTask placeTask;
    // If we decided on placing, force place for at least 1 second
    // (originally 10)
    private final TimerGame placeForceTimer = new TimerGame(1);

    // If we just placed something, stop placing and try going to the nearest container.
    private final TimerGame justPlacedTimer = new TimerGame(3);
    private BlockPos cachedContainerPosition = null;
    //20260729_kpopmodder: Keep container fallback diagnostics visible before changing any crafting-table behavior.
    private final StateChangeLogger debugLogger = new StateChangeLogger("DoStuffInContainerTask");
    private final ContainerBlockValidator blockValidator;
    private final ContainerTargetSelector targetSelector;
    private final ContainerPlanLogger planLogger;
    private final ContainerCursorHandler cursorHandler;
    private final CarryOnContainerController carryOnController;
    private int debugTickCount;
    private int safeOpenAttemptCount;

    public DoStuffInContainerTask(Block[] containerBlocks, ItemTarget containerTarget) {
        this.containerBlocks = containerBlocks;
        this.containerTarget = containerTarget;

        placeTask = new PlaceBlockNearbyTask(this.containerBlocks);
        blockValidator = new ContainerBlockValidator(this.containerBlocks, debugLogger);
        targetSelector = new ContainerTargetSelector(this.containerBlocks, blockValidator, debugLogger);
        planLogger = new ContainerPlanLogger(debugLogger);
        cursorHandler = new ContainerCursorHandler(debugLogger);
        carryOnController = new CarryOnContainerController(this.containerBlocks, blockValidator, debugLogger);
    }

    public DoStuffInContainerTask(Block containerBlock, ItemTarget containerTarget) {
        this(new Block[]{containerBlock}, containerTarget);
    }

    @Override
    protected void onStart() {
        AltoClef mod = AltoClef.getInstance();
        mod.getBehaviour().push();

        // Protect container since we might place it.
        mod.getBehaviour().addProtectedItems(ItemHelper.blocksToItems(containerBlocks));
        carryOnController.reset();
        planLogger.reset();
        debugTickCount = 0;
        safeOpenAttemptCount = 0;
        debugLogger.event("start: containerTarget=" + containerTarget
                + ", carryOnSafeSneak=" + carryOnController.shouldUseSafeInteraction()
                + ", normalPlaceTaskActive=" + placeTask.isActive()
                + ", normalPlaceTaskFinished=" + placeTask.isFinished()
                + ", normalPlaced=" + ContainerTaskDiagnostics.describePos(placeTask.getPlaced())
                + ", " + carryOnController.describeStatus(mod)
                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        debugTickCount++;
        debugLogger.state("container tick:" + debugTickCount,
                "container tick=" + debugTickCount
                        + ", containerTarget=" + containerTarget
                        + ", cached=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                        + ", normalPlaceTaskActive=" + placeTask.isActive()
                        + ", normalPlaceTaskFinished=" + placeTask.isFinished()
                        + ", normalPlaced=" + ContainerTaskDiagnostics.describePos(placeTask.getPlaced())
                        + ", " + carryOnController.describeStatus(mod)
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));

        Task carriedPlacement = carryOnController.getCarriedContainerPlacementTask(mod,
                pos -> {
                    BlockPos oldCached = cachedContainerPosition;
                    cachedContainerPosition = pos;
                    debugLogger.event("carried placement cache update: tick=" + debugTickCount
                            + ", oldCached=" + ContainerTaskDiagnostics.describePos(oldCached)
                            + ", newCached=" + ContainerTaskDiagnostics.describePos(pos)
                            + ", " + carryOnController.describeStatus(mod)
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                },
                () -> {
                    debugLogger.event("carried placement justPlacedTimer reset: tick=" + debugTickCount
                            + ", cached=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                            + ", " + carryOnController.describeStatus(mod)
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                    justPlacedTimer.reset();
                },
                this::setDebugState);
        if (carriedPlacement != null) {
            debugLogger.state("container return carried placement:" + debugTickCount,
                    "container return carried placement: tick=" + debugTickCount
                            + ", task=" + ContainerTaskDiagnostics.describeTask(carriedPlacement)
                            + ", cached=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                            + ", " + carryOnController.describeStatus(mod)
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            return carriedPlacement;
        }

        // If we're placing, keep on placing.
        if (placeTask.isActive() && !placeTask.isFinished()) {
            debugLogger.state("normal place task branch:" + debugTickCount,
                    "normal place task branch: tick=" + debugTickCount
                            + ", hasContainerBlockItem=" + mod.getItemStorage().hasItem(ItemHelper.blocksToItems(containerBlocks))
                            + ", normalPlaced=" + ContainerTaskDiagnostics.describePos(placeTask.getPlaced())
                            + ", " + carryOnController.describeStatus(mod)
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            if (mod.getItemStorage().hasItem(ItemHelper.blocksToItems(containerBlocks))) {
                setDebugState("Placing container");
                debugLogger.state("continue placing container:" + debugTickCount,
                        "continue placing container: tick=" + debugTickCount
                                + ", placed=" + ContainerTaskDiagnostics.describePos(placeTask.getPlaced())
                                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                return placeTask;
            }
            if (carryOnController.shouldUseSafeInteraction() && !justPlacedTimer.elapsed()) {
                mod.getInputControls().release(Input.SNEAK);
                setDebugState("Waiting for placed container verification");
                debugLogger.state("wait for carry-on-safe placed container verification:" + debugTickCount,
                        "wait for carry-on-safe placed container verification: tick=" + debugTickCount
                                + ", placed=" + ContainerTaskDiagnostics.describePos(placeTask.getPlaced())
                                + ", justPlacedElapsed=false"
                                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                return null;
            }
        }

        boolean containerOpen = isContainerOpen(mod);
        debugLogger.state("container open check:" + debugTickCount,
                "container open check: tick=" + debugTickCount
                        + ", open=" + containerOpen
                        + ", cached=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                        + ", " + carryOnController.describeStatus(mod)
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        if (containerOpen) {
            debugLogger.state("container open: targetPosition="
                            + ContainerTaskDiagnostics.describePos(cachedContainerPosition),
                    "container open: targetPosition="
                            + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            return containerSubTask(mod);
        }

        BlockPos override = overrideContainerPosition(mod);
        double makeCost = getCostToMakeNew(mod);
        boolean placeForceElapsedBeforePlan = placeForceTimer.elapsed();
        boolean justPlacedElapsedBeforePlan = justPlacedTimer.elapsed();
        ContainerTargetPlan plan = targetSelector.select(mod,
                override,
                placeTask.getPlaced(),
                carryOnController.getCarriedPlacedPosition(),
                makeCost,
                placeForceElapsedBeforePlan,
                justPlacedElapsedBeforePlan);
        planLogger.logPlan(containerTarget,
                plan,
                mod.getItemStorage().hasItem(containerTarget),
                ContainerTaskDiagnostics.describeInteractionContext(mod));

        // Make a new container if going to the container is a pretty bad cost.
        // Also keep on making the container if we're stuck in some
        boolean shouldResetPlaceForceTimer = plan.shouldResetPlaceForceTimer();
        debugLogger.state("container plan branch:" + debugTickCount,
                "container plan branch: tick=" + debugTickCount
                        + ", override=" + ContainerTaskDiagnostics.describePos(override)
                        + ", nearest=" + ContainerTaskDiagnostics.describeOptionalPos(plan.nearest())
                        + ", usingPlacedContainer=" + plan.usingPlacedContainer()
                        + ", actionReason=" + plan.actionReason()
                        + ", walkCost=" + ContainerTaskDiagnostics.formatDouble(plan.walkCost())
                        + ", makeCost=" + ContainerTaskDiagnostics.formatDouble(plan.makeCost())
                        + ", placeForceElapsedBeforePlan=" + placeForceElapsedBeforePlan
                        + ", justPlacedElapsedBeforePlan=" + justPlacedElapsedBeforePlan
                        + ", shouldResetPlaceForceTimer=" + shouldResetPlaceForceTimer
                        + ", " + carryOnController.describeStatus(mod)
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        if (shouldResetPlaceForceTimer) {
            debugLogger.state("container reset place force timer:" + debugTickCount,
                    "container reset place force timer: tick=" + debugTickCount
                            + ", reason=" + plan.actionReason()
                            + ", nearest=" + ContainerTaskDiagnostics.describeOptionalPos(plan.nearest())
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            placeForceTimer.reset();
        }
        boolean placeForceElapsedAfterReset = placeForceTimer.elapsed();
        boolean justPlacedElapsedAfterPlan = justPlacedTimer.elapsed();
        boolean shouldUseNewContainer = plan.shouldUseNewContainer(placeForceElapsedAfterReset, justPlacedElapsedAfterPlan);
        debugLogger.state("container plan decision:" + debugTickCount,
                "container plan decision: tick=" + debugTickCount
                        + ", shouldUseNewContainer=" + shouldUseNewContainer
                        + ", placeForceElapsedAfterReset=" + placeForceElapsedAfterReset
                        + ", justPlacedElapsedAfterPlan=" + justPlacedElapsedAfterPlan
                        + ", hasContainerTarget=" + mod.getItemStorage().hasItem(containerTarget)
                        + ", hasContainerBlockItem=" + mod.getItemStorage().hasItem(ItemHelper.blocksToItems(containerBlocks))
                        + ", reason=" + plan.actionReason()
                        + ", cachedBeforeDecision=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                        + ", " + carryOnController.describeStatus(mod)
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        if (shouldUseNewContainer) {
            // It's cheaper to make a new one, or our only option.

            // We're no longer going to our previous container.
            debugLogger.event("container cache cleared before new container flow: tick=" + debugTickCount
                    + ", oldCached=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                    + ", reason=" + plan.actionReason()
                    + ", shouldUseNewContainer=true"
                    + ", " + carryOnController.describeStatus(mod)
                    + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            cachedContainerPosition = null;

            // Get if we don't have...
            if (!mod.getItemStorage().hasItem(containerTarget)) {
                setDebugState("Getting container item");
                debugLogger.state("get container item:" + debugTickCount,
                        "get container item: tick=" + debugTickCount
                        + ", target=" + containerTarget
                        + ", nearest=" + ContainerTaskDiagnostics.describeOptionalPos(plan.nearest())
                        + ", walkCost=" + ContainerTaskDiagnostics.formatDouble(plan.walkCost())
                        + ", makeCost=" + ContainerTaskDiagnostics.formatDouble(plan.makeCost())
                        + ", reason=" + plan.actionReason()
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
                return TaskCatalogue.getItemTask(containerTarget);
            }

            setDebugState("Placing container...");
            debugLogger.state("place new container:" + debugTickCount,
                    "place new container: tick=" + debugTickCount
                    + ", target=" + containerTarget
                    + ", nearest=" + ContainerTaskDiagnostics.describeOptionalPos(plan.nearest())
                    + ", walkCost=" + ContainerTaskDiagnostics.formatDouble(plan.walkCost())
                    + ", makeCost=" + ContainerTaskDiagnostics.formatDouble(plan.makeCost())
                    + ", previousPlaced=" + ContainerTaskDiagnostics.describePos(placeTask.getPlaced())
                    + ", reason=" + plan.actionReason()
                    + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));

            justPlacedTimer.reset();
            if (carryOnController.shouldUseSafeInteraction()) {
                mod.getInputControls().release(Input.SNEAK);
            }
            // Now place!
            debugLogger.state("container return normal place task:" + debugTickCount,
                    "container return normal place task: tick=" + debugTickCount
                            + ", task=" + ContainerTaskDiagnostics.describeTask(placeTask)
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            return placeTask;
        }

        // This is insanely cursed.
        // TODO: Finish committing to optionals, this is ugly.
        BlockPos previousCached = cachedContainerPosition;
        BlockPos selectedTarget = plan.requireTargetPosition();
        cachedContainerPosition = selectedTarget;
        debugLogger.state("container cached target selected:" + debugTickCount + ":" + selectedTarget.toShortString(),
                "container cached target selected: tick=" + debugTickCount
                        + ", previousCached=" + ContainerTaskDiagnostics.describePos(previousCached)
                        + ", selected=" + selectedTarget.toShortString()
                        + ", usingPlacedContainer=" + plan.usingPlacedContainer()
                        + ", reason=" + plan.actionReason()
                        + ", " + carryOnController.describeStatus(mod)
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));

        // Walk to it and open it

        // Wait for food
        if (mod.getFoodChain().needsToEat()) {
            setDebugState("Waiting for eating...");
            debugLogger.state("wait for eating before opening container:" + debugTickCount,
                    "wait for eating before opening container: tick=" + debugTickCount
                            + ", targetPosition=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            return null;
        }
        setDebugState("Walking to container... " + cachedContainerPosition.toShortString());
        debugLogger.state("walk/open container:" + cachedContainerPosition.toShortString(),
                "walk/open container: targetPosition=" + cachedContainerPosition.toShortString()
                + ", walkCost=" + ContainerTaskDiagnostics.formatDouble(plan.walkCost())
                + ", makeCost=" + ContainerTaskDiagnostics.formatDouble(plan.makeCost())
                + ", usingPlacedContainer=" + plan.usingPlacedContainer()
                + ", cached=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));

        if (cursorHandler.hasCursorItem()) {
            debugLogger.state("container return cursor handler:" + debugTickCount,
                    "container return cursor handler: tick=" + debugTickCount
                            + ", targetPosition=" + cachedContainerPosition.toShortString()
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            return cursorHandler.handleCursorItem(mod);
        }
        if (carryOnController.shouldUseSafeInteraction()) {
            safeOpenAttemptCount++;
            debugLogger.state("carry-on-safe open pre-release:" + debugTickCount + ":" + safeOpenAttemptCount,
                    "carry-on-safe open pre-release: tick=" + debugTickCount
                            + ", attempt=" + safeOpenAttemptCount
                            + ", targetPosition=" + cachedContainerPosition.toShortString()
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
            mod.getInputControls().release(Input.SNEAK);
            debugLogger.state("carry-on-safe open post-release:" + debugTickCount + ":" + safeOpenAttemptCount,
                    "carry-on-safe open post-release: tick=" + debugTickCount
                            + ", attempt=" + safeOpenAttemptCount
                            + ", targetPosition=" + cachedContainerPosition.toShortString()
                            + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        }
        InteractWithBlockTask interactTask = new InteractWithBlockTask(cachedContainerPosition, false);
        debugLogger.state("container interact task created:" + debugTickCount,
                "container interact task created: tick=" + debugTickCount
                        + ", targetPosition=" + cachedContainerPosition.toShortString()
                        + ", shiftClick=false"
                        + ", task=" + ContainerTaskDiagnostics.describeTask(interactTask)
                        + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        return interactTask;
        //return new GetToBlockTask(nearest, true);
    }

    public ItemTarget getContainerTarget() {
        return containerTarget;
    }

    //20260730_kpopmodder: Public adapters preserve old same-package callers after moving this class into access.
    public final boolean isSameContainerTask(Task other) {
        return isEqual(other);
    }

    public final String getContainerDebugString() {
        return toDebugString();
    }

    // Virtual
    protected BlockPos overrideContainerPosition(AltoClef mod) {
        return null;
    }

    protected BlockPos getTargetContainerPosition() {
        return cachedContainerPosition;
    }

    @Override
    protected void onStop(Task interruptTask) {
        AltoClef mod = AltoClef.getInstance();
        debugLogger.event("stop diagnostics: interruptedBy=" + ContainerTaskDiagnostics.describeTask(interruptTask)
                + ", containerTarget=" + containerTarget
                + ", cached=" + ContainerTaskDiagnostics.describePos(cachedContainerPosition)
                + ", placedTask=" + ContainerTaskDiagnostics.describePos(placeTask.getPlaced())
                + ", carriedPlaced=" + ContainerTaskDiagnostics.describePos(carryOnController.getCarriedPlacedPosition())
                + ", placeTaskActive=" + placeTask.isActive()
                + ", placeTaskFinished=" + placeTask.isFinished()
                + ", carriedTaskActive=" + carryOnController.isCarriedTaskActive()
                + ", carriedTaskFinished=" + carryOnController.isCarriedTaskFinished()
                + ", " + ContainerTaskDiagnostics.describeInteractionContext(mod));
        mod.getBehaviour().pop();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DoStuffInContainerTask task) {
            if (!Arrays.equals(task.containerBlocks, containerBlocks)) return false;
            if (!task.containerTarget.equals(containerTarget)) return false;
            return isSubTaskEqual(task);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Doing stuff in " + containerTarget + " container";
    }

    protected abstract boolean isSubTaskEqual(DoStuffInContainerTask other);

    protected abstract boolean isContainerOpen(AltoClef mod);

    protected abstract Task containerSubTask(AltoClef mod);

    protected abstract double getCostToMakeNew(AltoClef mod);
}
