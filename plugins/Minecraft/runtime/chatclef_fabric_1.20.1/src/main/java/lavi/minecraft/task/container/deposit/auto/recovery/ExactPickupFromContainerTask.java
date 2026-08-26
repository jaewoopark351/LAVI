package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.AbstractDoToStorageContainerTask;
import adris.altoclef.tasks.slot.MoveItemToSlotTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.trackers.storage.ContainerCache;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ExactPickupFromContainerTask extends AbstractDoToStorageContainerTask {
    private static final int TRANSFER_TIMEOUT_TICKS = 200;
    private final BlockPos targetContainer;
    private final Map<Item, Integer> targetInventoryCounts;
    private final Map<Item, Integer> remainingWithdrawal;
    private final PlayerInventorySnapshotReader inventoryReader = new PlayerInventorySnapshotReader();

    private Result result = Result.RUNNING;
    private MoveItemToSlotTask moveTask;
    private Slot sourceSlot;
    private Item movingItem;
    private int plannedTransfer;
    private int inventoryBeforeTransfer;
    private int transferTicks;
    private boolean abortingTransfer;

    public ExactPickupFromContainerTask(BlockPos targetContainer,
                                        Map<Item, Integer> targetInventoryCounts,
                                        Map<Item, Integer> withdrawalLimits) {
        this.targetContainer = Objects.requireNonNull(targetContainer, "targetContainer").toImmutable();
        this.targetInventoryCounts = Collections.unmodifiableMap(
                new LinkedHashMap<>(targetInventoryCounts)
        );
        remainingWithdrawal = new LinkedHashMap<>(withdrawalLimits);
    }

    @Override
    protected Optional<BlockPos> getContainerTarget() {
        return Optional.of(targetContainer);
    }

    @Override
    protected Task onContainerOpenSubtask(AltoClef mod, ContainerCache containerCache) {
        if (moveTask != null) {
            return continueTransfer(mod);
        }

        ItemStack existingCursor = StorageHelper.getItemStackInCursorSlot();
        if (!existingCursor.isEmpty()) {
            Optional<Slot> cursorDestination = findDestination(
                    mod,
                    existingCursor,
                    existingCursor.getCount()
            );
            if (cursorDestination.isEmpty()) {
                result = Result.NO_INVENTORY_CAPACITY;
                return null;
            }
            if (mod.getSlotHandler().canDoSlotAction()) {
                mod.getSlotHandler().clickSlot(cursorDestination.get(), 0, SlotActionType.PICKUP);
            }
            return null;
        }

        Map<Item, Integer> current = inventoryReader.readMain(mod);
        if (targetsMet(current)) {
            result = Result.SATISFIED;
            return null;
        }

        for (Map.Entry<Item, Integer> entry : targetInventoryCounts.entrySet()) {
            Item item = entry.getKey();
            int currentCount = current.getOrDefault(item, 0);
            int needed = Math.max(0, entry.getValue() - currentCount);
            int allowed = remainingWithdrawal.getOrDefault(item, 0);
            if (needed == 0 || allowed == 0) {
                continue;
            }
            Optional<Slot> source = mod.getItemStorage().getSlotsWithItemContainer(item).stream()
                    .filter(slot -> !Slot.isCursor(slot))
                    .findFirst();
            if (source.isEmpty()) {
                continue;
            }
            ItemStack sourceStack = StorageHelper.getItemStackInSlot(source.get());
            int transfer = Math.min(Math.min(needed, allowed), sourceStack.getCount());
            Optional<Slot> destination = findDestination(mod, sourceStack, transfer);
            if (destination.isEmpty()) {
                result = Result.NO_INVENTORY_CAPACITY;
                return null;
            }

            ItemStack destinationStack = StorageHelper.getItemStackInSlot(destination.get());
            int destinationCount = destinationStack.getItem() == item ? destinationStack.getCount() : 0;
            sourceSlot = source.get();
            movingItem = item;
            plannedTransfer = transfer;
            inventoryBeforeTransfer = currentCount;
            transferTicks = 0;
            abortingTransfer = false;
            moveTask = new MoveItemToSlotTask(
                    new ItemTarget(item, destinationCount + transfer),
                    destination.get(),
                    ignored -> List.of(sourceSlot)
            );
            return moveTask;
        }

        result = Result.EXHAUSTED;
        return null;
    }

    private Task continueTransfer(AltoClef mod) {
        transferTicks++;
        if (abortingTransfer) {
            ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
            if (cursor.isEmpty()) {
                result = Result.NO_PROGRESS;
                return null;
            }
            if (cursor.getItem() != movingItem) {
                result = Result.CURSOR_CONFLICT;
                return null;
            }
            if (mod.getSlotHandler().canDoSlotAction()) {
                mod.getSlotHandler().clickSlot(sourceSlot, 0, SlotActionType.PICKUP);
            }
            return null;
        }
        if (!moveTask.isFinished()) {
            if (transferTicks >= TRANSFER_TIMEOUT_TICKS) {
                ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
                if (cursor.isEmpty()) {
                    result = Result.NO_PROGRESS;
                } else if (cursor.getItem() == movingItem) {
                    abortingTransfer = true;
                } else {
                    result = Result.CURSOR_CONFLICT;
                }
                return null;
            }
            return moveTask;
        }
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (!cursor.isEmpty()) {
            if (cursor.getItem() != movingItem) {
                result = Result.CURSOR_CONFLICT;
                return null;
            }
            if (mod.getSlotHandler().canDoSlotAction()) {
                mod.getSlotHandler().clickSlot(sourceSlot, 0, SlotActionType.PICKUP);
            }
            return null;
        }

        int current = inventoryReader.readMain(mod).getOrDefault(movingItem, 0);
        int gained = Math.max(0, current - inventoryBeforeTransfer);
        if (gained == 0) {
            result = Result.NO_PROGRESS;
            return null;
        }
        remainingWithdrawal.computeIfPresent(movingItem, (ignored, remaining) ->
                Math.max(0, remaining - Math.min(plannedTransfer, gained)));
        moveTask = null;
        sourceSlot = null;
        movingItem = null;
        plannedTransfer = 0;
        transferTicks = 0;
        abortingTransfer = false;
        return null;
    }

    private Optional<Slot> findDestination(AltoClef mod, ItemStack sourceStack, int transfer) {
        ItemStack fit = sourceStack.copy();
        fit.setCount(transfer);
        return mod.getItemStorage().getSlotsThatCanFitInPlayerInventory(fit, true).stream()
                .filter(slot -> {
                    ItemStack current = StorageHelper.getItemStackInSlot(slot);
                    int room = current.isEmpty()
                            ? fit.getMaxCount()
                            : current.getMaxCount() - current.getCount();
                    return room >= transfer;
                })
                .findFirst();
    }

    private boolean targetsMet(Map<Item, Integer> current) {
        return targetInventoryCounts.entrySet().stream()
                .allMatch(entry -> current.getOrDefault(entry.getKey(), 0) >= entry.getValue());
    }

    public Result result() {
        return result;
    }

    @Override
    protected void onStop(Task interruptTask) {
        if (moveTask == null || movingItem == null || sourceSlot == null) {
            return;
        }
        AltoClef mod = AltoClef.getInstance();
        ItemStack cursor = StorageHelper.getItemStackInCursorSlot();
        if (cursor.isEmpty() || cursor.getItem() != movingItem || !mod.getSlotHandler().canDoSlotAction()) {
            return;
        }
        boolean targetStillOpen = mod.getItemStorage().getLastBlockPosInteraction()
                .filter(targetContainer::equals)
                .isPresent();
        if (targetStillOpen) {
            ItemStack atSource = StorageHelper.getItemStackInSlot(sourceSlot);
            int sourceRoom = atSource.isEmpty()
                    ? cursor.getMaxCount()
                    : atSource.getMaxCount() - atSource.getCount();
            if ((atSource.isEmpty() || atSource.getItem() == movingItem)
                    && sourceRoom >= cursor.getCount()) {
                mod.getSlotHandler().clickSlot(sourceSlot, 0, SlotActionType.PICKUP);
                return;
            }
        }
        findDestination(mod, cursor, cursor.getCount())
                .ifPresent(slot -> mod.getSlotHandler().clickSlot(slot, 0, SlotActionType.PICKUP));
    }

    @Override
    public boolean isFinished() {
        return result != Result.RUNNING;
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof ExactPickupFromContainerTask task
                && targetContainer.equals(task.targetContainer)
                && targetInventoryCounts.equals(task.targetInventoryCounts)
                && remainingWithdrawal.equals(task.remainingWithdrawal);
    }

    @Override
    protected String toDebugString() {
        return "Recovering exact working-set deficits from " + targetContainer.toShortString();
    }

    public enum Result {
        RUNNING,
        SATISFIED,
        EXHAUSTED,
        NO_INVENTORY_CAPACITY,
        CURSOR_CONFLICT,
        NO_PROGRESS
    }
}
