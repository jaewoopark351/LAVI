package lavi.minecraft.task.container.deposit.auto.recovery;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import adris.altoclef.tasks.container.StoreInContainerTask;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;

public final class AutoDepositDestinationManifestTracker {
    private final AltoClef mod;
    private final AutoDepositDestinationManifest manifest;
    private Subscription<SlotClickChangedEvent> subscription;

    public AutoDepositDestinationManifestTracker(AltoClef mod,
                                                 AutoDepositDestinationManifest manifest) {
        this.mod = mod;
        this.manifest = manifest;
    }

    public void start() {
        if (subscription == null) {
            subscription = EventBus.subscribe(SlotClickChangedEvent.class, this::onSlotChanged);
        }
    }

    public void stop() {
        EventBus.unsubscribe(subscription);
        subscription = null;
    }

    public boolean isTracking() {
        return subscription != null;
    }

    private void onSlotChanged(SlotClickChangedEvent event) {
        if (event == null || event.slot == null || event.slot.isSlotInPlayerInventory()) {
            return;
        }
        if (mod.getWorld() != manifest.worldIdentity()
                || WorldHelper.getCurrentDimension() != manifest.dimension()) {
            return;
        }
        Optional<BlockPos> position = mod.getItemStorage().getLastBlockPosInteraction();
        if (position.isEmpty()) {
            return;
        }
        Block block = mod.getWorld().getBlockState(position.get()).getBlock();
        if (Arrays.stream(StoreInContainerTask.CONTAINER_BLOCKS).noneMatch(block::equals)) {
            return;
        }
        ContainerType type = ContainerType.getFromBlock(block);
        if (!ContainerType.screenHandlerMatches(type)) {
            return;
        }

        ItemStack before = event.before;
        ItemStack after = event.after;
        if (after == null || after.isEmpty()) {
            return;
        }
        int positiveDelta = after.getCount();
        if (before != null && !before.isEmpty() && before.getItem() == after.getItem()) {
            positiveDelta = after.getCount() - before.getCount();
        }
        if (positiveDelta > 0) {
            manifest.record(position.get(), after.getItem(), positiveDelta);
        }
    }
}
