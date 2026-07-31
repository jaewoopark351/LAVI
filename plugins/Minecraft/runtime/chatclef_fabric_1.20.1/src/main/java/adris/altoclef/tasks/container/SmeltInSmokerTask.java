package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.BotBehaviour;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.resources.CollectFuelTask;
import adris.altoclef.tasks.slot.MoveInaccessibleItemToInventoryTask;
import adris.altoclef.tasks.slot.MoveItemToSlotFromInventoryTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import adris.altoclef.util.slots.SmokerSlot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.SmokerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


// Ref
// https://minecraft.gamepedia.com/Smelting

/**
 * Smelt in a smoker, placing a smoker and collecting fuel as needed.
 */
public class SmeltInSmokerTask extends ResourceTask {

    private final SmeltTarget target;

    private final DoSmeltInSmokerTask doTask;

    public SmeltInSmokerTask(SmeltTarget target) {
        super(extractItemTargets(new SmeltTarget[]{target}));
        this.target = target;
        // TODO: Do them in order.
        boolean ignoreMaterials = false;
        doTask = new DoSmeltInSmokerTask(target, ignoreMaterials);
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
        ChatClefDiagnostics.startTrace("smelt_in_smoker_start", this,
                "target", target);
        ChatClefDiagnostics.logEvent("SMELT_SMOKER", "RESOURCE_START", "smelt_in_smoker_resource_start", this,
                "target", target);
        mod.getBehaviour().push();
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        Optional<BlockPos> smokerPos = mod.getBlockScanner().getNearestBlock(Blocks.SMOKER);
        ChatClefDiagnostics.logEvent("SMELT_SMOKER", "RESOURCE_TICK", "smelt_in_smoker_resource_tick", this,
                "target", target,
                "nearestSmokerPresent", smokerPos.isPresent(),
                "nearestSmokerPosition", smokerPos.map(Object::toString).orElse("none"),
                "nearestSmokerState", smokerPos.map(blockPos -> ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(blockPos))).orElse("unavailable"),
                "doTask", ChatClefDiagnostics.taskSummary(doTask));
        smokerPos.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
        return doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "smelt_in_smoker_resource_stop_begin",
                "target", target,
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));
        mod.getBehaviour().pop();
        // Close smoker screen
        ItemStack cursorStack = StorageHelper.getItemStackInCursorSlot();
        if (!cursorStack.isEmpty()) {
            Optional<Slot> moveTo = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursorStack, false);
            moveTo.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            if (ItemHelper.canThrowAwayStack(mod, cursorStack)) {
                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
            }
            Optional<Slot> garbage = StorageHelper.getGarbageSlot(mod);
            // Try throwing away cursor slot if it's garbage
            garbage.ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
        } else {
            StorageHelper.closeScreen();
        }
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "smelt_in_smoker_resource_stop_end",
                "target", target,
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));
    }

    @Override
    public boolean isFinished() {
        return super.isFinished() || doTask.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof SmeltInSmokerTask task) {
            return task.doTask.isEqual(doTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return doTask.toDebugString();
    }

    public SmeltTarget[] getTargets() {
        return new SmeltTarget[]{target};
    }

    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    static class DoSmeltInSmokerTask extends DoStuffInContainerTask {

        private final SmeltTarget _target;
        private final SmokerCache _smokerCache = new SmokerCache();
        private final ItemTarget _allMaterials;
        private boolean _ignoreMaterials;

        public DoSmeltInSmokerTask(SmeltTarget target, boolean ignoreMaterials) {
            super(Blocks.SMOKER, new ItemTarget(Items.SMOKER));
            _target = target;
            _ignoreMaterials = ignoreMaterials;
            _allMaterials = new ItemTarget(Stream.concat(Arrays.stream(_target.getMaterial().getMatches()), Arrays.stream(_target.getOptionalMaterials())).toArray(Item[]::new), _target.getMaterial().getTargetCount());
        }

        public void ignoreMaterials() {
            _ignoreMaterials = true;
        }

        @Override
        protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
            if (other instanceof DoSmeltInSmokerTask task) {
                return task._target.equals(_target) && task._ignoreMaterials == _ignoreMaterials;
            }
            return false;
        }

        @Override
        protected boolean isContainerOpen(AltoClef mod) {
            boolean open = mod.getPlayer().currentScreenHandler instanceof SmokerScreenHandler;
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "CONTAINER_OPEN_CHECK", "smoker_screen_handler_check", this,
                    "isContainerOpen", open,
                    "screenHandler", ChatClefDiagnostics.className(mod.getPlayer().currentScreenHandler));
            return open;
        }

        @Override
        protected void onStart() {
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "DO_START_BEGIN", "do_smelt_in_smoker_start", this,
                    "target", _target,
                    "allMaterials", _allMaterials,
                    "ignoreMaterials", _ignoreMaterials);
            super.onStart();
            BotBehaviour botBehaviour = AltoClef.getInstance().getBehaviour();

            botBehaviour.addProtectedItems(ItemHelper.PLANKS);
            botBehaviour.addProtectedItems(Items.COAL);
            botBehaviour.addProtectedItems(_allMaterials.getMatches());
            botBehaviour.addProtectedItems(_target.getMaterial().getMatches());
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "DO_START_END", "do_smelt_in_smoker_start", this,
                    "target", _target,
                    "allMaterials", _allMaterials,
                    "ignoreMaterials", _ignoreMaterials);
        }

        @Override
        protected Task onTick() {
            AltoClef mod = AltoClef.getInstance();

            tryUpdateOpenSmoker(mod);
            // Include both regular + optional items
            ItemTarget materialTarget = _allMaterials;
            ItemTarget outputTarget = _target.getItem();
            // Materials needed = (mat_target (- 0*mat_in_inventory) - out_in_inventory - mat_in_furnace - out_in_furnace)
            // ^ 0 * mat_in_inventory because we always care aobut the TARGET materials, not how many LEFT there are.
            int materialsNeeded = materialTarget.getTargetCount()
                    /*- mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())*/ // See comment above
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (materialTarget.matches(_smokerCache.materialSlot.getItem()) ? _smokerCache.materialSlot.getCount() : 0)
                    - (outputTarget.matches(_smokerCache.outputSlot.getItem()) ? _smokerCache.outputSlot.getCount() : 0);
            double totalFuelInSmoker = ItemHelper.getFuelAmount(_smokerCache.fuelSlot) + _smokerCache.burningFuelCount + _smokerCache.burnPercentage;
            // Fuel needed = (mat_target - out_in_inventory - out_in_furnace - totalFuelInFurnace)
            double fuelNeeded = _ignoreMaterials
                    ? Math.min(materialTarget.matches(_smokerCache.materialSlot.getItem()) ? _smokerCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                    : materialTarget.getTargetCount()
                    /* - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) */
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (outputTarget.matches(_smokerCache.outputSlot.getItem()) ? _smokerCache.outputSlot.getCount() : 0)
                    - totalFuelInSmoker;
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "DO_TICK_STATE", "do_smelt_in_smoker_material_fuel_state", this,
                    "target", _target,
                    "materialTarget", materialTarget,
                    "outputTarget", outputTarget,
                    "materialsNeeded", materialsNeeded,
                    "fuelNeeded", fuelNeeded,
                    "totalFuelInSmoker", totalFuelInSmoker,
                    "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.materialSlot),
                    "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.fuelSlot),
                    "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.outputSlot),
                    "burningFuelCount", _smokerCache.burningFuelCount,
                    "burnPercentage", _smokerCache.burnPercentage,
                    "inventoryMaterialCount", ChatClefDiagnostics.safeValue(() -> mod.getItemStorage().getItemCount(materialTarget.getMatches())),
                    "inventoryFuelCount", ChatClefDiagnostics.safeValue(() -> StorageHelper.calculateInventoryFuelCount(mod)));

            // We don't have enough materials...
            if (mod.getItemStorage().getItemCount(materialTarget.getMatches()) < materialsNeeded) {
                setDebugState("Getting Materials");
                Task materialTask = getMaterialTask(_target.getMaterial());
                ChatClefDiagnostics.logTaskTransition(this, null, materialTask, "do_smelt_in_smoker_return_material_task",
                        "materialsNeeded", materialsNeeded,
                        "materialTarget", materialTarget);
                return materialTask;
            }

            // We don't have enough fuel...
            if (_smokerCache.burningFuelCount <= 0 && StorageHelper.calculateInventoryFuelCount(mod) < fuelNeeded) {
                setDebugState("Getting Fuel");
                Task fuelTask = new CollectFuelTask(fuelNeeded + 1);
                ChatClefDiagnostics.logTaskTransition(this, null, fuelTask, "do_smelt_in_smoker_return_fuel_task",
                        "fuelNeeded", fuelNeeded,
                        "inventoryFuelCount", StorageHelper.calculateInventoryFuelCount(mod));
                return fuelTask;
            }

            // Make sure our materials are accessible in our inventory
            if (StorageHelper.isItemInaccessibleToContainer(mod, _allMaterials)) {
                Task moveTask = new MoveInaccessibleItemToInventoryTask(_allMaterials);
                ChatClefDiagnostics.logTaskTransition(this, null, moveTask, "do_smelt_in_smoker_return_accessible_material_task",
                        "allMaterials", _allMaterials);
                return moveTask;
            }

            // We have fuel and materials. Get to our container and smelt!
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "DO_TICK_DECISION", "do_smelt_in_smoker_enter_container_flow", this,
                    "materialsNeeded", materialsNeeded,
                    "fuelNeeded", fuelNeeded);
            return super.onTick();
        }

        // Override this if our materials must be acquired in a special way.
        // virtual
        protected Task getMaterialTask(ItemTarget target) {
            return TaskCatalogue.getItemTask(target);
        }

        @Override
        protected Task containerSubTask(AltoClef mod) {
            // We have appropriate materials/fuel.
            /*
             * - If output slot has something, receive it.
             * - Calculate needed material input. If we don't have, put it in.
             * - Calculate needed fuel input. If we don't have, put it in.
             * - Wait lol
             */
            ItemStack output = StorageHelper.getItemStackInSlot(SmokerSlot.OUTPUT_SLOT);
            ItemStack material = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_MATERIALS);
            ItemStack fuel = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_FUEL);
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "CONTAINER_SUBTASK_BEGIN", "smoker_container_subtask_state", this,
                    "outputSlot", ChatClefDiagnostics.itemStackSummary(output),
                    "materialSlot", ChatClefDiagnostics.itemStackSummary(material),
                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                    "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot),
                    "smokerFuel", ChatClefDiagnostics.safeValue(StorageHelper::getSmokerFuel),
                    "smokerCookPercent", ChatClefDiagnostics.safeValue(StorageHelper::getSmokerCookPercent));

            // Receive from output if present
            double currentlyCachedWhileCooking = StorageHelper.getSmokerFuel() + StorageHelper.getSmokerCookPercent();
            double needsWhileCooking = material.getCount() - currentlyCachedWhileCooking;
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "CONTAINER_SUBTASK_COOKING_STATE", "smoker_cooking_state", this,
                    "currentlyCachedWhileCooking", currentlyCachedWhileCooking,
                    "needsWhileCooking", needsWhileCooking);
            if (needsWhileCooking <= 0) {
                if (!fuel.isEmpty()) {
                    ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
                    if (!ItemHelper.canStackTogether(fuel, cursor)) {
                        Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
                        if (toFit.isPresent()) {
                            ChatClefDiagnostics.logSlotClick("REQUEST", "smoker_move_cursor_before_take_fuel", toFit.get(), 0, SlotActionType.PICKUP,
                                    "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor),
                                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel));
                            mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                            return null;
                        } else {
                            // Eh screw it
                            if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                                ChatClefDiagnostics.logSlotClick("REQUEST", "smoker_throw_cursor_before_take_fuel", Slot.UNDEFINED, 0, SlotActionType.PICKUP,
                                        "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor));
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                return null;
                            }
                        }
                    }
                    ChatClefDiagnostics.logSlotClick("REQUEST", "smoker_take_back_fuel_slot", SmokerSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP,
                            "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                            "needsWhileCooking", needsWhileCooking);
                    mod.getSlotHandler().clickSlot(SmokerSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP);
                    return null;
                }
            }
            if (!output.isEmpty()) {
                setDebugState("Receiving Output");
                // Ensure our cursor is empty/can receive our item
                ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
                if (!ItemHelper.canStackTogether(output, cursor)) {
                    Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
                    if (toFit.isPresent()) {
                        ChatClefDiagnostics.logSlotClick("REQUEST", "smoker_move_cursor_before_output", toFit.get(), 0, SlotActionType.PICKUP,
                                "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor),
                                "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
                        mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                        return null;
                    } else {
                        // Eh screw it
                        if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                            ChatClefDiagnostics.logSlotClick("REQUEST", "smoker_throw_cursor_before_output", Slot.UNDEFINED, 0, SlotActionType.PICKUP,
                                    "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor));
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            return null;
                        }
                    }
                }
                // Pick up
                ChatClefDiagnostics.logSlotClick("REQUEST", "smoker_pickup_output", SmokerSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP,
                        "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
                mod.getSlotHandler().clickSlot(SmokerSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP);
                return null;
                // return new MoveItemToSlotTask(new ItemTarget(output.getItem(), output.getCount()), toMoveTo.get(), mod -> FurnaceSlot.OUTPUT_SLOT);
            }

            // Fill in input if needed
            // Materials needed in slot = (mat_target - out_in_inventory - out_in_furnace)
            ItemTarget materialTarget = _allMaterials;

            int neededMaterialsInSlot = materialTarget.getTargetCount()
                    - mod.getItemStorage().getItemCountInventoryOnly(_target.getItem().getMatches())
                    - (_target.getItem().matches(output.getItem()) ? output.getCount() : 0);
            // We don't have the right material or we need more
            if (!_allMaterials.matches(material.getItem()) || neededMaterialsInSlot > material.getCount()) {
                int materialsAlreadyIn = (materialTarget.matches(material.getItem()) ? material.getCount() : 0);
                setDebugState("Moving Materials");
                Task moveMaterialsTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(materialTarget, neededMaterialsInSlot - materialsAlreadyIn), SmokerSlot.INPUT_SLOT_MATERIALS);
                ChatClefDiagnostics.logTaskTransition(this, null, moveMaterialsTask, "smoker_return_move_materials_task",
                        "neededMaterialsInSlot", neededMaterialsInSlot,
                        "materialsAlreadyIn", materialsAlreadyIn,
                        "materialSlot", ChatClefDiagnostics.itemStackSummary(material));
                return moveMaterialsTask;
            }

            /*
            double currentFuel = _ignoreMaterials
                    ? (Math.min(materialTarget.matches(_furnaceCache.materialSlot.getItem()) ? _furnaceCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                    : materialTarget.getTargetCount()
                    - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (outputTarget.matches(_furnaceCache.outputSlot.getItem()) ? _furnaceCache.outputSlot.getCount() : 0)
                    - totalFuelInFurnace;
             */
            // Fill in fuel if needed
            if (fuel.isEmpty() || ItemHelper.isFuel(fuel.getItem())) {
                double currentlyCached = StorageHelper.getSmokerFuel() + StorageHelper.getSmokerCookPercent();
                double needs = material.getCount() - currentlyCached;
                if (needs > 0) {
                    // Get best fuel to fill
                    double closestDelta = Double.NEGATIVE_INFINITY;
                    ItemStack bestStack = null;
                    for (ItemStack stack : mod.getItemStorage().getItemStacksPlayerInventory(true)) {
                        if (mod.getModSettings().isSupportedFuel(stack.getItem())) {
                            double fuelAmount = ItemHelper.getFuelAmount(stack.getItem()) * stack.getCount();
                            double delta = needs - fuelAmount;
                            if (
                                    (bestStack == null) ||
                                            // If our best is above, prioritize lower values
                                            (closestDelta > 0 && delta < closestDelta) ||
                                            // If our best is below, prioritize higher below values
                                            (closestDelta < 0 && delta < 0 && delta > closestDelta)
                            ) {
                                bestStack = stack;
                                closestDelta = delta;
                            }
                        }
                    }
                    if (bestStack != null) {
                        setDebugState("Filling fuel");
                        Task moveFuelTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(bestStack.getItem(), bestStack.getCount()), SmokerSlot.INPUT_SLOT_FUEL);
                        ChatClefDiagnostics.logTaskTransition(this, null, moveFuelTask, "smoker_return_move_fuel_task",
                                "needs", needs,
                                "bestStack", ChatClefDiagnostics.itemStackSummary(bestStack),
                                "closestDelta", closestDelta);
                        return moveFuelTask;
                    }
                }
            }

            setDebugState("Waiting...");
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "CONTAINER_SUBTASK_RETURN", "smoker_waiting_for_cook", this,
                    "materialSlot", ChatClefDiagnostics.itemStackSummary(material),
                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                    "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
            return null;
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (_smokerCache.burnPercentage > 0 || _smokerCache.burningFuelCount > 0 ||
                    _smokerCache.fuelSlot != null || _smokerCache.materialSlot != null ||
                    _smokerCache.outputSlot != null) {
                ChatClefDiagnostics.logEvent("SMELT_SMOKER", "COST_TO_MAKE_NEW", "smoker_cache_non_empty_cost", this,
                        "cost", 9999999.0,
                        "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.materialSlot),
                        "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.fuelSlot),
                        "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.outputSlot));
                return 9999999.0;
            }
            if (mod.getItemStorage().getItemCount(Items.COBBLESTONE) > 8 &&
                    mod.getItemStorage().getItemCount(ItemHelper.LOG) > 4) {
                double cost = 100.0 - 90.0 * (((double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE})
                        / 8.0) + ((double) mod.getItemStorage().getItemCount(ItemHelper.LOG) / 4.0));
                double boundedCost = Math.max(cost, 10.0);
                ChatClefDiagnostics.logEvent("SMELT_SMOKER", "COST_TO_MAKE_NEW", "smoker_inventory_cost", this,
                        "cost", boundedCost,
                        "cobblestoneCount", mod.getItemStorage().getItemCount(Items.COBBLESTONE),
                        "logCount", mod.getItemStorage().getItemCount(ItemHelper.LOG));
                return boundedCost;
            }
            double fallbackCost = StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD) ? 50.0 : 100.0;
            ChatClefDiagnostics.logEvent("SMELT_SMOKER", "COST_TO_MAKE_NEW", "smoker_fallback_cost", this,
                    "cost", fallbackCost,
                    "woodRequirementMet", StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD));
            return fallbackCost;
        }

        @Override
        protected BlockPos overrideContainerPosition(AltoClef mod) {
            // If we have a valid container position, KEEP it.
            return getTargetContainerPosition();
        }

        private void tryUpdateOpenSmoker(AltoClef mod) {
            if (isContainerOpen(mod)) {
                // Update current furnace cache
                _smokerCache.burnPercentage = StorageHelper.getSmokerCookPercent();
                _smokerCache.burningFuelCount = StorageHelper.getSmokerFuel();
                _smokerCache.fuelSlot = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_FUEL);
                _smokerCache.materialSlot = StorageHelper.getItemStackInSlot(SmokerSlot.INPUT_SLOT_MATERIALS);
                _smokerCache.outputSlot = StorageHelper.getItemStackInSlot(SmokerSlot.OUTPUT_SLOT);
                ChatClefDiagnostics.logEvent("SMELT_SMOKER", "CACHE_UPDATE", "smoker_cache_updated_from_open_screen", this,
                        "materialSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.materialSlot),
                        "fuelSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.fuelSlot),
                        "outputSlot", ChatClefDiagnostics.itemStackSummary(_smokerCache.outputSlot),
                        "burningFuelCount", _smokerCache.burningFuelCount,
                        "burnPercentage", _smokerCache.burnPercentage);
            } else {
                ChatClefDiagnostics.logEvent("SMELT_SMOKER", "CACHE_SKIP", "smoker_cache_not_updated_screen_closed", this);
            }
        }
    }

    static class SmokerCache {
        public ItemStack materialSlot = ItemStack.EMPTY;
        public ItemStack fuelSlot = ItemStack.EMPTY;
        public ItemStack outputSlot = ItemStack.EMPTY;
        public double burningFuelCount;
        public double burnPercentage;
    }
}
