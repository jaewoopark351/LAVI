package lavi.minecraft.integration.carryon;

import adris.altoclef.AltoClef;
import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnInputSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnOperationSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnPlayerSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnScreenSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTargetSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshotCollector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

//20260730_kpopmodder: Collect Carry On diagnostic snapshots without owning engine behavior.
public final class CarryOnSnapshotCollector {
    private CarryOnSnapshotCollector() {
    }

    public static CarryOnSnapshot collect(long taskInstanceId,
                                          long operationId,
                                          String eventName,
                                          CarryOnOperationType operationType,
                                          CarryOnTransition expectedTransition,
                                          String childTask,
                                          String targetType,
                                          String targetId,
                                          String targetPosition,
                                          CarryOnObservation stateBefore,
                                          CarryOnObservation stateAfter,
                                          String clickResult,
                                          int attemptCount,
                                          int elapsedTicks,
                                          CarryOnTerminalReason terminalReason) {
        AltoClef mod = AltoClef.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;

        return new CarryOnSnapshot(
                CarryOnOperationSnapshotCollector.collect(
                        mod,
                        taskInstanceId,
                        operationId,
                        eventName,
                        operationType,
                        expectedTransition,
                        stateBefore,
                        stateAfter,
                        clickResult,
                        attemptCount,
                        elapsedTicks,
                        terminalReason
                ),
                CarryOnTaskSnapshotCollector.collect(mod, childTask),
                CarryOnTargetSnapshotCollector.collect(client, targetType, targetId, targetPosition),
                CarryOnPlayerSnapshotCollector.collect(player),
                CarryOnInputSnapshotCollector.collect(mod),
                CarryOnBaritoneSnapshotCollector.collect(mod),
                CarryOnScreenSnapshotCollector.collect(client, player)
        );
    }
}
