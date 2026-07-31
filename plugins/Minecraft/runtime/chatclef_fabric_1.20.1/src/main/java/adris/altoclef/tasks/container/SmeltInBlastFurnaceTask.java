package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.multiversion.versionedfields.Blocks;
import adris.altoclef.multiversion.versionedfields.Items;
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
import adris.altoclef.util.slots.BlastFurnaceSlot;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.BlastFurnaceScreenHandler;
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
 * Smelt in a blast furnace, placing a blast furnace and collecting fuel as needed.
 */
public class SmeltInBlastFurnaceTask extends ResourceTask {

    private final SmeltTarget[] _targets;

    private final DoSmeltInBlastFurnaceTask _doTask;

    public SmeltInBlastFurnaceTask(SmeltTarget[] targets) {
        super(extractItemTargets(targets));
        _targets = targets;
        // TODO: Do them in order.
        _doTask = new DoSmeltInBlastFurnaceTask(targets[0]);
    }

    public SmeltInBlastFurnaceTask(SmeltTarget target) {
        this(new SmeltTarget[]{target});
    }

    private static ItemTarget[] extractItemTargets(SmeltTarget[] recipeTargets) {
        List<ItemTarget> result = new ArrayList<>(recipeTargets.length);
        for (SmeltTarget target : recipeTargets) {
            result.add(target.getItem());
        }
        return result.toArray(ItemTarget[]::new);
    }

    public void ignoreMaterials() {
        _doTask.ignoreMaterials();
    }

    @Override
    protected boolean shouldAvoidPickingUp(AltoClef mod) {
        return false;
    }

