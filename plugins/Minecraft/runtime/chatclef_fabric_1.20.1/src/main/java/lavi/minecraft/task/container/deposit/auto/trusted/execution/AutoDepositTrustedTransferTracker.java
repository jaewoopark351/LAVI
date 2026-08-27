package lavi.minecraft.task.container.deposit.auto.trusted.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//20260827_kpopmodder: Confirm trusted transfers from paired inventory loss and container gain.
public final class AutoDepositTrustedTransferTracker {
    private final AltoClef mod;
    private final Object worldIdentity;
    private final Dimension dimension;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;
    private final Map<Item, Integer> inventoryNet = new HashMap<>();
    private final Map<Item, Integer> containerNet = new HashMap<>();
    private Subscription<SlotClickChangedEvent> subscription;
    private BlockPos activePosition;

    public AutoDepositTrustedTransferTracker(
            AltoClef mod,
            Object worldIdentity,
            Dimension dimension,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        this.mod = mod;
        this.worldIdentity = worldIdentity;
        this.dimension = dimension;
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding,
                "exactOpenContainerBinding"
        );
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

    public void observeCandidate(BlockPos position) {
        activePosition = position == null ? null : position.toImmutable();
    }

    public int confirmedCount(Item... items) {
        int containerAdded = 0;
        int inventoryRemoved = 0;
        for (Item item : items) {
            containerAdded = saturatingAdd(containerAdded,
                    Math.max(0, containerNet.getOrDefault(item, 0)));
            inventoryRemoved = saturatingAdd(inventoryRemoved,
                    positiveMagnitudeOfNegative(inventoryNet.getOrDefault(item, 0)));
        }
        return Math.min(containerAdded, inventoryRemoved);
    }

    private void onSlotChanged(SlotClickChangedEvent event) {
        if (event == null || event.slot == null || !matchesActiveContainer()) {
            return;
        }
        Map<Item, Integer> target = event.slot.isSlotInPlayerInventory()
                ? inventoryNet
                : containerNet;
        recordDelta(target, event.before, -1);
        recordDelta(target, event.after, 1);
    }

    private boolean matchesActiveContainer() {
        if (activePosition == null || mod == null || mod.getWorld() == null || mod.getPlayer() == null
                || mod.getWorld() != worldIdentity
                || WorldHelper.getCurrentDimension() != dimension) {
            return false;
        }
        if (!exactOpenContainerBinding.matches(activePosition)) {
            return false;
        }
        Block block = mod.getWorld().getBlockState(activePosition).getBlock();
        return AutoDepositTrustedContainerSupport.isSupported(block)
                && ContainerType.screenHandlerMatches(
                ContainerType.getFromBlock(block),
                mod.getPlayer().currentScreenHandler
        );
    }

    private static void recordDelta(Map<Item, Integer> totals, ItemStack stack, int direction) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        totals.merge(stack.getItem(), direction * stack.getCount(),
                AutoDepositTrustedTransferTracker::saturatingAdd);
    }

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        if (sum > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (sum < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) sum;
    }

    private static int positiveMagnitudeOfNegative(int value) {
        if (value >= 0) {
            return 0;
        }
        return value == Integer.MIN_VALUE ? Integer.MAX_VALUE : -value;
    }
}
