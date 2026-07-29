package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.BotBehaviour;
import adris.altoclef.Debug;
import adris.altoclef.catalogue.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.container.smelt.SmeltContainerSnapshot;
import adris.altoclef.tasks.container.smelt.SmeltFuelPlanner;
import adris.altoclef.tasks.container.smelt.SmeltPlan;
import adris.altoclef.tasks.container.smelt.SmeltSlotPlanner;
import adris.altoclef.tasks.container.smelt.SmeltTransferPolicy;
import adris.altoclef.tasks.resources.CollectFuelTask;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

// Ref
// https://minecraft.gamepedia.com/Smelting

// #20260727_kpopmodder: Added this base task to share smelting container resource flow.
public abstract class AbstractSmeltInContainerTask<T extends AbstractDoSmeltInContainerTask> extends ResourceTask {
    private final SmeltTarget[] targets;
    private final T doTask;
    private final Block containerBlock;
    private final String nearestDebugName;
    private final StateChangeLogger debugLogger;

    protected AbstractSmeltInContainerTask(SmeltTarget[] targets, Block containerBlock, T doTask, String debugLabel, String nearestDebugName) {
        super(extractItemTargets(targets));
        this.targets = targets;
        this.containerBlock = containerBlock;
        this.doTask = doTask;
        this.nearestDebugName = nearestDebugName;
        debugLogger = new StateChangeLogger(debugLabel);
    }

    private static ItemTarget[] extractItemTargets(SmeltTarget[] recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.length);
        for (SmeltTarget target : recipeTargets) {
            result.add(target.getItem());
        }
        return result.toArray(ItemTarget[]::new);
    }

    public void ignoreMaterials() {
        doTask.ignoreMaterials();
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        mod.getBehaviour().push();
        debugLogger.event("start: targets=" + Arrays.toString(targets));
        if (targets.length != 1) {
            Debug.logWarning("Tried smelting multiple targets, only one target is supported at a time!");
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        Optional<BlockPos> containerPos = mod.getBlockScanner().getNearestBlock(containerBlock);
        containerPos.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
        debugLogger.state("resource tick: nearest" + nearestDebugName + "=" + containerPos.map(BlockPos::toShortString).orElse("none"));
        return doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        mod.getBehaviour().pop();
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }
    }

    @Override
    public boolean isFinished() {
        return super.isFinished() || doTask.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other != null && other.getClass() == getClass() && other instanceof AbstractSmeltInContainerTask<?> task) {
            return task.doTask.isEqual(doTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return doTask.toDebugString();
    }

    public SmeltTarget[] getTargets() {
        return targets;
    }

    protected SmeltTarget[] getTargetArray() {
        return targets;
    }
}

// #20260727_kpopmodder: Added this base class to keep the smelting slot algorithm in one place.
abstract class AbstractDoSmeltInContainerTask extends DoStuffInContainerTask {
    private final SmeltTarget target;
    private SmeltContainerSnapshot containerSnapshot = SmeltContainerSnapshot.empty();
    private final SmeltSlotPlanner slotPlanner = new SmeltSlotPlanner();
    private final SmeltFuelPlanner fuelPlanner = new SmeltFuelPlanner();
    private final SmeltTransferPolicy transferPolicy = new SmeltTransferPolicy();
    private final ItemTarget allMaterials;
    private final Class<? extends AbstractFurnaceScreenHandler> screenHandlerClass;
    private final Slot materialSlot;
    private final Slot fuelSlot;
    private final Slot outputSlot;
    private final String containerDebugName;
    private final StateChangeLogger debugLogger;
    private boolean ignoreMaterials;
    private boolean ignoreMaterialsLogged;
    private static final int FUEL_INSERTION_CACHE_GRACE_TICKS = 20 * 3;
    private static final int FUEL_MOVE_TASK_GRACE_TICKS = 20 * 3;
    // #20260727_kpopmodder: Furnace handlers can report a stale fuel slot after a throttled slot move.
    private double pendingInsertedFuelAmount;
    private int pendingInsertedFuelTicks;
    private Task pendingFuelMoveTask;
    private int pendingFuelMoveTicks;

    protected AbstractDoSmeltInContainerTask(
            SmeltTarget target,
            Block containerBlock,
            Item containerItem,
            Class<? extends AbstractFurnaceScreenHandler> screenHandlerClass,
            Slot materialSlot,
            Slot fuelSlot,
            Slot outputSlot,
            String debugLabel,
            String containerDebugName
    ) {
        super(containerBlock, new ItemTarget(containerItem));
        this.target = target;
        this.screenHandlerClass = screenHandlerClass;
        this.materialSlot = materialSlot;
        this.fuelSlot = fuelSlot;
        this.outputSlot = outputSlot;
        this.containerDebugName = containerDebugName;
        debugLogger = new StateChangeLogger(debugLabel);
        allMaterials = new ItemTarget(Stream.concat(Arrays.stream(this.target.getMaterial().getMatches()), Arrays.stream(this.target.getOptionalMaterials())).toArray(Item[]::new), this.target.getMaterial().getTargetCount());
    }