    @Override
    protected void onResourceStart(AltoClef mod) {
        ChatClefDiagnostics.startTrace("smelt_in_blast_furnace_start", this,
                "targets", Arrays.toString(_targets));
        ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "RESOURCE_START", "smelt_in_blast_furnace_resource_start", this,
                "targets", Arrays.toString(_targets));
        mod.getBehaviour().push();
        if (_targets.length != 1) {
            Debug.logWarning("Tried smelting multiple targets, only one target is supported at a time!");
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        Optional<BlockPos> blastFurnacePos = mod.getBlockScanner().getNearestBlock(Blocks.BLAST_FURNACE);
        ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "RESOURCE_TICK", "smelt_in_blast_furnace_resource_tick", this,
                "targets", Arrays.toString(_targets),
                "nearestBlastFurnacePresent", blastFurnacePos.isPresent(),
                "nearestBlastFurnacePosition", blastFurnacePos.map(Object::toString).orElse("none"),
                "nearestBlastFurnaceState", blastFurnacePos.map(blockPos -> ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(blockPos))).orElse("unavailable"),
                "doTask", ChatClefDiagnostics.taskSummary(_doTask));
        blastFurnacePos.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
        return _doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "smelt_in_blast_furnace_resource_stop_begin",
                "targets", Arrays.toString(_targets),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));
        mod.getBehaviour().pop();
        // Close blast furnace screen
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
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "smelt_in_blast_furnace_resource_stop_end",
                "targets", Arrays.toString(_targets),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));
    }

    @Override
    public boolean isFinished() {
        return super.isFinished() || _doTask.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof SmeltInBlastFurnaceTask task) {
            return task._doTask.isEqual(_doTask);
        }
        return false;
    }

    @Override
    protected String toDebugStringName() {
        return _doTask.toDebugString();
    }

    public SmeltTarget[] getTargets() {
        return _targets;
    }

    @SuppressWarnings("ConditionCoveredByFurtherCondition")
    static class DoSmeltInBlastFurnaceTask extends DoStuffInContainerTask {

        private final SmeltTarget _target;
        private final BlastFurnaceCache _blastFurnaceCache = new BlastFurnaceCache();
        private final ItemTarget _allMaterials;
        private boolean _ignoreMaterials;

        public DoSmeltInBlastFurnaceTask(SmeltTarget target) {
            super(Blocks.BLAST_FURNACE, new ItemTarget(Items.BLAST_FURNACE));
            _target = target;
            _allMaterials = new ItemTarget(Stream.concat(Arrays.stream(_target.getMaterial().getMatches()), Arrays.stream(_target.getOptionalMaterials())).toArray(Item[]::new), _target.getMaterial().getTargetCount());
        }

        public void ignoreMaterials() {
            _ignoreMaterials = true;
        }

        @Override
        protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
            if (other instanceof DoSmeltInBlastFurnaceTask task) {
                return task._target.equals(_target) && task._ignoreMaterials == _ignoreMaterials;
            }
            return false;
        }

        @Override
        protected boolean isContainerOpen(AltoClef mod) {
            boolean open = mod.getPlayer().currentScreenHandler instanceof BlastFurnaceScreenHandler;
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "CONTAINER_OPEN_CHECK", "blast_furnace_screen_handler_check", this,
                    "isContainerOpen", open,
                    "screenHandler", ChatClefDiagnostics.className(mod.getPlayer().currentScreenHandler));
            return open;
        }

        @Override
        protected void onStart() {
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "DO_START_BEGIN", "do_smelt_in_blast_furnace_start", this,
                    "target", _target,
                    "allMaterials", _allMaterials,
                    "ignoreMaterials", _ignoreMaterials);
            super.onStart();
            AltoClef mod = AltoClef.getInstance();

            mod.getBehaviour().addProtectedItems(ItemHelper.PLANKS);
            mod.getBehaviour().addProtectedItems(Items.COAL);
            mod.getBehaviour().addProtectedItems(_allMaterials.getMatches());
            mod.getBehaviour().addProtectedItems(_target.getMaterial().getMatches());
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "DO_START_END", "do_smelt_in_blast_furnace_start", this,
                    "target", _target,
                    "allMaterials", _allMaterials,
                    "ignoreMaterials", _ignoreMaterials);
        }

        @Override
        protected Task onTick() {
            AltoClef mod = AltoClef.getInstance();

            tryUpdateOpenBlastFurnace(mod);
            // Include both regular + optional items
            ItemTarget materialTarget = _allMaterials;
            ItemTarget outputTarget = _target.getItem();
            // Materials needed = (mat_target (- 0*mat_in_inventory) - out_in_inventory - mat_in_furnace - out_in_furnace)
            // ^ 0 * mat_in_inventory because we always care aobut the TARGET materials, not how many LEFT there are.
            int materialsNeeded = materialTarget.getTargetCount()
                    /*- mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())*/ // See comment above
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (materialTarget.matches(_blastFurnaceCache.materialSlot.getItem()) ? _blastFurnaceCache.materialSlot.getCount() : 0)
                    - (outputTarget.matches(_blastFurnaceCache.outputSlot.getItem()) ? _blastFurnaceCache.outputSlot.getCount() : 0);
            double totalFuelInBlastFurnace = ItemHelper.getFuelAmount(_blastFurnaceCache.fuelSlot) + _blastFurnaceCache.burningFuelCount + _blastFurnaceCache.burnPercentage;
            // Fuel needed = (mat_target - out_in_inventory - out_in_furnace - totalFuelInFurnace)
            double fuelNeeded = _ignoreMaterials
                    ? Math.min(materialTarget.matches(_blastFurnaceCache.materialSlot.getItem()) ? _blastFurnaceCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                    : materialTarget.getTargetCount()
                    /* - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) */
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (outputTarget.matches(_blastFurnaceCache.outputSlot.getItem()) ? _blastFurnaceCache.outputSlot.getCount() : 0)
                    - totalFuelInBlastFurnace;
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "DO_TICK_STATE", "do_smelt_in_blast_furnace_material_fuel_state", this,
                    "target", _target,
                    "materialTarget", materialTarget,
                    "outputTarget", outputTarget,
                    "materialsNeeded", materialsNeeded,
                    "fuelNeeded", fuelNeeded,
                    "totalFuelInBlastFurnace", totalFuelInBlastFurnace,
                    "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.materialSlot),
                    "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.fuelSlot),
                    "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.outputSlot),
                    "burningFuelCount", _blastFurnaceCache.burningFuelCount,
                    "burnPercentage", _blastFurnaceCache.burnPercentage,
                    "inventoryMaterialCount", ChatClefDiagnostics.safeValue(() -> mod.getItemStorage().getItemCount(materialTarget.getMatches())),
                    "inventoryFuelCount", ChatClefDiagnostics.safeValue(() -> StorageHelper.calculateInventoryFuelCount(mod)));

            // We don't have enough materials...
            if (mod.getItemStorage().getItemCount(materialTarget.getMatches()) < materialsNeeded) {
                setDebugState("Getting Materials");
                Task materialTask = getMaterialTask(_target.getMaterial());
                ChatClefDiagnostics.logTaskTransition(this, null, materialTask, "do_smelt_in_blast_furnace_return_material_task",
                        "materialsNeeded", materialsNeeded,
                        "materialTarget", materialTarget);
                return materialTask;
            }

            // We don't have enough fuel...
            if (_blastFurnaceCache.burningFuelCount <= 0 && StorageHelper.calculateInventoryFuelCount(mod) < fuelNeeded) {
                setDebugState("Getting Fuel");
                Task fuelTask = new CollectFuelTask(fuelNeeded + 1);
                ChatClefDiagnostics.logTaskTransition(this, null, fuelTask, "do_smelt_in_blast_furnace_return_fuel_task",
                        "fuelNeeded", fuelNeeded,
                        "inventoryFuelCount", StorageHelper.calculateInventoryFuelCount(mod));
                return fuelTask;
            }

            // Make sure our materials are accessible in our inventory
            if (StorageHelper.isItemInaccessibleToContainer(mod, _allMaterials)) {
                Task moveTask = new MoveInaccessibleItemToInventoryTask(_allMaterials);
                ChatClefDiagnostics.logTaskTransition(this, null, moveTask, "do_smelt_in_blast_furnace_return_accessible_material_task",
                        "allMaterials", _allMaterials);
                return moveTask;
            }

            // We have fuel and materials. Get to our container and smelt!
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "DO_TICK_DECISION", "do_smelt_in_blast_furnace_enter_container_flow", this,
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
            ItemStack output = StorageHelper.getItemStackInSlot(BlastFurnaceSlot.OUTPUT_SLOT);
            ItemStack material = StorageHelper.getItemStackInSlot(BlastFurnaceSlot.INPUT_SLOT_MATERIALS);
            ItemStack fuel = StorageHelper.getItemStackInSlot(BlastFurnaceSlot.INPUT_SLOT_FUEL);
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "CONTAINER_SUBTASK_BEGIN", "blast_furnace_container_subtask_state", this,
                    "outputSlot", ChatClefDiagnostics.itemStackSummary(output),
                    "materialSlot", ChatClefDiagnostics.itemStackSummary(material),
                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                    "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot),
                    "blastFurnaceFuel", ChatClefDiagnostics.safeValue(StorageHelper::getBlastFurnaceFuel),
                    "blastFurnaceCookPercent", ChatClefDiagnostics.safeValue(StorageHelper::getBlastFurnaceCookPercent));

            // Receive from output if present
            double currentlyCachedWhileCooking = StorageHelper.getBlastFurnaceFuel() + StorageHelper.getBlastFurnaceCookPercent();
            double needsWhileCooking = material.getCount() - currentlyCachedWhileCooking;
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "CONTAINER_SUBTASK_COOKING_STATE", "blast_furnace_cooking_state", this,
                    "currentlyCachedWhileCooking", currentlyCachedWhileCooking,
                    "needsWhileCooking", needsWhileCooking);
            if (needsWhileCooking <= 0) {
                if (!fuel.isEmpty()) {
                    ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
                    if (!ItemHelper.canStackTogether(fuel, cursor)) {
                        Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
                        if (toFit.isPresent()) {
                            ChatClefDiagnostics.logSlotClick("REQUEST", "blast_furnace_move_cursor_before_take_fuel", toFit.get(), 0, SlotActionType.PICKUP,
                                    "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor),
                                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel));
                            mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                            return null;
                        } else {
                            // Eh screw it
                            if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                                ChatClefDiagnostics.logSlotClick("REQUEST", "blast_furnace_throw_cursor_before_take_fuel", Slot.UNDEFINED, 0, SlotActionType.PICKUP,
                                        "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor));
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                return null;
                            }
                        }
                    }
                    ChatClefDiagnostics.logSlotClick("REQUEST", "blast_furnace_take_back_fuel_slot", BlastFurnaceSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP,
                            "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                            "needsWhileCooking", needsWhileCooking);
                    mod.getSlotHandler().clickSlot(BlastFurnaceSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP);
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
                        ChatClefDiagnostics.logSlotClick("REQUEST", "blast_furnace_move_cursor_before_output", toFit.get(), 0, SlotActionType.PICKUP,
                                "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor),
                                "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
                        mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                        return null;
                    } else {
                        // Eh screw it
                        if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                            ChatClefDiagnostics.logSlotClick("REQUEST", "blast_furnace_throw_cursor_before_output", Slot.UNDEFINED, 0, SlotActionType.PICKUP,
                                    "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor));
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            return null;
                        }
                    }
                }
                // Pick up
                ChatClefDiagnostics.logSlotClick("REQUEST", "blast_furnace_pickup_output", BlastFurnaceSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP,
                        "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
                mod.getSlotHandler().clickSlot(BlastFurnaceSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP);
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
                Task moveMaterialsTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(materialTarget, neededMaterialsInSlot - materialsAlreadyIn), BlastFurnaceSlot.INPUT_SLOT_MATERIALS);
                ChatClefDiagnostics.logTaskTransition(this, null, moveMaterialsTask, "blast_furnace_return_move_materials_task",
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
                double currentlyCached = StorageHelper.getBlastFurnaceFuel() + StorageHelper.getBlastFurnaceCookPercent();
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
                        Task moveFuelTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(bestStack.getItem(), bestStack.getCount()), BlastFurnaceSlot.INPUT_SLOT_FUEL);
                        ChatClefDiagnostics.logTaskTransition(this, null, moveFuelTask, "blast_furnace_return_move_fuel_task",
                                "needs", needs,
                                "bestStack", ChatClefDiagnostics.itemStackSummary(bestStack),
                                "closestDelta", closestDelta);
                        return moveFuelTask;
                    }
                }
            }

            setDebugState("Waiting...");
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "CONTAINER_SUBTASK_RETURN", "blast_furnace_waiting_for_cook", this,
                    "materialSlot", ChatClefDiagnostics.itemStackSummary(material),
                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                    "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
            return null;
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            if (_blastFurnaceCache.burnPercentage > 0 || _blastFurnaceCache.burningFuelCount > 0 ||
                    _blastFurnaceCache.fuelSlot != null || _blastFurnaceCache.materialSlot != null ||
                    _blastFurnaceCache.outputSlot != null) {
                ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "COST_TO_MAKE_NEW", "blast_furnace_cache_non_empty_cost", this,
                        "cost", 9999999.0,
                        "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.materialSlot),
                        "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.fuelSlot),
                        "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.outputSlot));
                return 9999999.0;
            }
            if (mod.getItemStorage().getItemCount(Items.COBBLESTONE) > 11 &&
                    mod.getItemStorage().getItemCount(Items.RAW_IRON) > 5) {
                double cost = 100.0 - 90.0 * (((double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE})
                        / 8.0) + ((double) mod.getItemStorage().getItemCount(Items.RAW_IRON) / 5.0));
                double boundedCost = Math.max(cost, 10.0);
                ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "COST_TO_MAKE_NEW", "blast_furnace_inventory_cost", this,
                        "cost", boundedCost,
                        "cobblestoneCount", mod.getItemStorage().getItemCount(Items.COBBLESTONE),
                        "rawIronCount", mod.getItemStorage().getItemCount(Items.RAW_IRON));
                return boundedCost;
            }
            double fallbackCost = StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD) ? 50.0 : 100.0;
            ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "COST_TO_MAKE_NEW", "blast_furnace_fallback_cost", this,
                    "cost", fallbackCost,
                    "woodRequirementMet", StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD));
            return fallbackCost;
        }

        @Override
        protected BlockPos overrideContainerPosition(AltoClef mod) {
            // If we have a valid container position, KEEP it.
            return getTargetContainerPosition();
        }

        private void tryUpdateOpenBlastFurnace(AltoClef mod) {
            if (isContainerOpen(mod)) {
                // Update current furnace cache
                _blastFurnaceCache.burnPercentage = StorageHelper.getBlastFurnaceCookPercent();
                _blastFurnaceCache.burningFuelCount = StorageHelper.getBlastFurnaceFuel();
                _blastFurnaceCache.fuelSlot = StorageHelper.getItemStackInSlot(BlastFurnaceSlot.INPUT_SLOT_FUEL);
                _blastFurnaceCache.materialSlot = StorageHelper.getItemStackInSlot(BlastFurnaceSlot.INPUT_SLOT_MATERIALS);
                _blastFurnaceCache.outputSlot = StorageHelper.getItemStackInSlot(BlastFurnaceSlot.OUTPUT_SLOT);
                ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "CACHE_UPDATE", "blast_furnace_cache_updated_from_open_screen", this,
                        "materialSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.materialSlot),
                        "fuelSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.fuelSlot),
                        "outputSlot", ChatClefDiagnostics.itemStackSummary(_blastFurnaceCache.outputSlot),
                        "burningFuelCount", _blastFurnaceCache.burningFuelCount,
                        "burnPercentage", _blastFurnaceCache.burnPercentage);
            } else {
                ChatClefDiagnostics.logEvent("SMELT_BLAST_FURNACE", "CACHE_SKIP", "blast_furnace_cache_not_updated_screen_closed", this);
            }
        }
    }

    static class BlastFurnaceCache {
        public ItemStack materialSlot = ItemStack.EMPTY;
        public ItemStack fuelSlot = ItemStack.EMPTY;
        public ItemStack outputSlot = ItemStack.EMPTY;
        public double burningFuelCount;
        public double burnPercentage;
    }
}
