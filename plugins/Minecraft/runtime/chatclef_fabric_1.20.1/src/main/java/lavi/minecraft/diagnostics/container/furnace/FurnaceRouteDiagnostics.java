package lavi.minecraft.diagnostics.container.furnace;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Arrays;

//20260807_kpopmodder: Observe furnace route arbitration transitions without changing branch selection.
final class FurnaceRouteDiagnostics {
    private FurnaceRouteDiagnostics() {
    }

    static void log(AltoClef mod,
                    Task task,
                    ItemTarget containerTarget,
                    Block[] containerBlocks,
                    String effectiveBranch,
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
                    boolean placeForceElapsedBeforeReset,
                    double placeForceDurationBeforeReset,
                    boolean placeForceResetThisTick,
                    boolean placeForceElapsedAfterReset,
                    double placeForceDurationAfterReset,
                    boolean justPlacedElapsed,
                    double justPlacedTimerAgeSeconds,
                    boolean hasContainerBlockItem,
                    int containerBlockItemCount,
                    String trigger) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || !isFurnaceContainer(containerTarget, containerBlocks)) {
            return;
        }
        FurnaceTaskDiagnosticState.RouteObservation route = FurnaceTaskDiagnosticState.observeRoute(task, effectiveBranch);
        String rawCostRelation = rawCostRelation(nearestPosition, costToWalk, costToMakeNew);
        String costDeltaBand = costDeltaBand(costToWalk, costToMakeNew);
        String nearestKey = nearestPosition == null ? "none" : nearestPosition.toShortString();
        String fingerprint = FurnaceDiagnosticEmitter.joinFingerprint(
                "FURNACE_CONTAINER_ROUTE_TRANSITION",
                route.previousEffectiveBranch,
                route.effectiveBranch,
                costDeltaBand,
                nearestKey,
                Boolean.toString(placeForceResetThisTick),
                Boolean.toString(hasContainerBlockItem),
                FurnaceDiagnosticEmitter.taskClass(candidateChild)
        );
        FurnaceDiagnosticEmitter.emit("FURNACE_CONTAINER_ROUTE_TRANSITION", "furnace_container_route_transition", task,
                "furnace_route|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "furnace_container_route_observer",
                        "trigger", trigger,
                        "decisionSequence", route.decisionSequence,
                        "previousEffectiveBranch", route.previousEffectiveBranch,
                        "effectiveBranch", route.effectiveBranch,
                        "branchChanged", route.branchChanged,
                        "branchAgeTicks", route.branchAgeTicks,
                        "branchTransitionCount", route.branchTransitionCount,
                        "rawCostRelation", rawCostRelation,
                        "costToWalk", costToWalk,
                        "costToMakeNew", costToMakeNew,
                        "costDelta", costDelta(costToWalk, costToMakeNew),
                        "costDeltaBand", costDeltaBand,
                        "placeForceElapsedBeforeReset", placeForceElapsedBeforeReset,
                        "placeForceDurationBeforeReset", placeForceDurationBeforeReset,
                        "placeForceResetThisTick", placeForceResetThisTick,
                        "placeForceElapsedAfterReset", placeForceElapsedAfterReset,
                        "placeForceDurationAfterReset", placeForceDurationAfterReset,
                        "placeForceHoldRemainingSeconds", holdRemainingSeconds(placeForceElapsedAfterReset, placeForceDurationAfterReset),
                        "justPlacedElapsed", justPlacedElapsed,
                        "justPlacedTimerAgeSeconds", justPlacedTimerAgeSeconds,
                        "justPlacedDurationSeconds", FurnaceContainerDiagnostics.JUST_PLACED_DURATION_SECONDS,
                        "nearestSource", nearestSource,
                        "nearestPresent", nearestPosition != null,
                        "nearestPosition", ChatClefDiagnostics.blockPos(nearestPosition),
                        "nearestBlockState", nearestPosition == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(nearestPosition)),
                        "nearestChunkLoaded", nearestPosition == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> mod.getChunkTracker().isChunkLoaded(nearestPosition)),
                        "nearestCanReach", nearestPosition == null ? "unavailable" : ChatClefDiagnostics.safeValue(() -> WorldHelper.canReach(nearestPosition)),
                        "nearestScannerUnreachable", "unavailable_without_scanner_state",
                        "cachedContainerPositionBefore", ChatClefDiagnostics.blockPos(cachedContainerPositionBefore),
                        "cachedContainerPositionAfter", ChatClefDiagnostics.blockPos(cachedContainerPositionAfter),
                        "overrideContainerPosition", ChatClefDiagnostics.blockPos(overrideContainerPosition),
                        "placeTaskPlaced", ChatClefDiagnostics.blockPos(placeTaskPlaced),
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "playerDistanceSqToNearest", distanceSq(mod, nearestPosition),
                        "horizontalDistanceSqToNearest", horizontalDistanceSq(mod, nearestPosition),
                        "verticalDeltaToNearest", verticalDelta(mod, nearestPosition),
                        "hasContainerBlockItem", hasContainerBlockItem,
                        "containerBlockItemCount", containerBlockItemCount,
                        "containerTarget", containerTarget,
                        "containerBlocks", Arrays.toString(containerBlocks),
                        "candidateChildClass", FurnaceDiagnosticEmitter.taskClass(candidateChild),
                        "candidateChildInstanceId", FurnaceDiagnosticEmitter.instanceId(candidateChild),
                        "candidateChildSemanticKey", candidateChildSemanticKey
                });
    }

    private static boolean isFurnaceContainer(ItemTarget containerTarget, Block[] containerBlocks) {
        boolean targetMatches = containerTarget != null && containerTarget.matches(Items.FURNACE);
        boolean blockMatches = containerBlocks != null && Arrays.stream(containerBlocks).anyMatch(block -> block == Blocks.FURNACE);
        return targetMatches || blockMatches;
    }

    private static String rawCostRelation(BlockPos nearestPosition, double costToWalk, double costToMakeNew) {
        if (nearestPosition == null || !Double.isFinite(costToWalk)) {
            return "NO_NEAREST";
        }
        int comparison = Double.compare(costToWalk, costToMakeNew);
        if (comparison < 0) {
            return "WALK_COST_LOWER";
        }
        if (comparison == 0) {
            return "WALK_COST_EQUAL";
        }
        return "WALK_COST_HIGHER";
    }

    private static Object costDelta(double costToWalk, double costToMakeNew) {
        if (!Double.isFinite(costToWalk) || !Double.isFinite(costToMakeNew)) {
            return "infinite";
        }
        return costToWalk - costToMakeNew;
    }

    private static String costDeltaBand(double costToWalk, double costToMakeNew) {
        if (!Double.isFinite(costToWalk) || !Double.isFinite(costToMakeNew)) {
            return "INFINITE";
        }
        double delta = costToWalk - costToMakeNew;
        if (delta <= -1.0) {
            return "LE_MINUS_1";
        }
        if (delta < 0.0) {
            return "MINUS_1_TO_0";
        }
        if (delta <= 1.0) {
            return "ZERO_TO_PLUS_1";
        }
        return "GT_PLUS_1";
    }

    private static double holdRemainingSeconds(boolean elapsed, double duration) {
        if (elapsed) {
            return 0.0;
        }
        return Math.max(0.0, FurnaceContainerDiagnostics.PLACE_FORCE_DURATION_SECONDS - duration);
    }

    private static Object distanceSq(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> BlockPosVer.getSquaredDistance(target, mod.getPlayer().getPos()));
    }

    private static Object horizontalDistanceSq(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> {
            Vec3d playerPosition = mod.getPlayer().getPos();
            double dx = playerPosition.x - (target.getX() + 0.5);
            double dz = playerPosition.z - (target.getZ() + 0.5);
            return dx * dx + dz * dz;
        });
    }

    private static Object verticalDelta(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getPos().y - target.getY());
    }
}
