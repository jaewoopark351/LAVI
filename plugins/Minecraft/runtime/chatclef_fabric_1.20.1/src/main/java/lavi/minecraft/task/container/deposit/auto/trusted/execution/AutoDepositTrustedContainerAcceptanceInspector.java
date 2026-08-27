package lavi.minecraft.task.container.deposit.auto.trusted.execution;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerType;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: Revalidate the selected trusted container against the live server GUI.
public final class AutoDepositTrustedContainerAcceptanceInspector {
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;

    public AutoDepositTrustedContainerAcceptanceInspector(
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding,
                "exactOpenContainerBinding"
        );
    }

    public Result inspect(AltoClef mod, BlockPos position, ItemTarget[] remainingTargets) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            return Result.NOT_OPEN;
        }
        if (!exactOpenContainerBinding.matches(position)) {
            return Result.NOT_OPEN;
        }
        Block block = mod.getWorld().getBlockState(position).getBlock();
        if (!AutoDepositTrustedContainerSupport.isSupported(block)
                || !ContainerType.screenHandlerMatches(
                ContainerType.getFromBlock(block),
                mod.getPlayer().currentScreenHandler
        )) {
            return Result.INVALID_CONTAINER;
        }

        boolean foundSource = false;
        for (ItemTarget target : remainingTargets) {
            List<Slot> sources = mod.getItemStorage()
                    .getSlotsWithItemPlayerInventory(false, target.getMatches());
            for (Slot source : sources) {
                ItemStack stack = StorageHelper.getItemStackInSlot(source);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                foundSource = true;
                if (mod.getItemStorage()
                        .getSlotThatCanFitInOpenContainer(stack, false)
                        .isPresent()) {
                    return Result.CAN_ACCEPT;
                }
            }
        }
        return foundSource ? Result.NO_WHOLE_STACK_CAPACITY : Result.NO_SOURCE_ITEMS;
    }

    public enum Result {
        NOT_OPEN,
        CAN_ACCEPT,
        NO_WHOLE_STACK_CAPACITY,
        NO_SOURCE_ITEMS,
        INVALID_CONTAINER
    }
}