    public void ignoreMaterials() {
        ignoreMaterials = true;
    }

    @Override
    protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
        if (other != null && other.getClass() == getClass() && other instanceof AbstractDoSmeltInContainerTask task) {
            return task.target.equals(target) && task.ignoreMaterials == ignoreMaterials;
        }
        return false;
    }

    @Override
    protected boolean isContainerOpen(AltoClef mod) {
        return screenHandlerClass.isInstance(mod.getPlayer().currentScreenHandler);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BotBehaviour botBehaviour = AltoClef.getInstance().getBehaviour();

        botBehaviour.addProtectedItems(ItemHelper.PLANKS);
        botBehaviour.addProtectedItems(Items.COAL);
        botBehaviour.addProtectedItems(allMaterials.getMatches());
        botBehaviour.addProtectedItems(target.getMaterial().getMatches());
        debugLogger.event("start: output=" + target.getItem() + ", materials=" + allMaterials);
        logIgnoreMaterialsIfNeeded();
    }

    private void logIgnoreMaterialsIfNeeded() {
        // #20260728_kpopmodder: Log ignore-material mode only when this smelt task actually starts, not during food-plan probing.
        if (ignoreMaterials && !ignoreMaterialsLogged) {
            ignoreMaterialsLogged = true;
            debugLogger.event("ignore materials enabled: target=" + target.getItem() + ", material=" + target.getMaterial());
        }
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        tryUpdateOpenContainer(mod);
        updatePendingInsertedFuel(mod);
        ItemTarget materialTarget = allMaterials;
        ItemTarget outputTarget = target.getItem();
        int inventoryOutputCount = mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches());
        int inventoryMaterialCount = mod.getItemStorage().getItemCount(materialTarget.getMatches());
        double inventoryFuelCount = StorageHelper.calculateInventoryFuelCount(mod);
        double totalFuelInContainer = getTotalPlannedFuelInContainer(mod);
        int materialsNeeded = slotPlanner.calculateMaterialsNeeded(materialTarget, outputTarget, inventoryOutputCount, containerSnapshot);
        double fuelNeeded = slotPlanner.calculateFuelNeeded(ignoreMaterials, materialTarget, outputTarget, inventoryOutputCount, totalFuelInContainer, containerSnapshot);

        if (inventoryMaterialCount < materialsNeeded) {
            setDebugState("Getting Materials");
            debugLogger.state("get materials: needed=" + materialsNeeded
                    + ", inventory=" + inventoryMaterialCount
                    + ", materialTarget=" + materialTarget
                    + ", " + describePlan(SmeltPlan.Action.GET_MATERIALS,
                    "inventory material count is below required input",
                    "needed=" + materialsNeeded + ", inventory=" + inventoryMaterialCount + ", materialTarget=" + materialTarget,
                    containerSnapshot));
            return getMaterialTask(target.getMaterial());
        }

        Task pendingFuelMove = getPendingFuelMoveTaskIfActive(mod);
        if (pendingFuelMove != null) {
            setDebugState("Filling fuel");
            debugLogger.state("continue fuel move",
                    "continue fuel move: " + describePlan(SmeltPlan.Action.CONTINUE_FUEL_MOVE,
                            "previous fuel slot transfer is still active",
                            "task=" + pendingFuelMove,
                            containerSnapshot));
            return pendingFuelMove;
        }

        if (containerSnapshot.getBurningFuelCount() <= 0 && inventoryFuelCount < fuelNeeded) {
            if (isPendingFuelInsertActive()) {
                setDebugState("Waiting for fuel slot");
                debugLogger.state("wait for pending fuel insert",
                        "wait for pending fuel insert: " + describePlan(SmeltPlan.Action.WAIT_PENDING_FUEL_INSERT,
                                "fuel slot may not reflect a recent fuel move yet",
                                "needed=" + formatDouble(fuelNeeded) + ", inventoryFuel=" + formatDouble(inventoryFuelCount),
                                containerSnapshot));
                return null;
            }
            setDebugState("Getting Fuel");
            debugLogger.state("get fuel: needed=" + formatDouble(fuelNeeded)
                    + ", inventoryFuel=" + formatDouble(inventoryFuelCount)
                    + ", " + describePlan(SmeltPlan.Action.GET_FUEL,
                    "inventory fuel is below planned fuel need",
                    "needed=" + formatDouble(fuelNeeded) + ", inventoryFuel=" + formatDouble(inventoryFuelCount),
                    containerSnapshot));
            return new CollectFuelTask(fuelNeeded + 1);
        }

        if (StorageHelper.isItemInaccessibleToContainer(mod, allMaterials)) {
            debugLogger.state("move inaccessible materials: " + describePlan(SmeltPlan.Action.MOVE_INACCESSIBLE_MATERIALS,
                    "material stack cannot be inserted into container from its current slot",
                    "materials=" + allMaterials,
                    containerSnapshot));
            return new MoveInaccessibleItemToInventoryTask(allMaterials);
        }

        if (!isContainerOpen(mod)) {
            debugLogger.state("ready for " + containerDebugName + " interaction",
                    "ready for " + containerDebugName + " interaction: " + describePlan(SmeltPlan.Action.READY_FOR_CONTAINER,
                            "inventory/material/fuel gates passed and container is not open",
                            "materialsNeeded=" + materialsNeeded + ", fuelNeeded=" + formatDouble(fuelNeeded),
                            containerSnapshot));
        }
        return super.onTick();
    }

    protected Task getMaterialTask(ItemTarget target) {
        return TaskCatalogue.getItemTask(target);
    }

    @Override
    protected Task containerSubTask(AltoClef mod) {
        tryUpdateOpenContainer(mod);
        ItemStack output = StorageHelper.getItemStackInSlot(outputSlot);
        ItemStack material = StorageHelper.getItemStackInSlot(materialSlot);
        ItemStack fuel = StorageHelper.getItemStackInSlot(fuelSlot);
        SmeltContainerSnapshot currentSnapshot = SmeltContainerSnapshot.of(
                isContainerOpen(mod),
                material,
                fuel,
                output,
                containerSnapshot.getBurningFuelCount(),
                containerSnapshot.getCookProgress()
        );
        containerSnapshot = currentSnapshot;

        double currentlyCachedWhileCooking = fuelPlanner.getCurrentFuelAndCookProgress(mod);
        double needsWhileCooking = material.getCount() - currentlyCachedWhileCooking;
        if (needsWhileCooking <= 0 && !fuel.isEmpty()) {
            debugLogger.state("remove extra fuel: " + describePlan(SmeltPlan.Action.REMOVE_EXTRA_FUEL,
                    "current burn/cook progress covers the remaining material and fuel slot still has fuel",
                    "fuel=" + describeStack(fuel)
                            + ", material=" + describeStack(material)
                            + ", output=" + describeStack(output)
                            + ", needsWhileCooking=" + formatDouble(needsWhileCooking),
                    currentSnapshot));
            if (!transferPolicy.ensureCursorCanStackWith(mod, fuel)) {
                return null;
            }
            mod.getSlotHandler().clickSlot(fuelSlot, 0, SlotActionType.PICKUP);
            return null;
        }

        if (!output.isEmpty()) {
            setDebugState("Receiving Output");
            debugLogger.state("receive output: " + describePlan(SmeltPlan.Action.RECEIVE_OUTPUT,
                    "output slot is not empty",
                    "output=" + describeStack(output)
                            + ", material=" + describeStack(material)
                            + ", fuel=" + describeStack(fuel),
                    currentSnapshot));
            if (!transferPolicy.ensureCursorCanStackWith(mod, output)) {
                return null;
            }
            mod.getSlotHandler().clickSlot(outputSlot, 0, SlotActionType.PICKUP);
            return null;
        }

        ItemTarget materialTarget = allMaterials;
        int outputInventoryCount = mod.getItemStorage().getItemCountInventoryOnly(target.getItem().getMatches());
        int neededMaterialsInSlot = slotPlanner.calculateNeededMaterialsInSlot(
                materialTarget,
                target.getItem(),
                outputInventoryCount,
                currentSnapshot
        );
        if (!allMaterials.matches(material.getItem()) || neededMaterialsInSlot > material.getCount()) {
            int materialsAlreadyIn = (materialTarget.matches(material.getItem()) ? material.getCount() : 0);
            setDebugState("Moving Materials");
            debugLogger.state("move materials into " + containerDebugName + ": " + describePlan(SmeltPlan.Action.MOVE_MATERIALS,
                    "material slot is empty, has the wrong item, or is underfilled",
                    "needInSlot=" + neededMaterialsInSlot
                            + ", alreadyIn=" + materialsAlreadyIn
                            + ", materialSlot=" + describeStack(material)
                            + ", output=" + describeStack(output),
                    currentSnapshot));
            return new MoveItemToSlotFromInventoryTask(new ItemTarget(materialTarget, neededMaterialsInSlot - materialsAlreadyIn), materialSlot);
        }

        if (fuel.isEmpty() || ItemHelper.isFuel(fuel.getItem())) {
            double currentlyCached = fuelPlanner.getCurrentFuelAndCookProgress(mod);
            double needs = material.getCount() - currentlyCached;
            if (needs > 0) {
                ItemStack bestStack = fuelPlanner.getBestFuelStack(mod, needs);
                if (bestStack != null) {
                    setDebugState("Filling fuel");
                    debugLogger.state("fill fuel: " + describePlan(SmeltPlan.Action.FILL_FUEL,
                            "material slot needs more fuel and a supported fuel stack was found",
                            "bestStack=" + describeStack(bestStack)
                                    + ", needs=" + formatDouble(needs)
                                    + ", fuelSlot=" + describeStack(fuel),
                            currentSnapshot));
                    Task fuelMoveTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(bestStack.getItem(), bestStack.getCount()), fuelSlot);
                    rememberPendingFuelMove(fuelMoveTask, bestStack);
                    return fuelMoveTask;
                }
            }
        }

        setDebugState("Waiting...");
        debugLogger.state("wait for smelting",
                "wait for smelting: " + describePlan(SmeltPlan.Action.WAIT_FOR_SMELTING,
                        "no slot transfer is currently required",
                        "material=" + describeStack(material)
                                + ", fuel=" + describeStack(fuel)
                                + ", output=" + describeStack(output),
                        currentSnapshot));
        return null;
    }

    @Override
    protected BlockPos overrideContainerPosition(AltoClef mod) {
        return getTargetContainerPosition();
    }

    protected final boolean hasActiveSmeltingCache() {
        return containerSnapshot.hasActiveSmeltingState();
    }

    private void tryUpdateOpenContainer(AltoClef mod) {
        if (isContainerOpen(mod) && mod.getPlayer().currentScreenHandler instanceof AbstractFurnaceScreenHandler handler) {
            containerSnapshot = SmeltContainerSnapshot.fromOpenContainer(handler, materialSlot, fuelSlot, outputSlot);
        }
    }

    private double getTotalPlannedFuelInContainer(AltoClef mod) {
        return fuelPlanner.calculateTotalPlannedFuelInContainer(
                containerSnapshot,
                pendingInsertedFuelAmount,
                fuelPlanner.getCursorFuelAmount(mod)
        );
    }

    private void notePendingInsertedFuel(ItemStack fuelStack) {
        pendingInsertedFuelAmount = Math.max(pendingInsertedFuelAmount, ItemHelper.getFuelAmount(fuelStack));
        pendingInsertedFuelTicks = FUEL_INSERTION_CACHE_GRACE_TICKS;
    }

    private void rememberPendingFuelMove(Task fuelMoveTask, ItemStack fuelStack) {
        pendingFuelMoveTask = fuelMoveTask;
        pendingFuelMoveTicks = FUEL_MOVE_TASK_GRACE_TICKS;
        notePendingInsertedFuel(fuelStack);
    }

    private Task getPendingFuelMoveTaskIfActive(AltoClef mod) {
        if (pendingFuelMoveTask == null) {
            return null;
        }
        if (!isContainerOpen(mod) || pendingFuelMoveTask.stopped() || pendingFuelMoveTask.isFinished() || pendingFuelMoveTicks <= 0) {
            clearPendingFuelMoveTask();
            return null;
        }
        pendingFuelMoveTicks--;
        return pendingFuelMoveTask;
    }

    private void clearPendingFuelMoveTask() {
        pendingFuelMoveTask = null;
        pendingFuelMoveTicks = 0;
    }

    private boolean isPendingFuelInsertActive() {
        return pendingInsertedFuelAmount > 0 && pendingInsertedFuelTicks > 0;
    }

    private void updatePendingInsertedFuel(AltoClef mod) {
        if (pendingInsertedFuelAmount <= 0) {
            return;
        }
        double cursorFuel = fuelPlanner.getCursorFuelAmount(mod);
        if (cursorFuel > 0) {
            pendingInsertedFuelAmount = Math.max(pendingInsertedFuelAmount, cursorFuel);
            pendingInsertedFuelTicks = FUEL_INSERTION_CACHE_GRACE_TICKS;
            return;
        }
        pendingInsertedFuelTicks--;
        if (pendingInsertedFuelTicks <= 0) {
            clearPendingInsertedFuel();
        }
    }

    private void clearPendingInsertedFuel() {
        pendingInsertedFuelAmount = 0;
        pendingInsertedFuelTicks = 0;
    }

    private String describePlan(SmeltPlan.Action action, String reason, String details, SmeltContainerSnapshot snapshot) {
        return SmeltPlan.of(action, reason, details, snapshot, pendingInsertedFuelAmount, pendingInsertedFuelTicks, pendingFuelMoveTicks).describe();
    }

    private String describeStack(ItemStack stack) {
        return SmeltContainerSnapshot.describeStack(stack);
    }

    private String formatDouble(double value) {
        return SmeltContainerSnapshot.formatDouble(value);
    }
}
