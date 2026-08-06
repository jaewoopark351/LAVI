package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.tasks.construction.DestroyBlockTask;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

//20260806_kpopmodder: Observe DestroyBlockTask navigation state at the Baritone path boundary.
final class DestroyNavigationDiagnostics {
    private DestroyNavigationDiagnostics() {
    }

    static void log(AltoClef mod,
                    DestroyBlockTask task,
                    BlockPos target,
                    String navigationState,
                    boolean reachPresent) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        SubmittedGoalDiagnosticState.SubmittedGoal submittedGoal = SubmittedGoalDiagnosticState.get(task);
        BaritonePathDiagnosticSnapshot snapshot = BaritonePathDiagnosticSnapshot.capture(
                mod,
                target,
                submittedGoal == null ? null : submittedGoal.goal,
                SubmittedGoalDiagnosticState.matchesTarget(submittedGoal, target)
        );
        String fingerprint = MiningDiagnosticEmitter.joinFingerprint(
                "DESTROY_NAVIGATION_STATE_TRANSITION",
                ChatClefDiagnostics.blockPos(target),
                navigationState,
                snapshot.customGoalActive,
                snapshot.baritonePathing,
                snapshot.pathPresent,
                Boolean.toString(reachPresent)
        );
        MiningDiagnosticEmitter.emit("DESTROY_NAVIGATION_STATE_TRANSITION", "destroy_navigation_state_transition", task,
                "destroy_navigation|" + System.identityHashCode(task),
                fingerprint,
                MiningDiagnosticEmitter.merge(new Object[]{
                        "owner", "destroy_block_task",
                        "trigger", "navigation_state",
                        "targetPosition", ChatClefDiagnostics.blockPos(target),
                        "targetBlockId", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target).getBlock()),
                        "targetBlockState", ChatClefDiagnostics.safeValue(() -> mod.getWorld().getBlockState(target)),
                        "blockStillExists", ChatClefDiagnostics.safeValue(() -> !mod.getWorld().getBlockState(target).isAir()),
                        "chunkLoaded", ChatClefDiagnostics.safeValue(() -> mod.getChunkTracker().isChunkLoaded(target)),
                        "worldCanBreak", ChatClefDiagnostics.safeValue(() -> WorldHelper.canBreak(target)),
                        "playerPosition", ChatClefDiagnostics.playerPosition(mod),
                        "distanceSq", ChatClefDiagnostics.safeValue(() -> BlockPosVer.getSquaredDistance(target, mod.getPlayer().getPos())),
                        "horizontalDistanceSq", ChatClefDiagnostics.safeValue(() -> horizontalDistanceSq(mod.getPlayer().getPos(), target)),
                        "verticalDelta", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getPos().y - target.getY()),
                        "reachPresent", reachPresent,
                        "playerOnGround", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isOnGround()),
                        "playerTouchingWater", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isTouchingWater()),
                        "foodChainNeedsToEat", ChatClefDiagnostics.safeValue(() -> mod.getFoodChain().needsToEat()),
                        "inNetherPortal", ChatClefDiagnostics.safeValue(WorldHelper::isInNetherPortal),
                        "navigationState", navigationState
                }, SubmittedGoalDiagnosticState.fields(submittedGoal, target), snapshot.fields()));
        GoalPathTransitionDiagnostics.log(mod, task, target, "PATH_STATE_CHANGED",
                submittedGoal == null ? null : submittedGoal.goal, "destroy_navigation_state");
    }

    private static double horizontalDistanceSq(Vec3d playerPosition, BlockPos target) {
        double dx = playerPosition.x - (target.getX() + 0.5);
        double dz = playerPosition.z - (target.getZ() + 0.5);
        return dx * dx + dz * dz;
    }
}
