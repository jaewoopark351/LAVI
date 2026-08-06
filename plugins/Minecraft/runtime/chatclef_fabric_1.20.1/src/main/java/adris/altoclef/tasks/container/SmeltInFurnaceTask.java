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
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.SmeltTarget;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.FurnaceSlot;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.furnace.FurnaceContainerDiagnostics;
import lavi.minecraft.diagnostics.tasktrace.VisibleTaskDiagnostics;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.FurnaceScreenHandler;
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
 * Smelt in a furnace, placing a furnace and collecting fuel as needed.
 */
public class SmeltInFurnaceTask extends ResourceTask {
    private final SmeltTarget[] _targets;

    private final DoSmeltInFurnaceTask _doTask;

    public SmeltInFurnaceTask(SmeltTarget[] targets) {
        super(extractItemTargets(targets));
        _targets = targets;
        // TODO: Do them in order.
        _doTask = new DoSmeltInFurnaceTask(targets[0]);
    }

    public SmeltInFurnaceTask(SmeltTarget target) {
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
        VisibleTaskDiagnostics.logLifecycle(mod, this, "START", "smelt_in_furnace_resource_start",
                "targets", Arrays.toString(_targets));
        ChatClefDiagnostics.startTrace("smelt_in_furnace_start", this,
                "targets", Arrays.toString(_targets));
        ChatClefDiagnostics.logEvent("SMELT_FURNACE", "RESOURCE_START", "smelt_in_furnace_resource_start", this,
                "targets", Arrays.toString(_targets));
        mod.getBehaviour().push();
        if (_targets.length != 1) {
            Debug.logWarning("Tried smelting multiple targets, only one target is supported at a time!");
        }
    }

