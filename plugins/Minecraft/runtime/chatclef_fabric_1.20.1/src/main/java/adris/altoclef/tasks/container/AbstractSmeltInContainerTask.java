package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.BotBehaviour;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
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
    private final SmeltingContainerCache containerCache = new SmeltingContainerCache();
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
        int materialsNeeded = materialTarget.getTargetCount()
                /*- mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())*/ // See comment above
                - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                - (materialTarget.matches(containerCache.materialSlot.getItem()) ? containerCache.materialSlot.getCount() : 0)
                - (outputTarget.matches(containerCache.outputSlot.getItem()) ? containerCache.outputSlot.getCount() : 0);
        double totalFuelInContainer = getTotalPlannedFuelInContainer(mod);
        double fuelNeeded = ignoreMaterials
                ? Math.min(materialTarget.matches(containerCache.materialSlot.getItem()) ? containerCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                : materialTarget.getTargetCount()
                /* - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) */
                - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                - (outputTarget.matches(containerCache.outputSlot.getItem()) ? containerCache.outputSlot.getCount() : 0)
                - totalFuelInContainer;

        if (mod.getItemStorage().getItemCount(materialTarget.getMatches()) < materialsNeeded) {
            setDebugState("Getting Materials");
            debugLogger.state("get materials: needed=" + materialsNeeded
                    + ", inventory=" + mod.getItemStorage().getItemCount(materialTarget.getMatches())
                    + ", materialTarget=" + materialTarget
                    + ", " + describeCache());
            return getMaterialTask(target.getMaterial());
        }

        Task pendingFuelMove = getPendingFuelMoveTaskIfActive(mod);
        if (pendingFuelMove != null) {
            setDebugState("Filling fuel");
            debugLogger.state("continue fuel move",
                    "continue fuel move: " + describeCache());
            return pendingFuelMove;
        }

        if (containerCache.burningFuelCount <= 0 && StorageHelper.calculateInventoryFuelCount(mod) < fuelNeeded) {
            if (isPendingFuelInsertActive()) {
                setDebugState("Waiting for fuel slot");
                debugLogger.state("wait for pending fuel insert",
                        "wait for pending fuel insert: needed=" + formatDouble(fuelNeeded)
                                + ", inventoryFuel=" + formatDouble(StorageHelper.calculateInventoryFuelCount(mod))
                                + ", " + describeCache());
                return null;
            }
            setDebugState("Getting Fuel");
            debugLogger.state("get fuel: needed=" + formatDouble(fuelNeeded)
                    + ", inventoryFuel=" + formatDouble(StorageHelper.calculateInventoryFuelCount(mod))
                    + ", " + describeCache());
            return new CollectFuelTask(fuelNeeded + 1);
        }

        if (StorageHelper.isItemInaccessibleToContainer(mod, allMaterials)) {
            debugLogger.state("move inaccessible materials: " + allMaterials + ", " + describeCache());
            return new MoveInaccessibleItemToInventoryTask(allMaterials);
        }

        if (!isContainerOpen(mod)) {
            debugLogger.state("ready for " + containerDebugName + " interaction",
                    "ready for " + containerDebugName + " interaction: materialsNeeded=" + materialsNeeded
                    + ", fuelNeeded=" + formatDouble(fuelNeeded)
                    + ", " + describeCache());
        }
        return super.onTick();
    }

    protected Task getMaterialTask(ItemTarget target) {
        return TaskCatalogue.getItemTask(target);
    }

    @Override
    protected Task containerSubTask(AltoClef mod) {
        ItemStack output = StorageHelper.getItemStackInSlot(outputSlot);
        ItemStack material = StorageHelper.getItemStackInSlot(materialSlot);
        ItemStack fuel = StorageHelper.getItemStackInSlot(fuelSlot);

        double currentlyCachedWhileCooking = getCurrentFuelAndCookProgress(mod);
        double needsWhileCooking = material.getCount() - currentlyCachedWhileCooking;
        if (needsWhileCooking <= 0 && !fuel.isEmpty()) {
            debugLogger.state("remove extra fuel: fuel=" + describeStack(fuel)
                    + ", material=" + describeStack(material)
                    + ", output=" + describeStack(output));
            if (!ensureCursorCanStackWith(mod, fuel)) {
                return null;
            }
            mod.getSlotHandler().clickSlot(fuelSlot, 0, SlotActionType.PICKUP);
            return null;
        }

        if (!output.isEmpty()) {
            setDebugState("Receiving Output");
            debugLogger.state("receive output: output=" + describeStack(output)
                    + ", material=" + describeStack(material)
                    + ", fuel=" + describeStack(fuel));
            if (!ensureCursorCanStackWith(mod, output)) {
                return null;
            }
            mod.getSlotHandler().clickSlot(outputSlot, 0, SlotActionType.PICKUP);
            return null;
        }

        ItemTarget materialTarget = allMaterials;
        int neededMaterialsInSlot = materialTarget.getTargetCount()
                - mod.getItemStorage().getItemCountInventoryOnly(target.getItem().getMatches())
                - (target.getItem().matches(output.getItem()) ? output.getCount() : 0);
        if (!allMaterials.matches(material.getItem()) || neededMaterialsInSlot > material.getCount()) {
            int materialsAlreadyIn = (materialTarget.matches(material.getItem()) ? material.getCount() : 0);
            setDebugState("Moving Materials");
            debugLogger.state("move materials into " + containerDebugName + ": needInSlot=" + neededMaterialsInSlot
                    + ", alreadyIn=" + materialsAlreadyIn
                    + ", materialSlot=" + describeStack(material)
                    + ", output=" + describeStack(output));
            return new MoveItemToSlotFromInventoryTask(new ItemTarget(materialTarget, neededMaterialsInSlot - materialsAlreadyIn), materialSlot);
        }

        if (fuel.isEmpty() || ItemHelper.isFuel(fuel.getItem())) {
            double currentlyCached = getCurrentFuelAndCookProgress(mod);
            double needs = material.getCount() - currentlyCached;
            if (needs > 0) {
                ItemStack bestStack = getBestFuelStack(mod, needs);
                if (bestStack != null) {
                    setDebugState("Filling fuel");
                    debugLogger.state("fill fuel: bestStack=" + describeStack(bestStack)
                            + ", needs=" + formatDouble(needs)
                            + ", fuelSlot=" + describeStack(fuel));
                    Task fuelMoveTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(bestStack.getItem(), bestStack.getCount()), fuelSlot);
                    rememberPendingFuelMove(fuelMoveTask, bestStack);
                    return fuelMoveTask;
                }
            }
        }

        setDebugState("Waiting...");
        debugLogger.state("wait for smelting",
                "wait for smelting: material=" + describeStack(material)
                + ", fuel=" + describeStack(fuel)
                + ", output=" + describeStack(output));
        return null;
    }

    @Override
    protected BlockPos overrideContainerPosition(AltoClef mod) {
        return getTargetContainerPosition();
    }

    protected final boolean hasActiveSmeltingCache() {
        return containerCache.burnPercentage > 0 || containerCache.burningFuelCount > 0 ||
                isNotEmpty(containerCache.fuelSlot) || isNotEmpty(containerCache.materialSlot) ||
                isNotEmpty(containerCache.outputSlot);
    }

    private boolean isNotEmpty(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    private void tryUpdateOpenContainer(AltoClef mod) {
        if (isContainerOpen(mod) && mod.getPlayer().currentScreenHandler instanceof AbstractFurnaceScreenHandler handler) {
            containerCache.burnPercentage = StorageHelper.getFurnaceCookPercent(handler);
            containerCache.burningFuelCount = StorageHelper.getFurnaceFuel(handler);
            containerCache.fuelSlot = StorageHelper.getItemStackInSlot(fuelSlot);
            containerCache.materialSlot = StorageHelper.getItemStackInSlot(materialSlot);
            containerCache.outputSlot = StorageHelper.getItemStackInSlot(outputSlot);
        }
    }

    private double getTotalPlannedFuelInContainer(AltoClef mod) {
        return ItemHelper.getFuelAmount(containerCache.fuelSlot)
                + containerCache.burningFuelCount
                + containerCache.burnPercentage
                + pendingInsertedFuelAmount
                + getCursorFuelAmount(mod);
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
        double cursorFuel = getCursorFuelAmount(mod);
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

    private double getCurrentFuelAndCookProgress(AltoClef mod) {
        if (mod.getPlayer().currentScreenHandler instanceof AbstractFurnaceScreenHandler handler) {
            return StorageHelper.getFurnaceFuel(handler) + StorageHelper.getFurnaceCookPercent(handler);
        }
        return 0;
    }

    private double getCursorFuelAmount(AltoClef mod) {
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (cursor.isEmpty() || !mod.getModSettings().isSupportedFuel(cursor.getItem())) {
            return 0;
        }
        return ItemHelper.getFuelAmount(cursor);
    }

    private boolean ensureCursorCanStackWith(AltoClef mod, ItemStack targetStack) {
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (ItemHelper.canStackTogether(targetStack, cursor)) {
            return true;
        }
        Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
        if (toFit.isPresent()) {
            mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
            return false;
        }
        if (ItemHelper.canThrowAwayStack(mod, cursor)) {
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            return false;
        }
        return true;
    }

    private ItemStack getBestFuelStack(AltoClef mod, double needs) {
        double closestDelta = Double.NEGATIVE_INFINITY;
        ItemStack bestStack = null;
        for (ItemStack stack : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
            if (mod.getModSettings().isSupportedFuel(stack.getItem())) {
                double fuelAmount = ItemHelper.getFuelAmount(stack.getItem()) * stack.getCount();
                double delta = needs - fuelAmount;
                if (
                        (bestStack == null) ||
                                (closestDelta > 0 && delta < closestDelta) ||
                                (delta < 0 && delta > closestDelta)
                ) {
                    bestStack = stack;
                    closestDelta = delta;
                }
            }
        }
        return bestStack;
    }

    private String describeCache() {
        return "cache[input=" + describeStack(containerCache.materialSlot)
                + ", fuel=" + describeStack(containerCache.fuelSlot)
                + ", output=" + describeStack(containerCache.outputSlot)
                + ", burningFuel=" + formatDouble(containerCache.burningFuelCount)
                + ", cook=" + formatDouble(containerCache.burnPercentage)
                + ", pendingFuel=" + formatDouble(pendingInsertedFuelAmount)
                + ", pendingFuelTicks=" + pendingInsertedFuelTicks
                + ", pendingFuelMoveTicks=" + pendingFuelMoveTicks
                + "]";
    }

    private String describeStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getItem().getTranslationKey() + " x " + stack.getCount();
    }

    private String formatDouble(double value) {
        if (Double.isInfinite(value)) {
            return "infinity";
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static class SmeltingContainerCache {
        private ItemStack materialSlot = ItemStack.EMPTY;
        private ItemStack fuelSlot = ItemStack.EMPTY;
        private ItemStack outputSlot = ItemStack.EMPTY;
        private double burningFuelCount = 0;
        private double burnPercentage = 0;
    }
}
