package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;

//20260815_kpopmodder: Observe furnace route shifts without changing route arbitration.
final class FurnaceRouteShiftDiagnostics {
    private static final String FROM_OPEN_EXISTING = "OPEN_EXISTING_CONTAINER";
    private static final String TO_GET_CONTAINER_ITEM = "GET_CONTAINER_ITEM";

    private FurnaceRouteShiftDiagnostics() {
    }

    static void logOpenExistingToGetContainerItem(AltoClef mod,
                                                  Task task,
                                                  ItemTarget containerTarget,
                                                  Block[] containerBlocks,
                                                  FurnaceTaskDiagnosticState.RouteObservation route,
                                                  Task candidateChild,
                                                  String candidateChildSemanticKey,
                                                  BlockPos nearestPosition,
                                                  String nearestSource,
                                                  BlockPos overrideContainerPosition,
                                                  BlockPos cachedContainerPositionBefore,
                                                  BlockPos cachedContainerPositionAfter,
                                                  BlockPos placeTaskPlaced,
                                                  double costToWalk,
                                                  double costToMakeNew,
                                                  String rawCostRelation,
                                                  String costDeltaBand,
                                                  boolean hasContainerBlockItem,
                                                  int containerBlockItemCount,
                                                  String trigger) {
        if (route == null
                || !route.branchChanged
                || !FROM_OPEN_EXISTING.equals(route.previousEffectiveBranch)
                || !TO_GET_CONTAINER_ITEM.equals(route.effectiveBranch)) {
            return;
        }
        FurnaceDiagnosticEmitter.emit(
                "OPEN_EXISTING_CONTAINER_TO_GET_CONTAINER_ITEM_TRANSITION",
                "furnace_route_shift_from_existing_container_to_container_item",
                task,
                "furnace_route_shift|" + Integer.toHexString(System.identityHashCode(task)),
                FurnaceDiagnosticEmitter.joinFingerprint(
                        "OPEN_EXISTING_CONTAINER_TO_GET_CONTAINER_ITEM_TRANSITION",
                        ChatClefDiagnostics.blockPos(cachedContainerPositionBefore),
                        ChatClefDiagnostics.blockPos(nearestPosition),
                        costDeltaBand,
                        FurnaceDiagnosticEmitter.taskClass(candidateChild)
                ),
                new Object[]{
                        "owner", "furnace_route_shift_observer",
                        "trigger", trigger,
                        "decisionSequence", route.decisionSequence,
                        "previousEffectiveBranch", route.previousEffectiveBranch,
                        "effectiveBranch", route.effectiveBranch,
                        "branchAgeTicksBeforeShift", route.branchAgeTicks,
                        "branchTransitionCount", route.branchTransitionCount,
                        "cachedContainerPositionBefore", ChatClefDiagnostics.blockPos(cachedContainerPositionBefore),
                        "cachedContainerPositionAfter", ChatClefDiagnostics.blockPos(cachedContainerPositionAfter),
                        "overrideContainerPosition", ChatClefDiagnostics.blockPos(overrideContainerPosition),
                        "placeTaskPlaced", ChatClefDiagnostics.blockPos(placeTaskPlaced),
                        "targetBlockStateNow", blockState(mod, cachedContainerPositionBefore),
                        "blockScannerStillContainsTarget", scannerStillContains(mod, cachedContainerPositionBefore, containerBlocks),
                        "nearestSource", nearestSource,
                        "nearestContainerPresent", nearestPosition != null,
                        "nearestContainerPosition", ChatClefDiagnostics.blockPos(nearestPosition),
                        "nearestContainerBlockState", blockState(mod, nearestPosition),
                        "inventoryContainerItemCount", containerBlockItemCount,
                        "hasContainerBlockItem", hasContainerBlockItem,
                        "costToWalk", costToWalk,
                        "costToMakeNew", costToMakeNew,
                        "costDelta", costDelta(costToWalk, costToMakeNew),
                        "rawCostRelation", rawCostRelation,
                        "costDeltaBand", costDeltaBand,
                        "containerTarget", containerTarget,
                        "containerBlocks", java.util.Arrays.toString(containerBlocks),
                        "candidateChildClass", FurnaceDiagnosticEmitter.taskClass(candidateChild),
                        "candidateChildInstanceId", FurnaceDiagnosticEmitter.instanceId(candidateChild),
                        "candidateChildSemanticKey", candidateChildSemanticKey,
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod)
                }
        );
    }

    private static Object blockState(AltoClef mod, BlockPos position) {
        if (mod == null || position == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(position));
    }

    private static Object scannerStillContains(AltoClef mod, BlockPos position, Block[] containerBlocks) {
        if (mod == null || position == null || containerBlocks == null || containerBlocks.length == 0) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> mod.getBlockScanner().isBlockAtPosition(position, containerBlocks));
    }

    private static Object costDelta(double costToWalk, double costToMakeNew) {
        if (!Double.isFinite(costToWalk) || !Double.isFinite(costToMakeNew)) {
            return "infinite";
        }
        return costToWalk - costToMakeNew;
    }
}
