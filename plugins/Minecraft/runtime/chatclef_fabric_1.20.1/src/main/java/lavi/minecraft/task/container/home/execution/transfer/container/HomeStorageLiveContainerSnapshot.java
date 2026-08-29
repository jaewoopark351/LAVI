package lavi.minecraft.task.container.home.execution.transfer.container;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;

//20260829_kpopmodder: Carry only the trusted GUI state read by one transfer tick.
public record HomeStorageLiveContainerSnapshot(
        ScreenHandler handler,
        PlayerInventory playerInventory,
        boolean open,
        String reason) {
}