    @Override
    protected Task onResourceTick(AltoClef mod) {
        Optional<BlockPos> furnacePos = mod.getBlockScanner().getNearestBlock(Blocks.FURNACE);
        ChatClefDiagnostics.logEvent("SMELT_FURNACE", "RESOURCE_TICK", "smelt_in_furnace_resource_tick", this,
                "targets", Arrays.toString(_targets),
                "nearestFurnacePresent", furnacePos.isPresent(),
                "nearestFurnacePosition", furnacePos.map(Object::toString).orElse("none"),
                "nearestFurnaceState", furnacePos.map(blockPos -> ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(blockPos))).orElse("unavailable"),
                "doTask", ChatClefDiagnostics.taskSummary(_doTask));
        furnacePos.ifPresent(blockPos -> mod.getBehaviour().avoidBlockBreaking(blockPos));
        VisibleTaskDiagnostics.logReturnTask(mod, this, _doTask, "smelt_in_furnace_return_do_task",
                "furnace=" + furnacePos.map(Object::toString).orElse("none"),
                "targets", Arrays.toString(_targets),
                "nearestFurnacePresent", furnacePos.isPresent(),
                "nearestFurnacePosition", furnacePos.map(Object::toString).orElse("none"));
        return _doTask;
    }

    @Override
    protected void onResourceStop(AltoClef mod, Task interruptTask) {
        VisibleTaskDiagnostics.logLifecycle(mod, this, "STOP_BEGIN", "smelt_in_furnace_resource_stop",
                "targets", Arrays.toString(_targets),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "smelt_in_furnace_resource_stop_begin",
                "targets", Arrays.toString(_targets),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));
        mod.getBehaviour().pop();
        // Close furnace screen
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
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "smelt_in_furnace_resource_stop_end",
                "targets", Arrays.toString(_targets),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));
        VisibleTaskDiagnostics.logLifecycle(mod, this, "STOP_END", "smelt_in_furnace_resource_stop",
                "targets", Arrays.toString(_targets),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
    }

    @Override
    public boolean isFinished() {
        return super.isFinished() || _doTask.isFinished();
    }

    @Override
    protected boolean isEqualResource(ResourceTask other) {
        if (other instanceof SmeltInFurnaceTask task) {
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


    static class DoSmeltInFurnaceTask extends DoStuffInContainerTask {

        private final SmeltTarget target;
        private final FurnaceCache furnaceCache = new FurnaceCache();
        private final ItemTarget allMaterials;
        private boolean ignoreMaterials;

        public DoSmeltInFurnaceTask(SmeltTarget target) {
            super(Blocks.FURNACE, new ItemTarget(Items.FURNACE));
            this.target = target;
            allMaterials = new ItemTarget(Stream.concat(Arrays.stream(this.target.getMaterial().getMatches()), Arrays.stream(this.target.getOptionalMaterials())).toArray(Item[]::new), this.target.getMaterial().getTargetCount());
        }

        public void ignoreMaterials() {
            ignoreMaterials = true;
        }

        @Override
        protected boolean isSubTaskEqual(DoStuffInContainerTask other) {
            if (other instanceof DoSmeltInFurnaceTask task) {
                return task.target.equals(target) && task.ignoreMaterials == ignoreMaterials;
            }
            return false;
        }

        @Override
        protected boolean isContainerOpen(AltoClef mod) {
            boolean open = mod.getPlayer().currentScreenHandler instanceof FurnaceScreenHandler;
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "CONTAINER_OPEN_CHECK", "furnace_screen_handler_check", this,
                    "isContainerOpen", open,
                    "screenHandler", ChatClefDiagnostics.className(mod.getPlayer().currentScreenHandler));
            return open;
        }

        @Override
        protected void onStart() {
            VisibleTaskDiagnostics.logLifecycle(AltoClef.getInstance(), this, "START", "do_smelt_in_furnace_start",
                    "target", target,
                    "allMaterials", allMaterials,
                    "ignoreMaterials", ignoreMaterials);
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "DO_START_BEGIN", "do_smelt_in_furnace_start", this,
                    "target", target,
                    "allMaterials", allMaterials,
                    "ignoreMaterials", ignoreMaterials);
            super.onStart();
            BotBehaviour botBehaviour = AltoClef.getInstance().getBehaviour();

            botBehaviour.addProtectedItems(ItemHelper.PLANKS);
            botBehaviour.addProtectedItems(Items.COAL);
            botBehaviour.addProtectedItems(allMaterials.getMatches());
            botBehaviour.addProtectedItems(target.getMaterial().getMatches());
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "DO_START_END", "do_smelt_in_furnace_start", this,
                    "target", target,
                    "allMaterials", allMaterials,
                    "ignoreMaterials", ignoreMaterials);
        }

        @Override
        protected Task onTick() {
            AltoClef mod = AltoClef.getInstance();

            tryUpdateOpenFurnace(mod);
            // Include both regular + optional items
            ItemTarget materialTarget = allMaterials;
            ItemTarget outputTarget = target.getItem();
            // Materials needed = (mat_target (- 0*mat_in_inventory) - out_in_inventory - mat_in_furnace - out_in_furnace)
            // ^ 0 * mat_in_inventory because we always care aobut the TARGET materials, not how many LEFT there are.
            int materialsNeeded = materialTarget.getTargetCount()
                    /*- mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches())*/ // See comment above
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (materialTarget.matches(furnaceCache.materialSlot.getItem()) ? furnaceCache.materialSlot.getCount() : 0)
                    - (outputTarget.matches(furnaceCache.outputSlot.getItem()) ? furnaceCache.outputSlot.getCount() : 0);
            double totalFuelInFurnace = ItemHelper.getFuelAmount(furnaceCache.fuelSlot) + furnaceCache.burningFuelCount + furnaceCache.burnPercentage;
            // Fuel needed = (mat_target - out_in_inventory - out_in_furnace - totalFuelInFurnace)
            double fuelNeeded = ignoreMaterials
                    ? Math.min(materialTarget.matches(furnaceCache.materialSlot.getItem()) ? furnaceCache.materialSlot.getCount() : 0, materialTarget.getTargetCount())
                    : materialTarget.getTargetCount()
                    /* - mod.getItemStorage().getItemCountInventoryOnly(materialTarget.getMatches()) */
                    - mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    - (outputTarget.matches(furnaceCache.outputSlot.getItem()) ? furnaceCache.outputSlot.getCount() : 0)
                    - totalFuelInFurnace;
            int inventoryMaterialCount = mod.getItemStorage().getItemCount(materialTarget.getMatches());
            int inventoryOutputCount = ChatClefDiagnostics.isBoundaryEnabled()
                    ? mod.getItemStorage().getItemCountInventoryOnly(outputTarget.getMatches())
                    : -1;
            double inventoryFuelCount = StorageHelper.calculateInventoryFuelCount(mod);
            boolean materialGateSatisfied = inventoryMaterialCount >= materialsNeeded;
            boolean fuelGateSatisfied = furnaceCache.burningFuelCount > 0 || inventoryFuelCount >= fuelNeeded;
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "DO_TICK_STATE", "do_smelt_in_furnace_material_fuel_state", this,
                    "target", target,
                    "materialTarget", materialTarget,
                    "outputTarget", outputTarget,
                    "materialsNeeded", materialsNeeded,
                    "fuelNeeded", fuelNeeded,
                    "totalFuelInFurnace", totalFuelInFurnace,
                    "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.materialSlot),
                    "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.fuelSlot),
                    "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.outputSlot),
                    "burningFuelCount", furnaceCache.burningFuelCount,
                    "burnPercentage", furnaceCache.burnPercentage,
                    "inventoryMaterialCount", inventoryMaterialCount,
                    "inventoryFuelCount", inventoryFuelCount);

            // We don't have enough materials...
            if (inventoryMaterialCount < materialsNeeded) {
                setDebugState("Getting Materials");
                Task materialTask = getMaterialTask(target.getMaterial());
                FurnaceContainerDiagnostics.logOperationGate(mod,
                        this,
                        "GET_MATERIAL",
                        inventoryMaterialCount,
                        materialsNeeded,
                        false,
                        inventoryFuelCount,
                        fuelNeeded,
                        fuelGateSatisfied,
                        "not_evaluated_material_missing",
                        false,
                        inventoryOutputCount);
                VisibleTaskDiagnostics.logReturnTask(mod, this, materialTask, "do_smelt_in_furnace_return_material_task",
                        "materialsNeeded=" + materialsNeeded + "|target=" + materialTarget,
                        "materialsNeeded", materialsNeeded,
                        "materialTarget", materialTarget,
                        "inventoryMaterialCount", inventoryMaterialCount);
                ChatClefDiagnostics.logTaskTransition(this, null, materialTask, "do_smelt_in_furnace_return_material_task",
                        "materialsNeeded", materialsNeeded,
                        "materialTarget", materialTarget);
                return materialTask;
            }

            // We don't have enough fuel...
            if (furnaceCache.burningFuelCount <= 0 && inventoryFuelCount < fuelNeeded) {
                setDebugState("Getting Fuel");
                Task fuelTask = new CollectFuelTask(fuelNeeded + 1);
                FurnaceContainerDiagnostics.logOperationGate(mod,
                        this,
                        "GET_FUEL",
                        inventoryMaterialCount,
                        materialsNeeded,
                        materialGateSatisfied,
                        inventoryFuelCount,
                        fuelNeeded,
                        false,
                        "not_evaluated_fuel_missing",
                        false,
                        inventoryOutputCount);
                VisibleTaskDiagnostics.logReturnTask(mod, this, fuelTask, "do_smelt_in_furnace_return_fuel_task",
                        "fuelNeeded=" + fuelNeeded,
                        "fuelNeeded", fuelNeeded,
                        "inventoryFuelCount", inventoryFuelCount);
                ChatClefDiagnostics.logTaskTransition(this, null, fuelTask, "do_smelt_in_furnace_return_fuel_task",
                        "fuelNeeded", fuelNeeded,
                        "inventoryFuelCount", inventoryFuelCount);
                return fuelTask;
            }

            // Make sure our materials are accessible in our inventory
            boolean materialsInaccessible = StorageHelper.isItemInaccessibleToContainer(mod, allMaterials);
            if (materialsInaccessible) {
                Task moveTask = new MoveInaccessibleItemToInventoryTask(allMaterials);
                FurnaceContainerDiagnostics.logOperationGate(mod,
                        this,
                        "MOVE_ACCESSIBLE_MATERIAL",
                        inventoryMaterialCount,
                        materialsNeeded,
                        materialGateSatisfied,
                        inventoryFuelCount,
                        fuelNeeded,
                        fuelGateSatisfied,
                        false,
                        false,
                        inventoryOutputCount);
                VisibleTaskDiagnostics.logReturnTask(mod, this, moveTask, "do_smelt_in_furnace_return_accessible_material_task",
                        "allMaterials=" + allMaterials,
                        "allMaterials", allMaterials);
                ChatClefDiagnostics.logTaskTransition(this, null, moveTask, "do_smelt_in_furnace_return_accessible_material_task",
                        "allMaterials", allMaterials);
                return moveTask;
            }

            // We have fuel and materials. Get to our container and smelt!
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "DO_TICK_DECISION", "do_smelt_in_furnace_enter_container_flow", this,
                    "materialsNeeded", materialsNeeded,
                    "fuelNeeded", fuelNeeded);
            FurnaceContainerDiagnostics.logOperationGate(mod,
                    this,
                    "ENTER_CONTAINER_FLOW",
                    inventoryMaterialCount,
                    materialsNeeded,
                    materialGateSatisfied,
                    inventoryFuelCount,
                    fuelNeeded,
                    fuelGateSatisfied,
                    true,
                    true,
                    inventoryOutputCount);
            VisibleTaskDiagnostics.logDecision(mod, this, "do_smelt_in_furnace_enter_container_flow",
                    "materialsNeeded=" + materialsNeeded + "|fuelNeeded=" + fuelNeeded,
                    "materialsNeeded", materialsNeeded,
                    "fuelNeeded", fuelNeeded,
                    "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.materialSlot),
                    "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.fuelSlot),
                    "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.outputSlot));
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
            ItemStack output = StorageHelper.getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
            ItemStack material = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS);
            ItemStack fuel = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_FUEL);
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "CONTAINER_SUBTASK_BEGIN", "furnace_container_subtask_state", this,
                    "outputSlot", ChatClefDiagnostics.itemStackSummary(output),
                    "materialSlot", ChatClefDiagnostics.itemStackSummary(material),
                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                    "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot),
                    "furnaceFuel", ChatClefDiagnostics.safeValue(StorageHelper::getFurnaceFuel),
                    "furnaceCookPercent", ChatClefDiagnostics.safeValue(StorageHelper::getFurnaceCookPercent));

            // Receive from output if present
            double currentlyCachedWhileCooking = StorageHelper.getFurnaceFuel() + StorageHelper.getFurnaceCookPercent();
            double needsWhileCooking = material.getCount() - currentlyCachedWhileCooking;
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "CONTAINER_SUBTASK_COOKING_STATE", "furnace_cooking_state", this,
                    "currentlyCachedWhileCooking", currentlyCachedWhileCooking,
                    "needsWhileCooking", needsWhileCooking);
            if (needsWhileCooking <= 0) {
                if (!fuel.isEmpty()) {
                    ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
                    if (!ItemHelper.canStackTogether(fuel, cursor)) {
                        Optional<Slot> toFit = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(cursor, false);
                        if (toFit.isPresent()) {
                            ChatClefDiagnostics.logSlotClick("REQUEST", "furnace_move_cursor_before_take_fuel", toFit.get(), 0, SlotActionType.PICKUP,
                                    "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor),
                                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel));
                            mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                            return null;
                        } else {
                            // Eh screw it
                            if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                                ChatClefDiagnostics.logSlotClick("REQUEST", "furnace_throw_cursor_before_take_fuel", Slot.UNDEFINED, 0, SlotActionType.PICKUP,
                                        "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor));
                                mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                                return null;
                            }
                        }
                    }
                    ChatClefDiagnostics.logSlotClick("REQUEST", "furnace_take_back_fuel_slot", FurnaceSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP,
                            "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                            "needsWhileCooking", needsWhileCooking);
                    mod.getSlotHandler().clickSlot(FurnaceSlot.INPUT_SLOT_FUEL, 0, SlotActionType.PICKUP);
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
                        ChatClefDiagnostics.logSlotClick("REQUEST", "furnace_move_cursor_before_output", toFit.get(), 0, SlotActionType.PICKUP,
                                "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor),
                                "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
                        mod.getSlotHandler().clickSlot(toFit.get(), 0, SlotActionType.PICKUP);
                        return null;
                    } else {
                        // Eh screw it
                        if (ItemHelper.canThrowAwayStack(mod, cursor)) {
                            ChatClefDiagnostics.logSlotClick("REQUEST", "furnace_throw_cursor_before_output", Slot.UNDEFINED, 0, SlotActionType.PICKUP,
                                    "cursorStack", ChatClefDiagnostics.itemStackSummary(cursor));
                            mod.getSlotHandler().clickSlot(Slot.UNDEFINED, 0, SlotActionType.PICKUP);
                            return null;
                        }
                    }
                }
                // Pick up
                ChatClefDiagnostics.logSlotClick("REQUEST", "furnace_pickup_output", FurnaceSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP,
                        "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
                mod.getSlotHandler().clickSlot(FurnaceSlot.OUTPUT_SLOT, 0, SlotActionType.PICKUP);
                return null;
                // return new MoveItemToSlotTask(new ItemTarget(output.getItem(), output.getCount()), toMoveTo.get(), mod -> FurnaceSlot.OUTPUT_SLOT);
            }

            // Fill in input if needed
            // Materials needed in slot = (mat_target - out_in_inventory - out_in_furnace)
            ItemTarget materialTarget = allMaterials;

            int neededMaterialsInSlot = materialTarget.getTargetCount()
                    - mod.getItemStorage().getItemCountInventoryOnly(target.getItem().getMatches())
                    - (target.getItem().matches(output.getItem()) ? output.getCount() : 0);
            // We don't have the right material or we need more
            if (!allMaterials.matches(material.getItem()) || neededMaterialsInSlot > material.getCount()) {
                int materialsAlreadyIn = (materialTarget.matches(material.getItem()) ? material.getCount() : 0);
                setDebugState("Moving Materials");
                Task moveMaterialsTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(materialTarget, neededMaterialsInSlot - materialsAlreadyIn), FurnaceSlot.INPUT_SLOT_MATERIALS);
                VisibleTaskDiagnostics.logReturnTask(mod, this, moveMaterialsTask, "furnace_return_move_materials_task",
                        "neededMaterialsInSlot=" + neededMaterialsInSlot + "|materialsAlreadyIn=" + materialsAlreadyIn,
                        "neededMaterialsInSlot", neededMaterialsInSlot,
                        "materialsAlreadyIn", materialsAlreadyIn,
                        "materialSlot", ChatClefDiagnostics.itemStackSummary(material));
                ChatClefDiagnostics.logTaskTransition(this, null, moveMaterialsTask, "furnace_return_move_materials_task",
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
                double currentlyCached = StorageHelper.getFurnaceFuel() + StorageHelper.getFurnaceCookPercent();
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
                                            (delta < 0 && delta > closestDelta)
                            ) {
                                bestStack = stack;
                                closestDelta = delta;
                            }
                        }
                    }
                    if (bestStack != null) {
                        setDebugState("Filling fuel");
                        Task moveFuelTask = new MoveItemToSlotFromInventoryTask(new ItemTarget(bestStack.getItem(), bestStack.getCount()), FurnaceSlot.INPUT_SLOT_FUEL);
                        VisibleTaskDiagnostics.logReturnTask(mod, this, moveFuelTask, "furnace_return_move_fuel_task",
                                "needs=" + needs + "|bestStack=" + ChatClefDiagnostics.itemStackSummary(bestStack),
                                "needs", needs,
                                "bestStack", ChatClefDiagnostics.itemStackSummary(bestStack),
                                "closestDelta", closestDelta);
                        ChatClefDiagnostics.logTaskTransition(this, null, moveFuelTask, "furnace_return_move_fuel_task",
                                "needs", needs,
                                "bestStack", ChatClefDiagnostics.itemStackSummary(bestStack),
                                "closestDelta", closestDelta);
                        return moveFuelTask;
                    }
                }
            }

            setDebugState("Waiting...");
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "CONTAINER_SUBTASK_RETURN", "furnace_waiting_for_cook", this,
                    "materialSlot", ChatClefDiagnostics.itemStackSummary(material),
                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                    "outputSlot", ChatClefDiagnostics.itemStackSummary(output));
            VisibleTaskDiagnostics.logDecision(mod, this, "furnace_waiting_for_cook",
                    "material=" + ChatClefDiagnostics.itemStackSummary(material)
                            + "|fuel=" + ChatClefDiagnostics.itemStackSummary(fuel)
                            + "|output=" + ChatClefDiagnostics.itemStackSummary(output),
                    "materialSlot", ChatClefDiagnostics.itemStackSummary(material),
                    "fuelSlot", ChatClefDiagnostics.itemStackSummary(fuel),
                    "outputSlot", ChatClefDiagnostics.itemStackSummary(output),
                    "furnaceFuel", ChatClefDiagnostics.safeValue(StorageHelper::getFurnaceFuel),
                    "furnaceCookPercent", ChatClefDiagnostics.safeValue(StorageHelper::getFurnaceCookPercent));
            return null;
        }

        @Override
        protected double getCostToMakeNew(AltoClef mod) {
            boolean furnaceCacheHasContents = furnaceCache.burnPercentage > 0 || furnaceCache.burningFuelCount > 0 ||
                    !furnaceCache.fuelSlot.isEmpty() || !furnaceCache.materialSlot.isEmpty() ||
                    !furnaceCache.outputSlot.isEmpty();
            boolean diagnosticsBoundary = ChatClefDiagnostics.isBoundaryEnabled();
            int cobblestoneCount = mod.getItemStorage().getItemCount(Items.COBBLESTONE);
            int furnaceBlockItemCount = diagnosticsBoundary ? mod.getItemStorage().getItemCount(Items.FURNACE) : -1;
            if (furnaceCacheHasContents) {
                double cost = 9999999.0;
                if (diagnosticsBoundary) {
                    FurnaceContainerDiagnostics.logMakeCostSnapshot(mod,
                            this,
                            "FURNACE_CACHE_NON_EMPTY",
                            cost,
                            true,
                            furnaceCache.materialSlot,
                            furnaceCache.fuelSlot,
                            furnaceCache.outputSlot,
                            furnaceCache.burningFuelCount,
                            furnaceCache.burnPercentage,
                            cobblestoneCount,
                            StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD),
                            furnaceBlockItemCount);
                }
                ChatClefDiagnostics.logEvent("SMELT_FURNACE", "COST_TO_MAKE_NEW", "furnace_cache_non_empty_cost", this,
                        "cost", cost,
                        "cachedMaterialSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.materialSlot),
                        "cachedFuelSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.fuelSlot),
                        "cachedOutputSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.outputSlot));
                return cost;
            }
            if (cobblestoneCount > 8) {
                double cost = 100.0 - 90.0 * (double) mod.getItemStorage().getItemCount(new Item[]{Items.COBBLESTONE}) / 8.0;
                double boundedCost = Math.max(cost, 10.0);
                if (diagnosticsBoundary) {
                    FurnaceContainerDiagnostics.logMakeCostSnapshot(mod,
                            this,
                            "COBBLESTONE_INVENTORY_COST",
                            boundedCost,
                            false,
                            furnaceCache.materialSlot,
                            furnaceCache.fuelSlot,
                            furnaceCache.outputSlot,
                            furnaceCache.burningFuelCount,
                            furnaceCache.burnPercentage,
                            cobblestoneCount,
                            StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD),
                            furnaceBlockItemCount);
                }
                ChatClefDiagnostics.logEvent("SMELT_FURNACE", "COST_TO_MAKE_NEW", "furnace_inventory_cost", this,
                        "cost", boundedCost,
                        "cobblestoneCount", cobblestoneCount);
                return boundedCost;
            }
            boolean woodRequirementMetInventory = StorageHelper.miningRequirementMetInventory(MiningRequirement.WOOD);
            double fallbackCost = woodRequirementMetInventory ? 50.0 : 100.0;
            if (diagnosticsBoundary) {
                FurnaceContainerDiagnostics.logMakeCostSnapshot(mod,
                        this,
                        woodRequirementMetInventory ? "WOOD_TOOL_FALLBACK_50" : "NO_WOOD_TOOL_FALLBACK_100",
                        fallbackCost,
                        false,
                        furnaceCache.materialSlot,
                        furnaceCache.fuelSlot,
                        furnaceCache.outputSlot,
                        furnaceCache.burningFuelCount,
                        furnaceCache.burnPercentage,
                        cobblestoneCount,
                        woodRequirementMetInventory,
                        furnaceBlockItemCount);
            }
            ChatClefDiagnostics.logEvent("SMELT_FURNACE", "COST_TO_MAKE_NEW", "furnace_fallback_cost", this,
                    "cost", fallbackCost,
                    "woodRequirementMet", woodRequirementMetInventory);
            return fallbackCost;
        }

        @Override
        protected BlockPos overrideContainerPosition(AltoClef mod) {
            // If we have a valid container position, KEEP it.
            return getTargetContainerPosition();
        }

        private void tryUpdateOpenFurnace(AltoClef mod) {
            if (isContainerOpen(mod)) {
                // Update current furnace cache
                furnaceCache.burnPercentage = StorageHelper.getFurnaceCookPercent();
                furnaceCache.burningFuelCount = StorageHelper.getFurnaceFuel();
                furnaceCache.fuelSlot = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_FUEL);
                furnaceCache.materialSlot = StorageHelper.getItemStackInSlot(FurnaceSlot.INPUT_SLOT_MATERIALS);
                furnaceCache.outputSlot = StorageHelper.getItemStackInSlot(FurnaceSlot.OUTPUT_SLOT);
                FurnaceContainerDiagnostics.markCacheUpdated(this, getTargetContainerPosition());
                ChatClefDiagnostics.logEvent("SMELT_FURNACE", "CACHE_UPDATE", "furnace_cache_updated_from_open_screen", this,
                        "materialSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.materialSlot),
                        "fuelSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.fuelSlot),
                        "outputSlot", ChatClefDiagnostics.itemStackSummary(furnaceCache.outputSlot),
                        "burningFuelCount", furnaceCache.burningFuelCount,
                        "burnPercentage", furnaceCache.burnPercentage);
            } else {
                ChatClefDiagnostics.logEvent("SMELT_FURNACE", "CACHE_SKIP", "furnace_cache_not_updated_screen_closed", this);
            }
        }
    }

    static class FurnaceCache {
        public ItemStack materialSlot = ItemStack.EMPTY;
        public ItemStack fuelSlot = ItemStack.EMPTY;
        public ItemStack outputSlot = ItemStack.EMPTY;
        public double burningFuelCount = 0;
        public double burnPercentage = 0;
    }
}
