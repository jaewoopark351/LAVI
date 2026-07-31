package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StlHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class MoveItemToSlotTask extends Task {

    private final ItemTarget toMove;
    private final Slot destination;
    private final Function<AltoClef, List<Slot>> getMovableSlots;

    public MoveItemToSlotTask(ItemTarget toMove, Slot destination, Function<AltoClef, List<Slot>> getMovableSlots) {
        this.toMove = toMove;
        this.destination = destination;
        this.getMovableSlots = getMovableSlots;
    }

    @Override
    protected void onStart() {
        ChatClefDiagnostics.logEvent("SLOT_TASK", "ON_START", "move_item_to_slot_start", this,
                "toMove", toMove,
                "destination", ChatClefDiagnostics.slotSummary(destination),
                "destinationStack", ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(destination)),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        ChatClefDiagnostics.logEvent("SLOT_TASK", "ON_TICK_BEGIN", "move_item_to_slot_tick", this,
                "toMove", toMove,
                "destination", ChatClefDiagnostics.slotSummary(destination),
                "destinationStack", ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(destination)),
                "cursorStack", ChatClefDiagnostics.safeValue(StorageHelper::getItemStackInCursorSlot));

        if (mod.getSlotHandler().canDoSlotAction()) {
            // Rough plan
            // - If empty slot or wrong item
            //      Find best matching item (smallest count over target, or largest count if none over)
            //      Click on it (one turn)
            // - If held slot has < items than target count
            //      Left click on destination slot (one turn)
            // - If held slot has > items than target count
            //      Right click on destination slot (one turn)
            ItemStack currentHeld = StorageHelper.getItemStackInCursorSlot();
            ItemStack atTarget = StorageHelper.getItemStackInSlot(destination);

            // Items that CAN be moved to that slot.
            Item[] validItems = toMove.getMatches();//Arrays.stream(_toMove.getMatches()).filter(item -> mod.getItemStorage().getItemCount(item) >= _toMove.getTargetCount()).toArray(Item[]::new);

            // We need to deal with our cursor stack OR put an item there (to move).
            boolean wrongItemHeld = !Arrays.asList(validItems).contains(currentHeld.getItem());
            ChatClefDiagnostics.logEvent("SLOT_TASK", "STATE", "move_item_to_slot_inventory_state", this,
                    "toMove", toMove,
                    "destination", ChatClefDiagnostics.slotSummary(destination),
                    "currentHeld", ChatClefDiagnostics.itemStackSummary(currentHeld),
                    "atTarget", ChatClefDiagnostics.itemStackSummary(atTarget),
                    "wrongItemHeld", wrongItemHeld,
                    "validItems", StlHelper.toString(validItems, Item::getTranslationKey));
            if (currentHeld.isEmpty() || wrongItemHeld) {
                Optional<Slot> toPlace;
                if (currentHeld.isEmpty()) {
                    // Just pick up
                    toPlace = getBestSlotToPickUp(mod, validItems);
                    ChatClefDiagnostics.logEvent("SLOT_TASK", "DECISION", "cursor_empty_pick_source", this,
                            "toMove", toMove,
                            "sourceSlotPresent", toPlace.isPresent(),
                            "sourceSlot", toPlace.map(ChatClefDiagnostics::slotSummary).orElse("none"));
                } else {
                    // Try to fit the currently held item first.
                    toPlace = mod.getItemStorage().getSlotThatCanFitInPlayerInventory(currentHeld, true);
                    if (toPlace.isEmpty()) {
                        // If all else fails, just swap it.
                        toPlace = getBestSlotToPickUp(mod, validItems);
                    }
                    ChatClefDiagnostics.logEvent("SLOT_TASK", "DECISION", "cursor_wrong_item_pick_or_fit", this,
                            "toMove", toMove,
                            "currentHeld", ChatClefDiagnostics.itemStackSummary(currentHeld),
                            "targetSlotPresent", toPlace.isPresent(),
                            "targetSlot", toPlace.map(ChatClefDiagnostics::slotSummary).orElse("none"));
                }
                if (toPlace.isEmpty()) {
                    ChatClefDiagnostics.logEvent("SLOT_TASK", "RETURN", "no_source_slot_available_stop", this,
                            "toMove", toMove,
                            "destination", ChatClefDiagnostics.slotSummary(destination),
                            "validItems", StlHelper.toString(validItems, Item::getTranslationKey));
                    Debug.logWarning("Called MoveItemToSlotTask when item/not enough item is available! valid items: " + StlHelper.toString(validItems, Item::getTranslationKey));
                    this.stop();
                    return null;
                }
                ChatClefDiagnostics.logSlotClick("REQUEST", "move_item_pick_source_slot", toPlace.get(), 0, SlotActionType.PICKUP,
                        "toMove", toMove,
                        "destination", ChatClefDiagnostics.slotSummary(destination));
                mod.getSlotHandler().clickSlot(toPlace.get(), 0, SlotActionType.PICKUP);
                return null;
            }

            int currentlyPlaced = Arrays.asList(validItems).contains(atTarget.getItem()) ? atTarget.getCount() : 0;
            if (currentHeld.getCount() + currentlyPlaced <= toMove.getTargetCount()) {
                // Just place all of 'em
                ChatClefDiagnostics.logSlotClick("REQUEST", "move_item_place_all_destination", destination, 0, SlotActionType.PICKUP,
                        "toMove", toMove,
                        "currentHeld", ChatClefDiagnostics.itemStackSummary(currentHeld),
                        "currentlyPlaced", currentlyPlaced);
                mod.getSlotHandler().clickSlot(destination, 0, SlotActionType.PICKUP);
            } else {
                // Place one at a time.
                ChatClefDiagnostics.logSlotClick("REQUEST", "move_item_place_one_destination", destination, 1, SlotActionType.PICKUP,
                        "toMove", toMove,
                        "currentHeld", ChatClefDiagnostics.itemStackSummary(currentHeld),
                        "currentlyPlaced", currentlyPlaced);
                mod.getSlotHandler().clickSlot(destination, 1, SlotActionType.PICKUP);
            }
            return null;
        }
        ChatClefDiagnostics.logEvent("SLOT_TASK", "WAIT", "move_item_wait_slot_timer", this,
                "toMove", toMove,
                "destination", ChatClefDiagnostics.slotSummary(destination));
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "move_item_to_slot_onStop",
                "toMove", toMove,
                "destination", ChatClefDiagnostics.slotSummary(destination),
                "destinationStack", ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(destination)));
    }

    @Override
    public boolean isFinished() {
        ItemStack atDestination = StorageHelper.getItemStackInSlot(destination);
        boolean finished = toMove.matches(atDestination.getItem()) && atDestination.getCount() >= toMove.getTargetCount();
        ChatClefDiagnostics.logEvent("SLOT_TASK", "IS_FINISHED", "move_item_to_slot_isFinished", this,
                "toMove", toMove,
                "destination", ChatClefDiagnostics.slotSummary(destination),
                "destinationStack", ChatClefDiagnostics.itemStackSummary(atDestination),
                "finished", finished);
        return finished;
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof MoveItemToSlotTask task) {
            return task.toMove.equals(toMove) && task.destination.equals(destination);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Moving " + toMove + " to " + destination;
    }

    private Optional<Slot> getBestSlotToPickUp(AltoClef mod, Item[] validItems) {
        Slot bestMatch = null;
        if (!getMovableSlots.apply(mod).isEmpty()) {
            for (Slot slot : getMovableSlots.apply(mod)) {
                if (Slot.isCursor(slot))
                    continue;
                if (!toMove.matches(StorageHelper.getItemStackInSlot(slot).getItem()))
                    continue;
                if (bestMatch == null) {
                    bestMatch = slot;
                    continue;
                }
                int countBest = StorageHelper.getItemStackInSlot(bestMatch).getCount();
                int countCheck = StorageHelper.getItemStackInSlot(slot).getCount();
                if ((countBest < toMove.getTargetCount() && countCheck > countBest)
                        || (countBest >= toMove.getTargetCount() && countCheck >= toMove.getTargetCount() && countCheck > countBest)) {
                    // If we don't have enough, go for largest
                    // If we have too much, go for smallest over the limit.
                    bestMatch = slot;
                }
            }
        }
        Slot selectedBestMatch = bestMatch;
        ChatClefDiagnostics.logEvent("SLOT_TASK", "BEST_SOURCE", "move_item_best_slot_to_pick_up", this,
                "toMove", toMove,
                "bestMatch", selectedBestMatch == null ? "none" : ChatClefDiagnostics.slotSummary(selectedBestMatch),
                "bestStack", selectedBestMatch == null ? "none" : ChatClefDiagnostics.safeValue(() -> StorageHelper.getItemStackInSlot(selectedBestMatch)));
        return Optional.ofNullable(bestMatch);
    }
}
