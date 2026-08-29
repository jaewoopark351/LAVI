package lavi.minecraft.task.container.home.execution.transfer.container;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.storage.ContainerType;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import net.minecraft.block.Block;
import net.minecraft.screen.ScreenHandler;

//20260829_kpopmodder: Inspect only the exact trusted live-container binding used by a transfer.
public final class HomeStorageLiveContainerInspector {
    public HomeStorageLiveContainerSnapshot inspect(
            AltoClef mod,
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null
                || !binding.matches(destination.position())) {
            return new HomeStorageLiveContainerSnapshot(
                    null,
                    null,
                    false,
                    "exact_trusted_gui_not_open"
            );
        }
        Block block = mod.getWorld().getBlockState(destination.position()).getBlock();
        ScreenHandler handler = mod.getPlayer().currentScreenHandler;
        if (!AutoDepositTrustedContainerSupport.isSupported(block)
                || handler == null
                || !ContainerType.screenHandlerMatches(
                        ContainerType.getFromBlock(block), handler
                )) {
            return new HomeStorageLiveContainerSnapshot(
                    null,
                    null,
                    false,
                    "bound_container_invalid"
            );
        }
        return new HomeStorageLiveContainerSnapshot(
                handler,
                mod.getPlayer().getInventory(),
                true,
                "open"
        );
    }
}
