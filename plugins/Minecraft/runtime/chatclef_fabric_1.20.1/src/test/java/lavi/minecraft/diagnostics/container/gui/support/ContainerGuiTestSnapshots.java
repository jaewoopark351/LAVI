package lavi.minecraft.diagnostics.container.gui.support;

import lavi.minecraft.diagnostics.container.gui.correlation.ContainerOpenInteractionObservation;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerTargetFamily;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshot;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowState;
import net.minecraft.util.math.BlockPos;

//20260904_kpopmodder: Keep immutable GUI-diagnostic fixtures out of behavior-owning runtime code.
public final class ContainerGuiTestSnapshots {
    private ContainerGuiTestSnapshots() {
    }

    public static ContainerScreenEventSnapshot heuristicChestTail() {
        return heuristicChestTailWithRootAssignment("unavailable");
    }

    public static ContainerScreenEventSnapshot heuristicChestTailWithRootAssignment(
            String rootAssignmentId) {
        ContainerOpenInteractionObservation interaction = new ContainerOpenInteractionObservation(
                11L,
                17L,
                ContainerTargetFamily.CHEST,
                new BlockPos(12, 64, -7),
                "minecraft:chest",
                "world@1",
                "minecraft:overworld",
                "adris.altoclef.tasks.container.StoreInContainerTask",
                "route@1",
                rootAssignmentId,
                "SUCCESS",
                new Object[]{
                        "commandRequestId", "request-1",
                        "commandCorrelationId", "command-correlation-1",
                        "commandContextAvailable", true
                }
        );
        return new ContainerScreenEventSnapshot(
                "screen-event@1",
                false,
                "screen@1",
                "Chest",
                "net.minecraft.client.gui.screen.ingame.GenericContainerScreen",
                true,
                "handler@1",
                "handler@1",
                7,
                7,
                "net.minecraft.screen.GenericContainerScreenHandler",
                true,
                true,
                ContainerTargetFamily.CHEST,
                "12,64,-7",
                "minecraft:chest",
                "minecraft:chest",
                "world@1",
                "minecraft:overworld",
                "GenericContainerScreen",
                "GenericContainerScreenHandler",
                true,
                true,
                true,
                "HEURISTIC_RECENT_EXACT_CONTAINER_INTERACTION",
                interaction,
                new ContainerClientTickWindowSnapshot(
                        ContainerClientTickWindowState.BETWEEN_TICKS,
                        false,
                        -1L,
                        17L,
                        17L
                )
        );
    }
}
